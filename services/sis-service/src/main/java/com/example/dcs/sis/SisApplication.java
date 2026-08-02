package com.example.dcs.sis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// No JDBC/JPA on the classpath, so the old DataSource/HibernateJpa auto-config
// excludes are unnecessary in Spring Boot 4 (those auto-config modules aren't present).
@SpringBootApplication
public class SisApplication {

	public static void main(String[] args) {
		SpringApplication.run(SisApplication.class, args);
	}

}
