package com.dynamicdashboard.cockpit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CockpitBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(CockpitBackendApplication.class, args);
    }
}
