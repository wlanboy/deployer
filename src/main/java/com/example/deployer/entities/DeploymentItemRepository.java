package com.example.deployer.entities;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeploymentItemRepository extends JpaRepository<DeploymentItem, Long> {
    List<DeploymentItem> findByDeploymentId(String deploymentId);
}

