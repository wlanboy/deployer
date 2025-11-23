package com.example.deployer.tokens;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data                   // generiert Getter, Setter, equals, hashCode, toString
@NoArgsConstructor      // generiert No-Args-Konstruktor
@AllArgsConstructor     // generiert All-Args-Konstruktor
public class OneTimeToken {
    @Id @GeneratedValue
    private Long id;

    private String token;
    
    private LocalDate validUntil; // null = noch nicht eingelöst
}