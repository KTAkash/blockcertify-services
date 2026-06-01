package com.akash.credential_verification.Service;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.Security;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class FabricCertificateService {

    @Value("${fabric.network.channelName}")
    private String channelName;

    @Value("${fabric.network.chaincodeName}")
    private String chaincodeName;

    @Value("${fabric.network.mspId}")
    private String mspId;

    @Value("${fabric.network.peer.endpoint}")
    private String peerEndpoint;

    @Value("${fabric.network.peer.tlsCert}")
    private String peerTlsCertPath;

    @Value("${fabric.network.peer.overrideAuth}")
    private String peerOverrideAuth;

    @Value("${fabric.network.orderer.endpoint}")
    private String ordererEndpoint;

    @Value("${fabric.network.orderer.tlsCert}")
    private String ordererTlsCertPath;

    @Value("${fabric.network.identity.cert}")
    private String identityCertPath;

    @Value("${fabric.network.identity.key}")
    private String identityKeyPath;

    private HFClient client;
    private Channel channel;
    private ChaincodeID chaincodeId;

    @PostConstruct
    public void init() throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        client = HFClient.createNewInstance();
        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        client.setUserContext(loadUser());

        channel = client.newChannel(channelName);
        channel.addPeer(client.newPeer("peer0.org1.example.com", grpcUrl(peerEndpoint),
                tlsProperties(peerTlsCertPath, peerOverrideAuth)));
        channel.addOrderer(client.newOrderer("orderer.example.com", grpcUrl(ordererEndpoint),
                tlsProperties(ordererTlsCertPath, "orderer.example.com")));
        channel.initialize();

        chaincodeId = ChaincodeID.newBuilder()
                .setName(chaincodeName)
                .build();
    }

    public String createCertificate(
            String certificateId,
            String studentId,
            String cid,
            String hash,
            String issuedBy,
            String status,
            String issuedAt
    ) throws Exception {
        submitTransaction(
                "CreateCertificate",
                certificateId,
                studentId,
                cid,
                hash,
                issuedBy,
                status,
                issuedAt
        );
        return certificateId;
    }

    public String getCertificate(String certificateId) throws Exception {
        return evaluateTransaction("GetCertificate", certificateId);
    }

    public void updateCertificateStatus(String certificateId, String status) throws Exception {
        submitTransaction("UpdateCertificateStatus", certificateId, status);
    }

    public boolean certificateExists(String certificateId) throws Exception {
        return Boolean.parseBoolean(evaluateTransaction("CertificateExists", certificateId));
    }

    public String getAllCertificates() throws Exception {
        return evaluateTransaction("GetAllCertificates");
    }

    private String evaluateTransaction(String functionName, String... args) throws Exception {
        QueryByChaincodeRequest request = client.newQueryProposalRequest();
        request.setChaincodeID(chaincodeId);
        request.setFcn(functionName);
        request.setArgs(args);

        Collection<ProposalResponse> responses = channel.queryByChaincode(request);
        ProposalResponse response = successfulResponses(responses).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No successful Fabric query response"));

        return new String(response.getChaincodeActionResponsePayload(), StandardCharsets.UTF_8);
    }

    private void submitTransaction(String functionName, String... args) throws Exception {
        TransactionProposalRequest request = client.newTransactionProposalRequest();
        request.setChaincodeID(chaincodeId);
        request.setFcn(functionName);
        request.setArgs(args);

        Collection<ProposalResponse> responses = channel.sendTransactionProposal(request);
        Collection<ProposalResponse> successfulResponses = successfulResponses(responses);

        if (successfulResponses.isEmpty()) {
            throw new IllegalStateException("No successful Fabric endorsement responses");
        }

        BlockEvent.TransactionEvent event = channel.sendTransaction(successfulResponses)
                .get(60, TimeUnit.SECONDS);

        if (!event.isValid()) {
            throw new IllegalStateException("Fabric transaction was committed as invalid: " + event.getTransactionID());
        }
    }

    private Collection<ProposalResponse> successfulResponses(Collection<ProposalResponse> responses) {
        Collection<ProposalResponse> failures = responses.stream()
                .filter(response -> response.getStatus() != ProposalResponse.Status.SUCCESS)
                .collect(Collectors.toList());

        if (!failures.isEmpty()) {
            String errors = failures.stream()
                    .map(response -> response.getPeer().getName() + ": " + response.getMessage())
                    .collect(Collectors.joining("; "));
            throw new IllegalStateException("Fabric proposal failed: " + errors);
        }

        return responses;
    }

    private FabricUser loadUser() throws Exception {
        String certificate = readConfiguredFile(identityCertPath, "Fabric identity certificate");
        PrivateKey privateKey = readPrivateKey(identityKeyPath);
        Enrollment enrollment = new X509Enrollment(privateKey, certificate);
        return new FabricUser("User1", mspId, enrollment);
    }

    private String readConfiguredFile(String configuredPath, String description) throws Exception {
        if (configuredPath == null || configuredPath.isBlank()) {
            throw new IllegalStateException(description + " path is not configured");
        }

        if (configuredPath.contains("BEGIN ")) {
            throw new IllegalStateException(description + " must be configured as a file path, not PEM contents");
        }

        Path path = Path.of(configuredPath);
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException(description + " file does not exist: " + path.toAbsolutePath());
        }

        return Files.readString(path);
    }

    private PrivateKey readPrivateKey(String keyPath) throws Exception {
        String privateKeyPem = readConfiguredFile(keyPath, "Fabric identity private key");
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

            throw new IllegalArgumentException("Unsupported private key format: " + Path.of(keyPath).toAbsolutePath());
        }
    }

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

    private static class FabricUser implements User {
        private final String name;
        private final String mspId;
        private final Enrollment enrollment;

        private FabricUser(String name, String mspId, Enrollment enrollment) {
            this.name = name;
            this.mspId = mspId;
            this.enrollment = enrollment;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Set<String> getRoles() {
            return Collections.emptySet();
        }

        @Override
        public String getAccount() {
            return null;
        }

        @Override
        public String getAffiliation() {
            return null;
        }

        @Override
        public Enrollment getEnrollment() {
            return enrollment;
        }

        @Override
        public String getMspId() {
            return mspId;
        }
    }
}
