package com.example.pollservice.controller;

import com.example.pollservice.model.Question;
import com.example.pollservice.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/questions")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    // Admin token must be configured (property "admin.api.token"); requests must send it in X-Admin-Token
    @Value("${admin.api.token:}")
    private String adminToken;

    private boolean isAdmin(String providedToken) {
        if (adminToken == null || adminToken.isEmpty() || providedToken == null) {
            return false;
        }
        return java.security.MessageDigest.isEqual(
                adminToken.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                providedToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    // POST endpoint to create a new question
    @PostMapping
    public ResponseEntity<Question> createQuestion(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestBody Question question) {
        if (!isAdmin(token)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        Question savedQuestion = questionService.saveQuestion(question);
        return new ResponseEntity<>(savedQuestion, HttpStatus.CREATED);
    }

    // GET endpoint to retrieve all questions
    @GetMapping
    public ResponseEntity<List<Question>> getAllQuestions() {
        List<Question> questions = questionService.getAllQuestions();
        return new ResponseEntity<>(questions, HttpStatus.OK);
    }

    // GET endpoint to retrieve a single question by ID
    @GetMapping("/{id}")
    public ResponseEntity<Question> getQuestionById(@PathVariable Long id) {
        return questionService.getQuestionById(id)
                .map(question -> new ResponseEntity<>(question, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // DELETE endpoint to remove a question by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteQuestion(
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @PathVariable Long id) {
        if (!isAdmin(token)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        try {
            questionService.deleteQuestion(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
