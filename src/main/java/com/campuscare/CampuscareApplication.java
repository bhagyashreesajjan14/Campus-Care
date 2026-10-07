package com.campuscare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.campuscare.model.AppUser;
import com.campuscare.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class CampuscareApplication {

	public static void main(String[] args) {
		SpringApplication.run(CampuscareApplication.class, args);
	}

	@Bean
	CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			if (userRepository.findByUsername("admin").isEmpty()) {
				AppUser admin = new AppUser();
				admin.setUsername("admin");
				admin.setPassword(passwordEncoder.encode("admin123"));
				admin.setName("Super Admin");
				admin.setEmail("admin@campuscare.com");
				admin.setRole("ROLE_SUPER_ADMIN");
				userRepository.save(admin);
			}
            
            if (userRepository.findByUsername("hosteladmin").isEmpty()) {
				AppUser hostelAdmin = new AppUser();
				hostelAdmin.setUsername("hosteladmin");
				hostelAdmin.setPassword(passwordEncoder.encode("hostel123"));
				hostelAdmin.setName("Hostel Admin");
				hostelAdmin.setEmail("hostel@campuscare.com");
				hostelAdmin.setRole("ROLE_DEPT_ADMIN");
				userRepository.save(hostelAdmin);
			}
            
            if (userRepository.findByUsername("collegeadmin").isEmpty()) {
				AppUser collegeAdmin = new AppUser();
				collegeAdmin.setUsername("collegeadmin");
				collegeAdmin.setPassword(passwordEncoder.encode("college123"));
				collegeAdmin.setName("College Admin");
				collegeAdmin.setEmail("college@campuscare.com");
				collegeAdmin.setRole("ROLE_DEPT_ADMIN");
				userRepository.save(collegeAdmin);
			}
		};
	}
}
