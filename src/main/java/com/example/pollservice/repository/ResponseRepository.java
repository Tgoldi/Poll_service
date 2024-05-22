package com.example.pollservice.repository;

import com.example.pollservice.model.Response;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResponseRepository extends JpaRepository<Response, Long> {
    // Additional queries to fetch responses by question or user could be added here
}
