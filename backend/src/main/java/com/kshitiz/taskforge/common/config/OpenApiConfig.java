package com.kshitiz.taskforge.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Title, description and security scheme shown in Swagger UI ({@code /swagger-ui.html}) and
 * the generated OpenAPI spec ({@code /v3/api-docs}).
 *
 * <p>Declares a "bearerAuth" scheme and applies it to every endpoint, so Swagger UI shows an
 * <b>Authorize</b> button: paste the access token from {@code /api/auth/login} there.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    /**
     * Describes the API for the generated documentation.
     *
     * @return OpenAPI metadata with JWT bearer authentication
     */
    @Bean
    public OpenAPI taskForgeOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TaskForge API")
                        .description("Project and task management API with AI-assisted planning")
                        .version("v1")
                        .license(new License().name("MIT")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
