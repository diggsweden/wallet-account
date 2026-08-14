// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.application.exception;

public class NoIntegrityTokenException extends WalletAccountException {

  public NoIntegrityTokenException(String message) {
    super(message);
  }
}
