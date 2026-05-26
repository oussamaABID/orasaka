package com.orasaka.workers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Isolated Spring Boot application for executing approved automation jobs. */
@SpringBootApplication
public class ExternalServicesWorkerApplication {

  public static void main(String[] args) {
    SpringApplication.run(ExternalServicesWorkerApplication.class, args);
  }
}
