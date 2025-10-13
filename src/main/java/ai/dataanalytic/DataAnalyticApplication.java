package ai.dataanalytic;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class DataAnalyticApplication {

    public static void main(String[] args) {
        log.info("Starting DataAnalyticApplication...");
        SpringApplication.run(DataAnalyticApplication.class, args);

    }
}
