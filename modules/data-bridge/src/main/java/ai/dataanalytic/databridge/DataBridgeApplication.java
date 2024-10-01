package ai.dataanalytic.databridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(scanBasePackages = "ai.dataanalytic.databridge")
public class DataBridgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataBridgeApplication.class, args);
    }

}