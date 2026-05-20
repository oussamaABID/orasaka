package com.orasaka.core.rag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrasakaKnowledgeServiceTest {

    @Mock
    private VectorStore vectorStore;

    private OrasakaKnowledgeService service;

    @BeforeEach
    void setUp() {
        service = new OrasakaKnowledgeService(vectorStore);
    }

    @Test
    void shouldRetrieveFormattedContext() {
        // Given
        String query = "test query";
        Document doc1 = new Document("Content 1");
        Document doc2 = new Document("Content 2");
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(doc1, doc2));

        // When
        String context = service.retrieveContext(query, 2);

        // Then
        assertThat(context).contains("Content 1");
        assertThat(context).contains("Content 2");
        assertThat(context).contains("---");
    }

    @Test
    void shouldReturnEmptyStringWhenVectorStoreIsNull() {
        // Given
        service = new OrasakaKnowledgeService(null);

        // When
        String context = service.retrieveContext("query", 5);

        // Then
        assertThat(context).isEmpty();
    }
}
