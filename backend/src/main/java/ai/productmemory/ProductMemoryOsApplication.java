package ai.productmemory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ProductMemoryOsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductMemoryOsApplication.class, args);
    }
}
