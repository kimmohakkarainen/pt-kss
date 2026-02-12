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
                        .description("REST API for EPUB processing: upload, mandatory metadata completion, image alt text review, language markup review, and download. Processing may pause for user input (awaiting-metadata, awaiting-alt-texts, awaiting-lang-markup-review); use the corresponding endpoints to complete and resume.")
                        .version("1.0"));
    }
}
