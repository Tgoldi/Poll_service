package com.example.pollservice.service;

import com.example.pollservice.model.Response;
import com.example.pollservice.repository.ResponseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ResponseService {

    @Autowired
    private ResponseRepository responseRepository;

    // Method to save a response
    public Response saveResponse(Response response) {
        return responseRepository.save(response);
    }

    // Method to fetch all responses
    public List<Response> getAllResponses() {
        return responseRepository.findAll();
    }

    // Method to find a response by its ID
    public Optional<Response> getResponseById(Long id) {
        return responseRepository.findById(id);
    }

    // Method to delete a response by ID
    public void deleteResponse(Long id) {
        responseRepository.deleteById(id);
    }
}
