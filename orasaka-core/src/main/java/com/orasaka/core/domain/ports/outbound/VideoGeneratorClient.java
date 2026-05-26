package com.orasaka.core.domain.ports.outbound;

import com.orasaka.core.domain.model.video.VideoRequest;
import com.orasaka.core.domain.model.video.VideoResponse;

/** Port interface for executing video generation inference against external model providers. */
public interface VideoGeneratorClient {

  /**
   * Generates a video based on the parameters in the request.
   *
   * @param request The video request parameters.
   * @return The generated video response.
   */
  VideoResponse generateVideo(VideoRequest request);
}
