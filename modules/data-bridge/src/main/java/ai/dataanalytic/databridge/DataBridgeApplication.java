package ai.dataanalytic.databridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "ai.dataanalytic.databridge",
        "ai.dataanalytic.querybridge.service",
        "ai.dataanalytic.querybridge.config"
})
public class DataBridgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataBridgeApplication.class, args);
    }

}
