package org.alexismp.pdfmerger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PdfmergerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PdfmergerApplication.class, args);
	}

	@Bean
	CommandLineRunner init(StorageService storageService, OrderService orderService) {
		return (args) -> {
			storageService.init();
			orderService.init();
		};
	}

	// @Bean
	// CommandLineRunner init(OrderService orderService) {
	// 	return (args) -> {
	// 		orderService.init();
	// 	};
	// }
}
