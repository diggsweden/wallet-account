// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.infrastructure.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.Test;

public class BytesMapperTest {

  @Test
  void assertThatBytesToString_nullBytes_shouldReturnNull() {

    assertNull(BytesMapper.bytesToString(null));
  }

  @Test
  void assertThatBytesToString_emptyBytes_shouldReturnEmptyText() {

    assertEquals("", BytesMapper.bytesToString(new byte[0]));
  }

  @Test
  void assertThatBytesToString_randomText_shouldReturnEqualMappedText() {

    var expectedOpaque = RandomGenerator.randomOpaque();
    byte[] bytes = expectedOpaque.getBytes(StandardCharsets.UTF_8);

    var actualOpaque = BytesMapper.bytesToString(bytes);

    assertEquals(expectedOpaque, actualOpaque);
  }

  @Test
  void assertThatStringToBytes_nullString_shouldReturnNull() {

    assertNull(BytesMapper.stringToBytes(null));
  }

  @Test
  void assertThatStringToBytes_emptyString_shouldReturnEmptyBytes() {

    assertThat(BytesMapper.stringToBytes("")).isEmpty();
  }

  @Test
  void assertThatStringToBytes_randomText_shouldRoundTrip() {

    var expectedContent = RandomGenerator.randomOpaque();
    var bytes = BytesMapper.stringToBytes(expectedContent);

    assertEquals(expectedContent, new String(bytes, StandardCharsets.UTF_8));
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
