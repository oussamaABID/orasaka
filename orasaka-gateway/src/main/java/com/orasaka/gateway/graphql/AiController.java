package com.orasaka.gateway.graphql;

import com.orasaka.core.client.OrasakaAiClient;
import com.orasaka.core.context.OrasakaContext;
import com.orasaka.core.model.OrasakaChatRequest;
import com.orasaka.core.model.OrasakaChatResponse;
import com.orasaka.identity.service.IdentityService;
import com.orasaka.identity.domain.User;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;

/**
 * GraphQL Controller for AI orchestration.
 * Orchestrates Core and Identity domains using Virtual Threads.
 */
@Controller
public class AiController {

    private final OrasakaAiClient aiClient;
    private final IdentityService identityService;
    private final ExecutorService virtualThreadExecutor;

    public AiController(OrasakaAiClient aiClient, IdentityService identityService) {
        this.aiClient = aiClient;
        this.identityService = identityService;
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @MutationMapping
    public CompletableFuture<OrasakaChatResponse> chat(@Argument String prompt, @Argument String conversationId) {
        return CompletableFuture.supplyAsync(() -> {
            // 1. Resolve User (Hardcoded UUID for demonstration)
            String userId = "550e8400-e29b-41d4-a716-446655440000";
            User user = identityService.getUser(userId);

            // 2. Build Context
            OrasakaContext context = new OrasakaContext(userId, conversationId, user.preferences());

            // 3. Dispatch to Core
            OrasakaChatRequest request = new OrasakaChatRequest(prompt, null, null, context);
            return aiClient.chat(request);
        }, virtualThreadExecutor);
    }

    @SubscriptionMapping
    public Flux<OrasakaChatResponse> chatStream(@Argument String prompt, @Argument String conversationId) {
        return Flux.create(sink -> {
            virtualThreadExecutor.submit(() -> {
                try {
                    String userId = "550e8400-e29b-41d4-a716-446655440000";
                    User user = identityService.getUser(userId);
                    OrasakaContext context = new OrasakaContext(userId, conversationId, user.preferences());
                    
                    OrasakaChatRequest request = new OrasakaChatRequest(prompt, null, null, context);
                    OrasakaChatResponse fullResponse = aiClient.chat(request);
                    
                    // Simulate word-by-word streaming for CLI/UI consumption
                    String[] words = fullResponse.content().split(" ");
                    for (String word : words) {
                        sink.next(new OrasakaChatResponse(word + " ", conversationId, null));
                        Thread.sleep(50); // Simulate network latency/token generation
                    }
                    sink.complete();
                } catch (Exception e) {
                    sink.error(e);
                }
            });
        });
    }
}
