package fi.publishertools.kss;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI kssOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PT-KSS API")
                        .description("REST API for EPUB processing and metadata management")
                        .version("1.0"));
    }
}
