package br.com.marceloscoleso.quality_evaluator_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class QualityEvaluatorApiApplication {

    public static void main(String[] args) {
        
        Dotenv dotenv = Dotenv.load();

        
        System.setProperty("SPRING_DATASOURCE_URL", dotenv.get("SPRING_DATASOURCE_URL"));
        System.setProperty("SPRING_DATASOURCE_USERNAME", dotenv.get("SPRING_DATASOURCE_USERNAME"));
        System.setProperty("SPRING_DATASOURCE_PASSWORD", dotenv.get("SPRING_DATASOURCE_PASSWORD"));
        System.setProperty("SERVER_PORT", dotenv.get("SERVER_PORT"));
        System.setProperty("JWT_SECRET", dotenv.get("JWT_SECRET"));


       
        SpringApplication.run(QualityEvaluatorApiApplication.class, args);
    }
}