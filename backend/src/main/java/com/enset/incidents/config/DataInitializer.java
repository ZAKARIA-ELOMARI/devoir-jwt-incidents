package com.enset.incidents.config;

import com.enset.incidents.entity.Role;
import com.enset.incidents.entity.User;
import com.enset.incidents.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Cree les comptes de test au demarrage (ADMIN et TECHNICIAN ne peuvent pas s'inscrire eux-memes).
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        create("Administrateur", "admin@enset.ma", "Admin123!", Role.ADMIN);
        create("Technicien A", "tech1@enset.ma", "Tech123!", Role.TECHNICIAN);
        create("Technicien B", "tech2@enset.ma", "Tech123!", Role.TECHNICIAN);
        create("Ahmed Benali", "emp1@enset.ma", "Emp123!", Role.EMPLOYEE);
        create("Sara Alaoui", "emp2@enset.ma", "Emp123!", Role.EMPLOYEE);
        log.info("Comptes de test prets (voir README.md)");
    }

    private void create(String fullName, String email, String password, Role role) {
        if (!userRepository.existsByEmail(email)) {
            userRepository.save(new User(fullName, email, passwordEncoder.encode(password), role));
        }
    }
}
