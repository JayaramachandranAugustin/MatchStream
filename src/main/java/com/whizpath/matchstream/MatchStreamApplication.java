package com.whizpath.matchstream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@SpringBootApplication
@EnableKafkaStreams
public class MatchStreamApplication {

	public static void main(String[] args) {
		SpringApplication.run(MatchStreamApplication.class, args);
	}

}
