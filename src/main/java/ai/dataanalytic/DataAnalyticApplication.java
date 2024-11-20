package ai.dataanalytic;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
public class DataAnalyticApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataAnalyticApplication.class, args);
    }
}
