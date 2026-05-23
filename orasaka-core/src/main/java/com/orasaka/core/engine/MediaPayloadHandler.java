package com.orasaka.core.engine;

import com.orasaka.core.support.OrasakaContext;
import com.orasaka.core.support.OrasakaImageRequest;
import com.orasaka.core.support.OrasakaSpeechRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.image.ImageMessage;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImagePrompt;

final class MediaPayloadHandler {

  private MediaPayloadHandler() {}

  static String resolveVoicePreference(OrasakaContext context) {
    return Optional.ofNullable(context)
        .map(OrasakaContext::preferences)
        .map(p -> p.get("tts-voice"))
        .map(Object::toString)
        .orElse("alloy");
  }

  static TextToSpeechPrompt toSpeechPrompt(OrasakaSpeechRequest request, String voice) {
    return new TextToSpeechPrompt(request.text());
  }

  static ImagePrompt toImagePrompt(OrasakaImageRequest request, String provider) {
    ImageOptions springOptions = OrasakaOptionsMapper.mapImageOptions(request, provider);
    return new ImagePrompt(List.of(new ImageMessage(request.prompt())), springOptions);
  }
}
