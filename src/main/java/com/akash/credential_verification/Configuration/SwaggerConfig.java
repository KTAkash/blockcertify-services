package com.akash.credential_verification.Configuration;


import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI credentialOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Credential_Verification")
                        .description("API for Credentials Verification")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Thiben Akash")
                                .email("thibenakash171@gmail.com")
                        )
                )
                .externalDocs(new ExternalDocumentation()
                        .description("Project Documentation")
                        .url("https://your-project-docs-url.com"));
    }
}