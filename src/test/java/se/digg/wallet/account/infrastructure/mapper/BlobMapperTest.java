// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.infrastructure.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import javax.sql.rowset.serial.SerialBlob;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.Test;

public class BlobMapperTest {

  @Test
  void assertThatBlobToString_nullBlob_shouldReturnNull() throws Exception {

    assertNull(BlobMapper.blobToString(null));
  }

  @Test
  void assertThatBlobToString_blobWithEmptyText_shouldReturnEqualMappedText() throws Exception {

    var expectedEmptyText = "";
    byte[] bytes = expectedEmptyText.getBytes(StandardCharsets.UTF_8);
    var blob = new SerialBlob(bytes);

    var actualEmptyText = BlobMapper.blobToString(blob);

    assertEquals(expectedEmptyText, actualEmptyText);
  }

  @Test
  void assertThatBlobToString_blobWithRandomText_shouldReturnEqualMappedText() throws Exception {

    var expectedOpaque = RandomGenerator.randomOpaque();
    byte[] bytes = expectedOpaque.getBytes(StandardCharsets.UTF_8);
    var blob = new SerialBlob(bytes);

    var actualOpaque = BlobMapper.blobToString(blob);

    assertEquals(expectedOpaque, actualOpaque);
  }

  @Test
  void assertThatStringToBlob_nullString_shouldReturnNull() throws Exception {

    assertNull(BlobMapper.stringToBlob(null));
  }

  @Test
  void assertThatStringToBlob_emptyString_shouldReturnEmptyBlob() throws Exception {

    var expectedContent = "";
    var blob = BlobMapper.stringToBlob(expectedContent);

    assertThat(blob.length()).isZero();
  }

  @Test
  void assertThatStringToBlob_randomText_shouldReturnEqualMappedBlob() throws Exception {

    var expectedContent = RandomGenerator.randomOpaque();
    var blob = BlobMapper.stringToBlob(expectedContent);

    byte[] bytes = blob.getBytes(1, (int) blob.length());
    var actualContent = new String(bytes, StandardCharsets.UTF_8);

    assertEquals(expectedContent, actualContent);
  }

  public static class RandomGenerator {

    public static String randomOpaque() {

      // 1. Instantiate SecureRandom (automatically seeds itself)
      SecureRandom secureRandom = new SecureRandom();

      // 2. Generate opaque bytes (e.g., 32 bytes = 256 bits)
      byte[] randomBytes = new byte[512];
      secureRandom.nextBytes(randomBytes);

      // 3. Output as Base64 for a "readable" opaque string
      return Base64.getEncoder().encodeToString(randomBytes);
    }
  }
}
