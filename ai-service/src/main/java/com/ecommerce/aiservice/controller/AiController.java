package com.ecommerce.aiservice.controller;

import com.ecommerce.aiservice.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> search(@RequestParam String query) {
        List<Map<String, Object>> results = aiService.semanticSearch(query);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/assistant/chat")
    public ResponseEntity<String> chat(@RequestBody Map<String, String> request) {
        String message = request.getOrDefault("message", "");
        String aiResponse = aiService.shoppingAssistantChat(message);
        return ResponseEntity.ok(aiResponse);
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<Map<String, Object>>> getRecommendations(@RequestParam String productName) {
        List<Map<String, Object>> recommendations = aiService.getRecommendations(productName);
        return ResponseEntity.ok(recommendations);
    }
}
