package com.enset.incidents;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// UserDetailsServiceAutoConfiguration exclu : on n'utilise pas le mot de passe genere par defaut
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class IncidentManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(IncidentManagerApplication.class, args);
    }
}
