package com.example.deployer.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data                   // generiert Getter, Setter, equals, hashCode, toString
@NoArgsConstructor      // generiert No-Args-Konstruktor
@AllArgsConstructor     // generiert All-Args-Konstruktor
public class Deployment {
    @Id
    private String id;
    private String name;
    private String repoId;
}
