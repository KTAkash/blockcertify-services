package com.akash.credential_verification.Configuration;

import io.ipfs.api.IPFS;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

@Configuration
public class IpfsConfig {

    @Bean
    public IPFS ipfsClient(@Value("${ipfs.api.url:http://127.0.0.1:5001}") String apiUrl) {
        URI uri = URI.create(apiUrl);
        String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
        String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
        int port = uri.getPort() == -1 ? 5001 : uri.getPort();

        return new IPFS(host, port, "/api/v0/", false, "https".equalsIgnoreCase(scheme));
    }
}
