package com.example.pollservice.controller;

import com.example.pollservice.model.Response;
import com.example.pollservice.security.AdminOnly;
import com.example.pollservice.security.CastsVote;
import com.example.pollservice.security.ProtectedRead;
import com.example.pollservice.security.VoteGuardInterceptor;
import com.example.pollservice.service.ResponseService;
import jakarta.servlet.http.HttpServletRequest;
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

    // POST endpoint to save a response
    @CastsVote
    @PostMapping
    public ResponseEntity<Response> createResponse(@RequestBody Response response, HttpServletRequest request) {
        // Always create a new row: a client-supplied id must not overwrite someone else's response.
        response.setId(null);
        // When signed user identities are on, the user is the one the server verified, never the body's userId.
        Object verifiedUserId = request.getAttribute(VoteGuardInterceptor.VERIFIED_USER_ID);
        if (!(verifiedUserId instanceof Long userId)) {
            // Identity is not verified (POLL_USER_TOKEN_SECRET unset): the body's userId is unproven, so it must
            // not be able to use up another user's vote. Save as before, without the one-vote-per-user rule.
            return new ResponseEntity<>(responseService.saveResponse(response), HttpStatus.CREATED);
        }
        response.setUserId(userId);
        Response savedResponse = responseService.saveNewResponse(response);
        if (savedResponse == null) {
            // One response per user per question.
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
        return new ResponseEntity<>(savedResponse, HttpStatus.CREATED);
    }

    // GET endpoint to retrieve all responses
    @ProtectedRead
    @GetMapping
    public ResponseEntity<List<Response>> getAllResponses() {
        List<Response> responses = responseService.getAllResponses();
        return new ResponseEntity<>(responses, HttpStatus.OK);
    }

    // GET endpoint to retrieve a response by ID
    @ProtectedRead
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
