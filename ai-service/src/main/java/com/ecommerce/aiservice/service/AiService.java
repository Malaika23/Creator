package com.ecommerce.aiservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.InMemoryVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AiService {

    private final ChatModel chatModel;
    private final EmbeddingModel embeddingModel;
    private InMemoryVectorStore vectorStore;
    private boolean isOllamaAvailable = false;

    // Use constructor injection, letting them be optional or catching initialization exceptions
    public AiService(ChatModel chatModel, EmbeddingModel embeddingModel) {
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
    }

    @PostConstruct
    public void init() {
        try {
            // Verify connection by calling embedding model with a dummy string
            embeddingModel.embed("health check");
            this.vectorStore = new InMemoryVectorStore(embeddingModel);
            this.isOllamaAvailable = true;
            log.info("Successfully connected to local Ollama AI model registry.");
            
            // Seed default catalog database into Vector Store
            seedMockProducts();
        } catch (Exception e) {
            log.error("Ollama AI models are not running locally on port 11434. Activating simulated fallback mode.", e);
            this.isOllamaAvailable = false;
        }
    }

    public List<Map<String, Object>> semanticSearch(String query) {
        if (!isOllamaAvailable) {
            log.info("Simulating keyword fallback search for: {}", query);
            return getSimulatedSearch(query);
        }

        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.query(query).withTopK(3)
        );

        return docs.stream()
                .map(doc -> Map.of(
                        "name", doc.getMetadata().get("name"),
                        "price", doc.getMetadata().get("price"),
                        "description", doc.getContent()
                ))
                .collect(Collectors.toList());
    }

    public String shoppingAssistantChat(String userQuery) {
        if (!isOllamaAvailable) {
            return "Ollama is currently offline. Simulating response:\n" +
                   "I found several comfortable products for you. You might like the 'Classic Leather Office Shoes' priced at ₹2500.";
        }

        // 1. Retrieve context using Vector Search
        List<Document> searchResults = vectorStore.similaritySearch(
                SearchRequest.query(userQuery).withTopK(3)
        );

        String context = searchResults.stream()
                .map(doc -> String.format("- Product: %s | Price: ₹%s | Description: %s", 
                        doc.getMetadata().get("name"), doc.getMetadata().get("price"), doc.getContent()))
                .collect(Collectors.joining("\n"));

        // 2. Generate System instruction and Chat Context
        String systemPrompt = "You are a helpful e-commerce shopping assistant. Use the following product catalog list to answer user queries. " +
                "If the query cannot be answered by the products below, suggest general advice politely.\n\n" +
                "Available Catalog:\n" + context + "\n\nUser Query: " + userQuery;

        log.info("Generating AI response from LLM for query: {}", userQuery);
        ChatResponse response = chatModel.call(new Prompt(new UserMessage(systemPrompt)));
        return response.getResult().getOutput().getContent();
    }

    public List<Map<String, Object>> getRecommendations(String productName) {
        // Find product similarities using embedding vectors
        return semanticSearch(productName);
    }

    private void seedMockProducts() {
        List<Document> seedDocs = List.of(
            new Document("Classic black leather shoes perfect for formal office wear and business meetings.", 
                Map.of("name", "Classic Leather Office Shoes", "price", 2499.00)),
            new Document("Comfortable daily running shoes with breathable mesh upper and memory foam insole.", 
                Map.of("name", "FlexRun Sport Sneakers", "price", 3499.00)),
            new Document("High-performance gaming laptop with 16GB RAM, RTX 4050, and 512GB SSD storage.", 
                Map.of("name", "Horizon Gaming Laptop", "price", 68999.00)),
            new Document("Wedding guest dress in maroon color with elegant silk lace work and embroidery.", 
                Map.of("name", "Silk Wedding Dress", "price", 4500.00))
        );
        vectorStore.accept(seedDocs);
        log.info("Seeded InMemoryVectorStore with {} default catalog embeddings.", seedDocs.size());
    }

    private List<Map<String, Object>> getSimulatedSearch(String query) {
        List<Map<String, Object>> catalog = List.of(
            Map.of("name", "Classic Leather Office Shoes", "price", 2499.00, "description", "Classic black leather shoes perfect for formal office wear and business meetings."),
            Map.of("name", "FlexRun Sport Sneakers", "price", 3499.00, "description", "Comfortable daily running shoes with breathable mesh upper and memory foam insole."),
            Map.of("name", "Horizon Gaming Laptop", "price", 68999.00, "description", "High-performance gaming laptop with 16GB RAM, RTX 4050, and 512GB SSD storage."),
            Map.of("name", "Silk Wedding Dress", "price", 4500.00, "description", "Wedding guest dress in maroon color with elegant silk lace work and embroidery.")
        );

        String lowercaseQuery = query.toLowerCase();
        return catalog.stream()
                .filter(item -> item.get("name").toString().toLowerCase().contains(lowercaseQuery) || 
                                item.get("description").toString().toLowerCase().contains(lowercaseQuery))
                .collect(Collectors.toList());
    }
}
