package fi.publishertools.kss;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import fi.publishertools.kss.integration.ollama.OllamaCacheProperties;

@SpringBootApplication
@EnableConfigurationProperties({ UploadProperties.class, OllamaCacheProperties.class })
public class KssApplication {

	public static void main(String[] args) {
		SpringApplication.run(KssApplication.class, args);
	}

}
