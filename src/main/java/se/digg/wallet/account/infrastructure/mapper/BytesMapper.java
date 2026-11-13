// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.infrastructure.mapper;

import java.nio.charset.StandardCharsets;

public class BytesMapper {

  public static String bytesToString(byte[] bytes) {
    if (bytes == null) {
      return null;
    }
    return new String(bytes, StandardCharsets.UTF_8);
  }

  public static byte[] stringToBytes(String text) {
    if (text == null) {
      return null;
    }
    return text.getBytes(StandardCharsets.UTF_8);
  }

  private BytesMapper() {
    // no instantiation
  }
}
