package ie.atu.cloudnative.catalogservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CatalogServiceApplication {

	public static void main(String[] args) {
        System.out.println("Hi");
		SpringApplication.run(CatalogServiceApplication.class, args);
	}

}
