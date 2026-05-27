package com.travolish.traveller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TravellerServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TravellerServiceApplication.class, args);
	}

}
