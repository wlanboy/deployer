package com.example.deployer.entities;

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
public class DeploymentItem {
    @Id
    @GeneratedValue
    private Long id;

    private String deploymentId;
    private String repoId; 
    private String playbook;
    private String inventory;
    private String tags;
    private String skipTags;
    private String hostLimit;
}
