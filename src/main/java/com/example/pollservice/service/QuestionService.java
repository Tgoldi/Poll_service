package com.example.pollservice.service;

import com.example.pollservice.model.Question;
import com.example.pollservice.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class QuestionService {

    @Autowired
    private QuestionRepository questionRepository;

    // Method to save or update a question
    public Question saveQuestion(Question question) {
        return questionRepository.save(question);
    }

    // Method to fetch all questions
    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
    }

    // Method to find a question by its ID
    public Optional<Question> getQuestionById(Long id) {
        return questionRepository.findById(id);
    }

    // Method to delete a question
    public void deleteQuestion(Long id) {
        questionRepository.deleteById(id);
    }
}
