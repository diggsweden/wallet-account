// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.application.exception;

/**
 * Thrown when an HSM client id is already stored for another wallet.
 */
public class HsmClientIdAlreadyInUseException extends WalletAccountException {

  public HsmClientIdAlreadyInUseException(String message) {
    super(message);
  }
}
