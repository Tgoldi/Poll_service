package com.example.pollservice.controller;

import com.example.pollservice.model.Response;
import com.example.pollservice.security.AdminOnly;
import com.example.pollservice.security.RespondentIdentity;
import com.example.pollservice.security.RespondentTokenInterceptor;
import com.example.pollservice.service.ResponseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/responses")
public class ResponseController {

    @Autowired
    private ResponseService responseService;

    @Autowired
    private RespondentTokenInterceptor respondentTokenInterceptor;

    // POST endpoint to save a response
    @RespondentIdentity
    @PostMapping
    public ResponseEntity<Response> createResponse(@RequestBody Response response,
            @RequestAttribute(name = RespondentTokenInterceptor.VERIFIED_USER_ID, required = false) Long verifiedUserId) {
        // Always create a new row: a client-supplied id must not overwrite someone else's response.
        response.setId(null);
        if (respondentTokenInterceptor.isEnabled()) {
            if (verifiedUserId == null) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
            // The user is the one proven by the signed X-User-Token, never the one in the body.
            response.setUserId(verifiedUserId);
        }
        Response savedResponse = responseService.saveResponse(response);
        return new ResponseEntity<>(savedResponse, HttpStatus.CREATED);
    }

    // GET endpoint to retrieve all responses (operator only)
    @AdminOnly
    @GetMapping
    public ResponseEntity<List<Response>> getAllResponses() {
        List<Response> responses = responseService.getAllResponses();
        return new ResponseEntity<>(responses, HttpStatus.OK);
    }

    // GET endpoint to retrieve a response by ID (operator only)
    @AdminOnly
    @GetMapping("/{id}")
    public ResponseEntity<Response> getResponseById(@PathVariable Long id) {
        return responseService.getResponseById(id)
                .map(response -> new ResponseEntity<>(response, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // DELETE endpoint to delete a response by ID
    @AdminOnly
    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteResponse(@PathVariable Long id) {
        try {
            responseService.deleteResponse(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
