// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.infrastructure.mapper;

import javax.sql.rowset.serial.SerialBlob;
import java.sql.SQLException;

public class TestSerialBlob extends SerialBlob {
  public TestSerialBlob(byte[] b) throws SQLException {
    super(b);
  }

  @Override
  public long length() {
    // a value greater than Integer.MAX_VALUE
    return Long.MAX_VALUE;
  }
}
