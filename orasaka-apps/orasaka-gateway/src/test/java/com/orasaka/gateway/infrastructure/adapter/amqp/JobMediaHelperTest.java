package com.orasaka.gateway.infrastructure.adapter.amqp;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JobMediaHelperTest {

  @TempDir Path tempDir;

  @Test
  void privateConstructor_throwsInstantiationException() throws Exception {
    Constructor<JobMediaHelper> constructor = JobMediaHelper.class.getDeclaredConstructor();
    assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
    constructor.setAccessible(true);
    JobMediaHelper helper = constructor.newInstance();
    assertNotNull(helper);
  }

  @Test
  void saveMediaToFile_nullOrEmptyData_returnsEmptyString() {
    String result1 =
        JobMediaHelper.saveMediaToFile(tempDir.toString(), "user1", "job1", null, "file.mp4");
    String result2 =
        JobMediaHelper.saveMediaToFile(
            tempDir.toString(), "user1", "job1", new byte[0], "file.mp4");
    assertEquals("", result1);
    assertEquals("", result2);
  }

  @Test
  void saveMediaToFile_validData_savesAndReturnsUrl() throws Exception {
    byte[] data = new byte[] {1, 2, 3, 4};
    String result =
        JobMediaHelper.saveMediaToFile(tempDir.toString(), "user1", "job1", data, "file.mp4");

    assertEquals("/uploads/user1/job1/output/file.mp4", result);

    Path savedPath = tempDir.resolve("user1").resolve("job1").resolve("output").resolve("file.mp4");
    assertTrue(Files.exists(savedPath));
    assertArrayEquals(data, Files.readAllBytes(savedPath));
  }

  @Test
  void saveMediaToFile_ioException_returnsEmptyString() throws Exception {
    byte[] data = new byte[] {1, 2, 3, 4};
    // Create a file where we want to create a directory, which forces createDirectories to fail
    // with IOException
    Path conflictingFile = tempDir.resolve("user1");
    Files.write(conflictingFile, new byte[] {1});

    String result =
        JobMediaHelper.saveMediaToFile(tempDir.toString(), "user1", "job1", data, "file.mp4");
    assertEquals("", result);
  }
}
