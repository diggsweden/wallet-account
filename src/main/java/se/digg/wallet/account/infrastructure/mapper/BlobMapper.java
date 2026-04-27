// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.infrastructure.mapper;

import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.SQLException;
import javax.sql.rowset.serial.SerialBlob;

public class BlobMapper {

  public static String blobToString(Blob blob) throws SQLException {
    if (blob == null) {
      return null;
    }
    long length = blob.length();
    if (length == 0) {
      return "";
    }
    if (length > Integer.MAX_VALUE) {
      throw new SQLException("Blob is too large for a single String conversion.");
    }
    byte[] bytes = blob.getBytes(1, (int) length);
    return new String(bytes, StandardCharsets.UTF_8);
  }

  public static Blob stringToBlob(String text) throws SQLException {
    if (text == null) {
      return null;
    }
    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
    return new SerialBlob(bytes);
  }
}
