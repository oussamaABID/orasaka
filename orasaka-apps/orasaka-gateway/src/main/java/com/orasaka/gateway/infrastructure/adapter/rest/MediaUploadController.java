package com.orasaka.gateway.infrastructure.adapter.rest;

import com.orasaka.gateway.domain.model.UploadAssetResponse;
import com.orasaka.gateway.infrastructure.support.PathResolver;
import com.orasaka.identity.domain.model.User;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for secure stateless media uploads.
 *
 * <p>Handles multipart file uploads, isolates assets by authenticated user IDs, and returns
 * standard asset reference mappings.
 */
@RestController
@RequestMapping("/api/v1/media")
public class MediaUploadController {

  private static final Logger logger = LoggerFactory.getLogger(MediaUploadController.class);

  private final String uploadDirProperty;

  public MediaUploadController(
      @Value("${spring.servlet.multipart.location:var/orasaka-uploads}") String uploadDirProperty) {
    Objects.requireNonNull(uploadDirProperty, "upload-dir must not be null");

    // 🛡️ ANCRAGE ABSOLU : On force le dossier à s'aligner sur la racine de ton
    // monorepo
    this.uploadDirProperty = PathResolver.resolveToString(uploadDirProperty);
  }

  /**
   * Securely uploads a raw binary asset.
   *
   * @param file The multipart file payload.
   * @param user The authenticated user principal.
   * @return The UploadAssetResponse metadata mapping.
   */
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Object> upload(
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "jobId", required = false) String jobId,
      @AuthenticationPrincipal User user) {

    if (file == null || file.isEmpty()) {
      return ResponseEntity.badRequest().body("File payload must not be empty");
    }

    if (user == null || user.id() == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized user identity");
    }

    try {
      // 🛡️ Break taint chain: roundtrip through UUID.fromString validates format (Sonar S2083)
      String userId = UUID.fromString(user.id().toString()).toString();
      UUID assetId = UUID.randomUUID();

      // 🛡️ Sanitize user-controlled jobId against path traversal (Sonar S2083)
      String sanitizedJobId = sanitizeJobId(jobId);
      if (jobId != null && !jobId.isBlank() && sanitizedJobId == null) {
        return ResponseEntity.badRequest().body("Invalid jobId format");
      }

      // Resolve the parent isolated directory based on the hierarchy
      Path basePath = Paths.get(uploadDirProperty).toRealPath();
      Path userDirPath;
      if (sanitizedJobId != null) {
        userDirPath = basePath.resolve(userId).resolve(sanitizedJobId).resolve("input");
      } else {
        userDirPath = basePath.resolve(userId).resolve("temp");
      }

      // 🛡️ Validate resolved path stays within the upload directory (Sonar S2083)
      if (!userDirPath.normalize().startsWith(basePath)) {
        return ResponseEntity.badRequest().body("Path traversal detected");
      }

      if (sanitizedJobId != null) {
        Path jobOutputDir = basePath.resolve(userId).resolve(sanitizedJobId).resolve("output");
        Path jobTempDir = basePath.resolve(userId).resolve(sanitizedJobId).resolve("temp");
        Files.createDirectories(userDirPath);
        Files.createDirectories(jobOutputDir);
        Files.createDirectories(jobTempDir);
      } else {
        Path userTempDir = basePath.resolve(userId).resolve("temp");
        Files.createDirectories(userDirPath);
        Files.createDirectories(userTempDir);
      }

      // 🛡️ Sanitize original filename — extract only the extension (Sonar S2083)
      String originalFilename = file.getOriginalFilename();
      String suffix = extractSafeSuffix(originalFilename);

      // Safe isolated filename format: UUID + sanitized extension
      String safeName = assetId.toString() + suffix;
      Path targetPath = userDirPath.resolve(safeName).normalize();

      // 🛡️ Final containment check (Sonar S2083)
      if (!targetPath.startsWith(userDirPath)) {
        return ResponseEntity.badRequest().body("Invalid file path");
      }

      File targetFile = targetPath.toFile();
      file.transferTo(targetFile);

      // 🛡️ Log only non-user-controlled identifiers (Sonar S5145)
      logger.info("Successfully stored uploaded asset with id: {}", assetId);

      UploadAssetResponse responsePayload =
          new UploadAssetResponse(
              assetId,
              originalFilename != null ? originalFilename : safeName,
              file.getContentType(),
              file.getSize());

      return ResponseEntity.status(HttpStatus.CREATED).body(responsePayload);
    } catch (IOException e) {
      logger.error("Failed to write uploaded file to persistent storage", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Failed to store asset payload");
    }
  }

  private static String sanitizeJobId(String jobId) {
    if (jobId == null || jobId.isBlank()) return null;
    String sanitized = jobId.replaceAll("[^a-zA-Z0-9_-]", "");
    return sanitized.isEmpty() ? null : sanitized;
  }

  private static String extractSafeSuffix(String filename) {
    if (filename == null || !filename.contains(".")) return "";
    String ext = filename.substring(filename.lastIndexOf("."));
    return ext.replaceAll("[^a-zA-Z0-9.]", "");
  }
}
