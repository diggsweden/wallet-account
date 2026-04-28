// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.application.exception;

public class WalletAccountException extends RuntimeException {
  public WalletAccountException() {
    super();
  }

  public WalletAccountException(String message) {
    super(message);
  }
}
