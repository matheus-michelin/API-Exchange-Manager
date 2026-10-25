package com.manager.money.exchange.manager;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AlphaApplication {

	public static void main(String[] args) {

		SpringApplication app = new SpringApplication(AlphaApplication.class);
		app.setBannerMode(Banner.Mode.OFF);
		app.run(args);
	}

}
