package com.orasaka.core.application.pipeline;

import com.orasaka.core.domain.model.chat.InternalChatRequest;
import com.orasaka.core.infrastructure.support.CoreException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeTypeUtils;

/**
 * Compiles Spring AI {@link Message} lists from {@link InternalChatRequest} history and the current
 * prompt text. Handles media attachment embedding for vision-capable models.
 *
 * <p>Extracted from {@link EnginePipelineBridge} to keep the bridge focused on orchestration.
 *
 */
final class MessageCompiler {

  private MessageCompiler() {}

  /**
   * Compiles the message history and current prompt into a list of Spring AI messages.
   *
   * @param request The incoming chat request.
   * @param promptText The resolved prompt text (after pipeline refinement).
   * @param mediaResult The extracted media result from the raw query.
   * @param hasMedia Whether the raw query contained embedded media.
   * @return Ordered list of Spring AI messages ready for prompt assembly.
   */
  static List<Message> compile(
      InternalChatRequest request,
      String promptText,
      Base64MediaExtractor.ExtractionResult mediaResult,
      boolean hasMedia) {

    List<Message> messages = new ArrayList<>();
    if (request.messages() != null) {
      for (InternalChatRequest.ChatMessage msg : request.messages()) {
        messages.add(mapMessage(msg));
      }
    }

    if (promptText == null || promptText.isBlank()) {
      return messages;
    }

    if (hasMedia) {
      appendMediaMessage(messages, promptText, mediaResult);
    } else {
      appendTextOrRefinedMediaMessage(messages, promptText);
    }
    return messages;
  }

  /**
   * Maps an Orasaka chat message to a Spring AI {@link Message} based on the role string.
   *
   * @param msg The Orasaka chat message containing role and content.
   * @return The corresponding Spring AI message ({@link SystemMessage}, {@link AssistantMessage},
   *     or {@link UserMessage}).
   */
  static Message mapMessage(InternalChatRequest.ChatMessage msg) {
    return switch (msg.role().toLowerCase()) {
      case "system" -> new SystemMessage(msg.content());
      case "assistant" -> new AssistantMessage(msg.content());
      default -> new UserMessage(msg.content());
    };
  }

  /** Appends a user message with an image media attachment. */
  private static void appendMediaMessage(
      List<Message> messages,
      String promptText,
      Base64MediaExtractor.ExtractionResult mediaResult) {
    byte[] imageBytes =
        mediaResult
            .imageBytes()
            .orElseThrow(() -> new CoreException("Expected media bytes but none found"));
    Media media = new Media(MimeTypeUtils.IMAGE_PNG, new ByteArrayResource(imageBytes));
    messages.add(UserMessage.builder().text(promptText).media(media).build());
  }

  /** Appends a text prompt or falls back to media extraction from refined text. */
  private static void appendTextOrRefinedMediaMessage(List<Message> messages, String promptText) {
    Base64MediaExtractor.ExtractionResult refinedResult = Base64MediaExtractor.extract(promptText);
    if (refinedResult.imageBytes().isPresent()) {
      byte[] imageBytes =
          refinedResult
              .imageBytes()
              .orElseThrow(() -> new CoreException("Expected refined media bytes but none found"));
      Media media = new Media(MimeTypeUtils.IMAGE_PNG, new ByteArrayResource(imageBytes));
      messages.add(UserMessage.builder().text(refinedResult.cleanedQuery()).media(media).build());
    } else {
      messages.add(new UserMessage(promptText));
    }
  }
}
