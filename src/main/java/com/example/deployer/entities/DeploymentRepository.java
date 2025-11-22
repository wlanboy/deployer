package com.example.deployer.entities;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeploymentRepository extends JpaRepository<Deployment, String> {}
