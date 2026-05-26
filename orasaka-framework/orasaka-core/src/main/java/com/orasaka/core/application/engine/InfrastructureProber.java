package com.orasaka.core.application.engine;

import com.orasaka.core.infrastructure.config.CoreProperties;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Asynchronous background infrastructure prober. Periodically pings critical local resources and
 * exposes a thread-safe, non-blocking cache of status indicators.
 */
@Component
public class InfrastructureProber {

  private static final Logger log = LoggerFactory.getLogger(InfrastructureProber.class);
  private final AtomicBoolean videoEngineOnline = new AtomicBoolean(false);
  private final AtomicBoolean imageEngineOnline = new AtomicBoolean(false);

  private final CoreProperties coreProperties;
  private final int videoProbePort;
  private final int imageProbePort;

  public InfrastructureProber(
      CoreProperties coreProperties,
      @Value("${orasaka.core.video.generation.probe-port:8188}") int videoProbePort,
      @Value("${orasaka.core.image.generation.probe-port:8085}") int imageProbePort) {
    this.coreProperties = coreProperties;
    this.videoProbePort = videoProbePort;
    this.imageProbePort = imageProbePort;
    log.info(
        "InfrastructureProber constructor called: registering background probes on video port {} and image port {}",
        videoProbePort,
        imageProbePort);
  }

  /**
   * Periodically pings the configured video port to check if the video inference engine is
   * responsive. Runs every 5 seconds dynamically.
   */
  @Scheduled(
      fixedDelayString = "${orasaka.core.video.probing.interval-ms:5000}",
      initialDelay = 1000)
  public void probeVideoInfrastructure() {
    String host = "localhost";
    try {
      String baseUrl = coreProperties.video().generation().baseUrl();
      if (baseUrl != null && !baseUrl.isBlank()) {
        URI uri = URI.create(baseUrl);
        String extractedHost = uri.getHost();
        if (extractedHost != null && !extractedHost.isBlank()) {
          host = extractedHost;
        }
      }
    } catch (Exception e) {
      log.warn("Failed to parse host from video generation base-url", e);
    }

    boolean online = checkPort(host, videoProbePort, 200);
    boolean previous = videoEngineOnline.getAndSet(online);
    log.debug(
        "Probed video infrastructure on host {} port {}. Online: {}", host, videoProbePort, online);
    if (previous != online) {
      log.info("Video infrastructure status changed. Online: {}", online);
    }
  }

  /**
   * Periodically pings the configured image port to check if the image inference engine (Stable
   * Diffusion) is responsive. Runs every 5 seconds dynamically.
   */
  @Scheduled(
      fixedDelayString = "${orasaka.core.image.probing.interval-ms:5000}",
      initialDelay = 1000)
  public void probeImageInfrastructure() {
    String host = "localhost";
    try {
      String baseUrl = coreProperties.image().generation().baseUrl();
      if (baseUrl != null && !baseUrl.isBlank()) {
        URI uri = URI.create(baseUrl);
        String extractedHost = uri.getHost();
        if (extractedHost != null && !extractedHost.isBlank()) {
          host = extractedHost;
        }
      }
    } catch (Exception e) {
      log.warn("Failed to parse host from image generation base-url", e);
    }

    boolean online = checkPort(host, imageProbePort, 200);
    boolean previous = imageEngineOnline.getAndSet(online);
    log.debug(
        "Probed image infrastructure on host {} port {}. Online: {}", host, imageProbePort, online);
    if (previous != online) {
      log.info("Image infrastructure status changed. Online: {}", online);
    }
  }

  /**
   * Non-blocking getter to check if the video engine is active.
   *
   * @return true if the video port was responsive during the last background probe.
   */
  public boolean isVideoEngineOnline() {
    return videoEngineOnline.get();
  }

  /**
   * Non-blocking getter to check if the image engine is active.
   *
   * @return true if the image port was responsive during the last background probe.
   */
  public boolean isImageEngineOnline() {
    return imageEngineOnline.get();
  }

  /** Getter for the configured video probe port. */
  public int getVideoProbePort() {
    return videoProbePort;
  }

  /** Getter for the configured image probe port. */
  public int getImageProbePort() {
    return imageProbePort;
  }

  private static boolean checkPort(String host, int port, int timeoutMs) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, port), timeoutMs);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
