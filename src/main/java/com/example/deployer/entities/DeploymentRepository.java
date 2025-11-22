package com.example.deployer.entities;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeploymentRepository extends JpaRepository<Deployment, String> {

    List<Deployment> findByRepoId(String repoId);

    List<Deployment> findByName(String name);

    boolean existsByIdAndRepoId(String id, String repoId);
}
