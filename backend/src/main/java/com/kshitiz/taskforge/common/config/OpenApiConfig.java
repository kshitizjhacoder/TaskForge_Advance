package com.kshitiz.taskforge.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Title, description and version shown at the top of Swagger UI
 * ({@code /swagger-ui.html}) and in the generated OpenAPI spec ({@code /v3/api-docs}).
 */
@Configuration
public class OpenApiConfig {

    /**
     * Describes the API for the generated documentation.
     *
     * @return OpenAPI metadata
     */
    @Bean
    public OpenAPI taskForgeOpenApi() {
        return new OpenAPI().info(new Info()
                .title("TaskForge API")
                .description("Project and task management API with AI-assisted planning")
                .version("v1")
                .license(new License().name("MIT")));
    }
}
