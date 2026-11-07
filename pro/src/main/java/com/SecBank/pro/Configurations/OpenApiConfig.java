package com.SecBank.pro.Configurations;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Documentation OpenAPI servie sur /v3/api-docs, interface sur /swagger-ui.html.
 * Toutes les routes sauf l'authentification exigent un jeton Bearer JWT.
 */
@Configuration
public class OpenApiConfig {

    public static final String SCHEMA_SECURITE = "bearerAuth";

    @Value("${server.port:8080}")
    private int port;

    @Bean
    public OpenAPI eBankOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("E-Bank API")
                        .version("1.0.0")
                        .description("""
                                API de la banque de demonstration.
                                Authentification : POST /api/auth/login renvoie un jeton JWT (champ `token`).
                                Les routes /api/admin/** exigent le role ADMIN, /api/client/** le role CLIENT.
                                Codes transverses : 401 sans jeton valide, 403 role insuffisant.
                                """))
                .servers(List.of(new Server().url("http://localhost:" + port).description("Local")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEMA_SECURITE))
                .components(new Components().addSecuritySchemes(SCHEMA_SECURITE,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Jeton renvoye par /api/auth/login (champ `token`).")));
    }
}
