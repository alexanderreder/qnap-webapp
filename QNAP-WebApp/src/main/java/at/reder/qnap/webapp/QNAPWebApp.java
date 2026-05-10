package at.reder.qnap.webapp;

import java.util.Collections;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Umbrella application for QNAP NAS.
 */
@SpringBootApplication
public class QNAPWebApp {

    public static void main(String[] args) {
        // Start Car Statistics on port 8081
        SpringApplication carApp = new SpringApplication(at.reder.carstatistics.Carstatistics.class);
        carApp.setDefaultProperties(Collections.singletonMap("server.port", "8081"));
        carApp.run(args);

        // Start Teaching on port 8082
        SpringApplication teachingApp = new SpringApplication(at.reder.teaching.webapp.WebApp.class);
        teachingApp.setDefaultProperties(Collections.singletonMap("server.port", "8082"));
        teachingApp.run(args);

        // Start QNAP Hub on port 8080
        SpringApplication hubApp = new SpringApplication(QNAPWebApp.class);
        hubApp.setDefaultProperties(Collections.singletonMap("server.port", "8080"));
        hubApp.run(args);
    }
}