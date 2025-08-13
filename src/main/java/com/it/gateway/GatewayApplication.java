package com.it.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@SpringBootApplication
@EnableAsync
@EnableTransactionManagement
public class GatewayApplication {
    public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

}
