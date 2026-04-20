package campus.ai.rag.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CampusAiRagServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusAiRagServiceApplication.class, args);
    }

}
