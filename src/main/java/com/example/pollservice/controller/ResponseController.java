package com.example.pollservice.controller;

import com.example.pollservice.model.Response;
import com.example.pollservice.service.ResponseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/responses")
public class ResponseController {

    @Autowired
    private ResponseService responseService;

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    // POST endpoint to save a response
    @PostMapping
    public ResponseEntity<Response> createResponse(@Valid @RequestBody Response response) {
        response.setUserId(getCurrentUserId());
        Response savedResponse = responseService.saveResponse(response);
        return new ResponseEntity<>(savedResponse, HttpStatus.CREATED);
    }

    // GET endpoint to retrieve all responses
    @GetMapping
    public ResponseEntity<List<Response>> getAllResponses() {
        String currentUserId = getCurrentUserId();
        List<Response> responses = responseService.getAllResponses().stream()
                .filter(response -> currentUserId != null && currentUserId.equals(response.getUserId()))
                .collect(Collectors.toList());
        return new ResponseEntity<>(responses, HttpStatus.OK);
    }

    // GET endpoint to retrieve a response by ID
    @GetMapping("/{id}")
    public ResponseEntity<Response> getResponseById(@PathVariable Long id) {
        String currentUserId = getCurrentUserId();
        return responseService.getResponseById(id)
                .filter(response -> currentUserId != null && currentUserId.equals(response.getUserId()))
                .map(response -> new ResponseEntity<>(response, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // DELETE endpoint to delete a response by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteResponse(@PathVariable Long id) {
        try {
            String currentUserId = getCurrentUserId();
            return responseService.getResponseById(id)
                    .map(response -> {
                        if (currentUserId == null || !currentUserId.equals(response.getUserId())) {
                            return new ResponseEntity<HttpStatus>(HttpStatus.FORBIDDEN);
                        }
                        responseService.deleteResponse(id);
                        return new ResponseEntity<HttpStatus>(HttpStatus.NO_CONTENT);
                    })
                    .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
