package com.xcelore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class XceloreApplication {
    public static void main(String[] args) {
        SpringApplication.run(XceloreApplication.class, args);
    }
}

// Swagger Configuration
package com.xcelore;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI doctorApiDoc() {
        return new OpenAPI().info(new Info().
                title("Doctor Suggestion API").
                description("Spring Boot API for Xcelore Assignment").
                version("1.0.0"));
    }
}

// ENTITY: Doctor
package com.xcelore.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Doctor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(min = 3, message = "Name must be at least 3 characters")
    private String name;

    @Size(max = 20, message = "City must be at most 20 characters")
    @Pattern(regexp = "Delhi|Noida|Faridabad", message = "City must be Delhi, Noida or Faridabad")
    private String city;

    @Email
    private String email;

    @Size(min = 10, message = "Phone number must be at least 10 digits")
    private String phone;

    @Pattern(regexp = "Orthopaedic|Gynecology|Dermatology|ENT", message = "Invalid speciality")
    private String speciality;
}

// ENTITY: Patient
package com.xcelore.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(min = 3, message = "Name must be at least 3 characters")
    private String name;

    @Size(max = 20, message = "City must be at most 20 characters")
    private String city;

    @Email
    private String email;

    @Size(min = 10, message = "Phone number must be at least 10 digits")
    private String phone;

    @Pattern(regexp = "Arthritis|Back Pain|Tissue injuries|Dysmenorrhea|Skin infection|skin burn|Ear pain", message = "Invalid symptom")
    private String symptom;
}

// REPOSITORIES
package com.xcelore.repository;

import com.xcelore.model.Doctor;
import com.xcelore.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    List<Doctor> findByCityAndSpeciality(String city, String speciality);
}

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {}

// SERVICE
package com.xcelore.service;

import com.xcelore.model.Doctor;
import com.xcelore.model.Patient;
import com.xcelore.repository.DoctorRepository;
import com.xcelore.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SuggestionService {

    private static final Map<String, String> SYMPTOM_SPECIALITY = Map.of(
        "Arthritis", "Orthopaedic",
        "Back Pain", "Orthopaedic",
        "Tissue injuries", "Orthopaedic",
        "Dysmenorrhea", "Gynecology",
        "Skin infection", "Dermatology",
        "skin burn", "Dermatology",
        "Ear pain", "ENT"
    );

    @Autowired
    private PatientRepository patientRepo;

    @Autowired
    private DoctorRepository doctorRepo;

    public Object suggestDoctor(Long patientId) {
        Optional<Patient> optionalPatient = patientRepo.findById(patientId);
        if (optionalPatient.isEmpty()) return "Patient not found";

        Patient patient = optionalPatient.get();
        String city = patient.getCity();
        String symptom = patient.getSymptom();

        if (!List.of("Delhi", "Noida", "Faridabad").contains(city))
            return "We are still waiting to expand to your location";

        String speciality = SYMPTOM_SPECIALITY.get(symptom);
        if (speciality == null) return "Symptom does not match any speciality";

        List<Doctor> doctors = doctorRepo.findByCityAndSpeciality(city, speciality);
        return doctors.isEmpty() ?
            "There isn’t any doctor present at your location for your symptom" :
            doctors;
    }
}

// CONTROLLER
package com.xcelore.controller;

import com.xcelore.model.Doctor;
import com.xcelore.model.Patient;
import com.xcelore.repository.DoctorRepository;
import com.xcelore.repository.PatientRepository;
import com.xcelore.service.SuggestionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Doctor Suggestion API", description = "APIs to manage doctors, patients and suggest doctors based on symptoms")
public class ApiController {

    @Autowired
    private DoctorRepository doctorRepo;

    @Autowired
    private PatientRepository patientRepo;

    @Autowired
    private SuggestionService suggestionService;

    @PostMapping("/doctors")
    public Doctor addDoctor(@RequestBody @Valid Doctor doctor) {
        return doctorRepo.save(doctor);
    }

    @PostMapping("/patients")
    public Patient addPatient(@RequestBody @Valid Patient patient) {
        return patientRepo.save(patient);
    }

    @GetMapping("/suggest-doctor/{patientId}")
    public Object suggestDoctor(@PathVariable Long patientId) {
        return suggestionService.suggestDoctor(patientId);
    }

    @DeleteMapping("/doctors/{id}")
    public void deleteDoctor(@PathVariable Long id) {
        doctorRepo.deleteById(id);
    }

    @DeleteMapping("/patients/{id}")
    public void deletePatient(@PathVariable Long id) {
        patientRepo.deleteById(id);
    }
}

// application.properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
spring.jpa.hibernate.ddl-auto=update
springdoc.api-docs.path=/api-docs