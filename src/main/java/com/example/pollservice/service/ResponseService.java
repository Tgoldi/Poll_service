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

    /**
     * Saves a new response unless this user already answered this question.
     * Returns null when a response from the same user to the same question exists.
     */
    public synchronized Response saveNewResponse(Response response) {
        Long questionId = response.getQuestion() == null ? null : response.getQuestion().getId();
        if (questionId != null && response.getUserId() != null
                && responseRepository.existsByQuestion_IdAndUserId(questionId, response.getUserId())) {
            return null;
        }
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
