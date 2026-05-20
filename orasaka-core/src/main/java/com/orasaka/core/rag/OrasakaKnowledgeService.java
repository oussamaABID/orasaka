package com.orasaka.core.rag;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Retrieval-Augmented Generation (RAG).
 * Abstracts the underlying VectorStore provided by the host application.
 */
@Service
public class OrasakaKnowledgeService {

    private final VectorStore vectorStore;

    /**
     * Initializes the service with a vector store.
     * 
     * @param vectorStore The host application's {@link VectorStore}.
     */
    public OrasakaKnowledgeService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Retrieves relevant context for a given query.
     * 
     * @param query The user query.
     * @param topK Number of relevant documents to retrieve.
     * @return Formatted context string.
     */
    public String retrieveContext(String query, int topK) {
        if (vectorStore == null) return "";
        
        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .build()
        );

        return documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));
    }

    /**
     * Provides access to the underlying vector store.
     * 
     * @return The active {@link VectorStore}.
     */
    public VectorStore getVectorStore() {
        return vectorStore;
    }
}
