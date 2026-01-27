package fi.publishertools.kss;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(UploadProperties.class)
public class KssApplication {

	public static void main(String[] args) {
		SpringApplication.run(KssApplication.class, args);
	}

}
