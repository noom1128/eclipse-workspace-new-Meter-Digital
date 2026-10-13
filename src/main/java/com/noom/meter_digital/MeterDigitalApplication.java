package com.noom.meter_digital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MeterDigitalApplication {

	public static void main(String[] args) {
		SpringApplication.run(MeterDigitalApplication.class, args);
	}

}


