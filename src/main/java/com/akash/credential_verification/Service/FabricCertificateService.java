package com.akash.credential_verification.Service;

import com.akash.credential_verification.Model.University;
import com.akash.credential_verification.Repository.UniversityRepository;
import jakarta.annotation.PostConstruct;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.identity.X509Enrollment;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Security;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class FabricCertificateService {

    // ── keep these for the shared orderer (same for all orgs) ──
    @Value("${fabric.network.channelName}")
    private String channelName;
    @Value("${fabric.network.chaincodeName}")
    private String chaincodeName;
    @Value("${fabric.network.orderer.endpoint}")
    private String ordererEndpoint;
    @Value("${fabric.network.orderer.tlsCert}")
    private String ordererTlsCertPath;

    // ── injected dependencies ──
    private final UniversityRepository universityRepo;
    private final AesEncryptionService aesService;

    // ── per-university channel cache ──
    private final Map<String, ChannelContext> channelCache = new ConcurrentHashMap<>();

    private record ChannelContext(HFClient client, Channel channel, ChaincodeID chaincodeId) {}

    public FabricCertificateService(UniversityRepository universityRepo,
                                     AesEncryptionService aesService) {
        this.universityRepo = universityRepo;
        this.aesService = aesService;
    }

    @PostConstruct
    public void init() {
        // Only BouncyCastle registration here — no hardcoded identity anymore
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    // ── core: build or return cached channel for a university ──
    private ChannelContext channelFor(String universityId) {
        return channelCache.computeIfAbsent(universityId, id -> {
            try {
                University uni = universityRepo.findById(id)
                        .orElseThrow(() -> new IllegalStateException(
                                "University not found: " + id));

                if (!uni.isActive()) {
                    throw new IllegalStateException(
                            "University account is inactive: " + uni.getName());
                }

                // Decrypt private key from MongoDB
                String privateKeyPem = aesService.decrypt(uni.getEncryptedPrivateKey());
                PrivateKey privateKey = readPrivateKeyFromPem(privateKeyPem);

                // Build Fabric identity from MongoDB fields
                Enrollment enrollment = new X509Enrollment(privateKey, uni.getCertPem());
                FabricUser user = new FabricUser(
                        "svc-" + uni.getUsername(),
                        uni.getMspId(),
                        enrollment
                );

                // Build HFClient for this university
                HFClient client = HFClient.createNewInstance();
                client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
                client.setUserContext(user);

                // Build channel using this university's peer
                Channel channel = client.newChannel(channelName);
                channel.addPeer(
                        client.newPeer(
                                uni.getPeerHostnameOverride(),
                                grpcUrl(uni.getPeerEndpoint()),
                                tlsPropertiesFromPem(
                                        uni.getPeerTlsCertPem(),
                                        uni.getPeerHostnameOverride()
                                )
                ));
                // Orderer is shared — still loaded from application.properties
                channel.addOrderer(
                        client.newOrderer(
                                "orderer.example.com",
                                grpcUrl(ordererEndpoint),
                                tlsProperties(ordererTlsCertPath, "orderer.example.com")
                ));
                channel.initialize();

                ChaincodeID ccId = ChaincodeID.newBuilder()
                        .setName(chaincodeName)
                        .build();

                return new ChannelContext(client, channel, ccId);

            } catch (Exception e) {
                throw new IllegalStateException(
                        "Failed to initialise Fabric channel for university: " + id, e);
            }
        });
    }

    // ── public API — all methods now take universityId ──

    public String createCertificate(String universityId,
                                     String certificateId,
                                     String studentId,
                                     String cid,
                                     String hash,
                                     String issuedBy,
                                     String status,
                                     String issuedAt) throws Exception {
        ChannelContext ctx = channelFor(universityId);
        submitTransaction(ctx, "CreateCertificate",
                certificateId, studentId, cid, hash, issuedBy, status, issuedAt);
        return certificateId;
    }

    public String getCertificate(String universityId,
                                  String certificateId) throws Exception {
        ChannelContext ctx = channelFor(universityId);
        return evaluateTransaction(ctx, "GetCertificate", certificateId);
    }

    public void updateCertificateStatus(String universityId,
                                         String certificateId,
                                         String status) throws Exception {
        ChannelContext ctx = channelFor(universityId);
        submitTransaction(ctx, "UpdateCertificateStatus", certificateId, status);
    }

    public boolean certificateExists(String universityId,
                                      String certificateId) throws Exception {
        ChannelContext ctx = channelFor(universityId);
        return Boolean.parseBoolean(
                evaluateTransaction(ctx, "CertificateExists", certificateId));
    }

    // Reads can use ANY university's channel — world state is shared
    // We use the calling university's channel for simplicity
    public String getAllCertificates(String universityId) throws Exception {
        ChannelContext ctx = channelFor(universityId);
        return evaluateTransaction(ctx, "GetAllCertificates");
    }

    // ── private helpers ──

    private String evaluateTransaction(ChannelContext ctx,
                                        String functionName,
                                        String... args) throws Exception {
        QueryByChaincodeRequest request = ctx.client().newQueryProposalRequest();
        request.setChaincodeID(ctx.chaincodeId());
        request.setFcn(functionName);
        request.setArgs(args);

        Collection<ProposalResponse> responses = ctx.channel().queryByChaincode(request);
        ProposalResponse response = successfulResponses(responses).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No successful Fabric query response"));

        return new String(response.getChaincodeActionResponsePayload(), StandardCharsets.UTF_8);
    }

    private void submitTransaction(ChannelContext ctx,
                                    String functionName,
                                    String... args) throws Exception {
        TransactionProposalRequest request = ctx.client().newTransactionProposalRequest();
        request.setChaincodeID(ctx.chaincodeId());
        request.setFcn(functionName);
        request.setArgs(args);

        Collection<ProposalResponse> responses = ctx.channel().sendTransactionProposal(request);
        Collection<ProposalResponse> successful = successfulResponses(responses);

        if (successful.isEmpty()) {
            throw new IllegalStateException("No successful Fabric endorsement responses");
        }

        BlockEvent.TransactionEvent event = ctx.channel()
                .sendTransaction(successful)
                .get(60, TimeUnit.SECONDS);

        if (!event.isValid()) {
            throw new IllegalStateException(
                    "Fabric transaction invalid: " + event.getTransactionID());
        }
    }

    private Collection<ProposalResponse> successfulResponses(
            Collection<ProposalResponse> responses) {
        Collection<ProposalResponse> failures = responses.stream()
                .filter(r -> r.getStatus() != ProposalResponse.Status.SUCCESS)
                .collect(Collectors.toList());

        if (!failures.isEmpty()) {
            String errors = failures.stream()
                    .map(r -> r.getPeer().getName() + ": " + r.getMessage())
                    .collect(Collectors.joining("; "));
            throw new IllegalStateException("Fabric proposal failed: " + errors);
        }
        return responses;
    }

    // Reads PEM from String instead of file path
    private PrivateKey readPrivateKeyFromPem(String privateKeyPem) throws Exception {
        try (PEMParser parser = new PEMParser(new StringReader(privateKeyPem))) {
            Object pemObject = parser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME);
            if (pemObject instanceof PEMKeyPair keyPair) {
                return converter.getPrivateKey(keyPair.getPrivateKeyInfo());
            }
            if (pemObject instanceof PrivateKeyInfo privateKeyInfo) {
                return converter.getPrivateKey(privateKeyInfo);
            }
            throw new IllegalArgumentException("Unsupported private key format");
        }
    }

    // TLS properties from PEM string — no file needed
    private Properties tlsPropertiesFromPem(String tlsCertPem, String hostnameOverride) {
        Properties properties = new Properties();
        properties.put("pemBytes", tlsCertPem.getBytes(StandardCharsets.UTF_8));
        properties.put("sslProvider", "openSSL");
        properties.put("negotiationType", "TLS");
        properties.put("hostnameOverride", hostnameOverride);
        return properties;
    }

    // Orderer still uses file path from application.properties
    private Properties tlsProperties(String tlsCertPath, String hostnameOverride) {
        Properties properties = new Properties();
        properties.put("pemFile", tlsCertPath);
        properties.put("sslProvider", "openSSL");
        properties.put("negotiationType", "TLS");
        properties.put("hostnameOverride", hostnameOverride);
        return properties;
    }

    private String grpcUrl(String endpoint) {
        if (endpoint.startsWith("grpc://") || endpoint.startsWith("grpcs://")) {
            return endpoint;
        }
        return "grpcs://" + endpoint;
    }

    // Unchanged inner class
    private static class FabricUser implements User {
        private final String name;
        private final String mspId;
        private final Enrollment enrollment;

        private FabricUser(String name, String mspId, Enrollment enrollment) {
            this.name = name;
            this.mspId = mspId;
            this.enrollment = enrollment;
        }

        @Override public String getName() { return name; }
        @Override public Set<String> getRoles() { return Collections.emptySet(); }
        @Override public String getAccount() { return null; }
        @Override public String getAffiliation() { return null; }
        @Override public Enrollment getEnrollment() { return enrollment; }
        @Override public String getMspId() { return mspId; }
    }

    // Add this public method
    public void evictChannelCache(String universityId) {
        ChannelContext ctx = channelCache.remove(universityId);
        if (ctx != null) {
            try {
                ctx.channel().shutdown(true);
            } catch (Exception ignored) {}
        }
    }
}
