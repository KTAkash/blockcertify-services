package com.akash.credential_verification.Service;

import com.akash.credential_verification.DTO.FileUploadResponse;
import com.akash.credential_verification.Model.FileMetadata;
import com.akash.credential_verification.Repository.FileMetadataRepository;
import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;
import io.ipfs.multihash.Multihash;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileService {

    private final IPFS ipfs;
    private final FileMetadataRepository fileMetadataRepository;

    @Value("${ipfs.gateway.url:https://ipfs.io/ipfs/}")
    private String gatewayUrl;

    public FileUploadResponse upload(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        byte[] bytes = file.getBytes();
        String fileName = safeFileName(file.getOriginalFilename());
        String contentType = resolveContentType(file.getContentType(), fileName);
        String hash = sha256(bytes);

        NamedStreamable.ByteArrayWrapper ipfsFile = new NamedStreamable.ByteArrayWrapper(fileName, bytes);
        List<MerkleNode> nodes = ipfs.add(ipfsFile);
        if (nodes.isEmpty() || nodes.getFirst().hash == null) {
            throw new IOException("IPFS did not return a CID");
        }

        String cid = nodes.getFirst().hash.toString();
        FileMetadata metadata = new FileMetadata();
        metadata.setCid(cid);
        metadata.setFileName(fileName);
        metadata.setContentType(contentType);
        metadata.setSize(file.getSize());
        metadata.setHash(hash);
        metadata.setGatewayUrl(buildGatewayUrl(cid));
        metadata.setUploadedAt(Instant.now());
        fileMetadataRepository.save(metadata);

        return toResponse(metadata);
    }

    public PreviewFile getPreviewFile(String cid) throws IOException {
        FileMetadata metadata = fileMetadataRepository.findFirstByCid(cid).orElse(null);
        byte[] bytes = ipfs.cat(Multihash.fromBase58(cid));
        String fileName = metadata == null ? cid : metadata.getFileName();
        String contentType = metadata == null
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : resolveContentType(metadata.getContentType(), metadata.getFileName());

        Resource resource = new NamedByteArrayResource(bytes, fileName);
        return new PreviewFile(resource, contentType, fileName);
    }

    public String getGatewayUrl(String cid) {
        return buildGatewayUrl(cid);
    }

    private FileUploadResponse toResponse(FileMetadata metadata) {
        return new FileUploadResponse(
                metadata.getCid(),
                metadata.getFileName(),
                metadata.getContentType(),
                metadata.getSize(),
                metadata.getHash(),
                metadata.getGatewayUrl()
        );
    }

    private String buildGatewayUrl(String cid) {
        String baseUrl = gatewayUrl.endsWith("/") ? gatewayUrl : gatewayUrl + "/";
        return baseUrl + cid;
    }

    private String resolveContentType(String contentType, String fileName) {
        if (contentType != null && !contentType.isBlank()) {
            return contentType;
        }

        return org.springframework.http.MediaTypeFactory.getMediaType(fileName)
                .map(MediaType::toString)
                .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }

    private String safeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "upload";
        }

        return fileName.replace("\\", "_").replace("/", "_");
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public record PreviewFile(Resource resource, String contentType, String fileName) {
    }

    private static final class NamedByteArrayResource extends ByteArrayResource {
        private final String fileName;

        private NamedByteArrayResource(byte[] byteArray, String fileName) {
            super(byteArray);
            this.fileName = fileName;
        }

        @Override
        public String getFilename() {
            return fileName;
        }
    }
}
