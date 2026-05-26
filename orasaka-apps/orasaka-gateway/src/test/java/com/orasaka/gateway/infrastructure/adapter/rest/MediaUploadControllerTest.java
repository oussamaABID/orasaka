package com.orasaka.gateway.infrastructure.adapter.rest;

import static org.junit.jupiter.api.Assertions.*;

import com.orasaka.gateway.domain.model.UploadAssetResponse;
import com.orasaka.identity.domain.model.User;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class MediaUploadControllerTest {

  @TempDir Path tempDir;

  private MediaUploadController controller;

  @BeforeEach
  void setUp() {
    controller = new MediaUploadController(tempDir.toString());
  }

  @Test
  void upload_emptyFile_returnsBadRequest() {
    User user =
        new User(
            UUID.randomUUID(),
            "testuser",
            "test@example.com",
            true,
            Set.of("ROLE_USER"),
            Map.of(),
            List.of());
    MultipartFile file = new MockMultipartFile("file", "", "text/plain", new byte[0]);

    ResponseEntity<Object> response = controller.upload(file, null, user);

    assertNotNull(response);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("File payload must not be empty", response.getBody());
  }

  @Test
  void upload_nullUser_returnsUnauthorized() {
    MultipartFile file =
        new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());

    ResponseEntity<Object> response = controller.upload(file, null, null);

    assertNotNull(response);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertEquals("Unauthorized user identity", response.getBody());
  }

  @Test
  void upload_validFileWithoutJobId_savesToTempAndReturnsCreated() throws Exception {
    UUID userId = UUID.randomUUID();
    User user =
        new User(
            userId, "testuser", "test@example.com", true, Set.of("ROLE_USER"), Map.of(), List.of());
    MultipartFile file =
        new MockMultipartFile("file", "photo.png", "image/png", "fake-png-data".getBytes());

    // Create the upload directory hierarchy to avoid toRealPath() throwing NoSuchFileException
    Files.createDirectories(tempDir.toRealPath());

    ResponseEntity<Object> response = controller.upload(file, null, user);

    assertNotNull(response);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertTrue(response.getBody() instanceof UploadAssetResponse);

    UploadAssetResponse body = (UploadAssetResponse) response.getBody();
    assertNotNull(body.assetId());
    assertEquals("photo.png", body.filename());
    assertEquals("image/png", body.contentType());
    assertEquals(13L, body.sizeBytes());

    // Verify file actually exists on disk in the temp folder
    Path userTempDir = tempDir.resolve(userId.toString()).resolve("temp");
    assertTrue(Files.exists(userTempDir));

    // Find the saved file in userTempDir
    try (var filesStream = Files.list(userTempDir)) {
      long filesCount = filesStream.count();
      assertEquals(1, filesCount);
    }
  }

  @Test
  void upload_validFileWithJobId_savesToJobInputAndReturnsCreated() throws Exception {
    UUID userId = UUID.randomUUID();
    User user =
        new User(
            userId, "testuser", "test@example.com", true, Set.of("ROLE_USER"), Map.of(), List.of());
    MultipartFile file =
        new MockMultipartFile("file", "video.mp4", "video/mp4", "fake-mp4-data".getBytes());

    Files.createDirectories(tempDir.toRealPath());

    ResponseEntity<Object> response = controller.upload(file, "job-999", user);

    assertNotNull(response);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertTrue(response.getBody() instanceof UploadAssetResponse);

    Path jobInputDir = tempDir.resolve(userId.toString()).resolve("job-999").resolve("input");
    Path jobOutputDir = tempDir.resolve(userId.toString()).resolve("job-999").resolve("output");
    Path jobTempDir = tempDir.resolve(userId.toString()).resolve("job-999").resolve("temp");

    assertTrue(Files.exists(jobInputDir));
    assertTrue(Files.exists(jobOutputDir));
    assertTrue(Files.exists(jobTempDir));

    try (var filesStream = Files.list(jobInputDir)) {
      assertEquals(1, filesStream.count());
    }
  }
}
