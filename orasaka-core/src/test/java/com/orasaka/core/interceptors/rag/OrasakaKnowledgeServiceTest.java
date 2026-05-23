package com.orasaka.core.interceptors.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class OrasakaKnowledgeServiceTest {

  @Mock private VectorStore vectorStore;

  @Mock private ObjectProvider<VectorStore> vectorStoreProvider;

  private OrasakaKnowledgeService service;

  @BeforeEach
  void setUp() {
    Mockito.lenient().when(vectorStoreProvider.getIfAvailable()).thenReturn(vectorStore);
    service = new OrasakaKnowledgeService(vectorStoreProvider);
  }

  @Test
  void shouldRetrieveFormattedContext() {
    // Given
    String query = "test query";
    Document doc1 = new Document("Content 1");
    Document doc2 = new Document("Content 2");
    when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc1, doc2));

    // When
    String context = service.retrieveContext(query, 2);

    // Then
    assertThat(context).contains("Content 1");
    assertThat(context).contains("Content 2");
    assertThat(context).contains("---");
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldReturnEmptyStringWhenVectorStoreIsNull() {
    // Given
    ObjectProvider<VectorStore> emptyProvider = Mockito.mock(ObjectProvider.class);
    when(emptyProvider.getIfAvailable()).thenReturn(null);
    service = new OrasakaKnowledgeService(emptyProvider);

    // When
    String context = service.retrieveContext("query", 5);

    // Then
    assertThat(context).isEmpty();
  }
}
