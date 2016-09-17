package hackzurich;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HackzurichServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(HackzurichServerApplication.class, args);
    }
}
