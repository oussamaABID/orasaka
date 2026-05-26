package com.orasaka.gateway;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

@SpringBootTest
@org.junit.jupiter.api.Disabled("Requires local Ollama instance")
public class OllamaStreamTest {

  @Autowired private ChatModel chatModel;

  @Test
  public void testOllamaStreamingWithOptions() {
    System.out.println(">>> STARTING OLLAMA STREAM OPTIONS TEST <<<");

    List<Message> messages =
        List.of(
            new SystemMessage(
                "System constraints:\nUser Primary Industry: technology\nUser AI Behavior: professional\n"),
            new UserMessage("Hello! Tell me a quick fact about Orasaka Corporation."));

    OllamaChatOptions finalOptions =
        OllamaChatOptions.builder().model("llama3.2:3b").temperature(0.7).numCtx(8192).build();

    Prompt prompt = new Prompt(messages, finalOptions);
    try {
      Flux<org.springframework.ai.chat.model.ChatResponse> stream = chatModel.stream(prompt);
      List<org.springframework.ai.chat.model.ChatResponse> responses = stream.collectList().block();
      System.out.println("Responses received: " + (responses == null ? "null" : responses.size()));
      if (responses != null) {
        for (int i = 0; i < responses.size(); i++) {
          var resp = responses.get(i);
          System.out.println(
              "Chunk "
                  + i
                  + ": "
                  + (resp.getResult() != null && resp.getResult().getOutput() != null
                      ? resp.getResult().getOutput().getText()
                      : "null"));
        }
      }
    } catch (Exception e) {
      System.out.println("Error during streaming: " + e.getMessage());
      e.printStackTrace();
    }
    System.out.println(">>> END OLLAMA STREAM OPTIONS TEST <<<");
  }
}
