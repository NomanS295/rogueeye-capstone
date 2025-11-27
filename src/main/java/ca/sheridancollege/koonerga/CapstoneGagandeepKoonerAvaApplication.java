package ca.sheridancollege.koonerga;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
    "ca.sheridancollege.koonerga",
    "ca.noman.shahid"
})
public class CapstoneGagandeepKoonerAvaApplication {
    public static void main(String[] args) {
        SpringApplication.run(CapstoneGagandeepKoonerAvaApplication.class, args);
    }
}

