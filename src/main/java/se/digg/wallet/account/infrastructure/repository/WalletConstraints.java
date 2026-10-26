// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.infrastructure.repository;

import java.util.Set;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Unique constraints on the wallets table (see V1__baseline.sql). The database is the real guard
 * against duplicates; these let the service tell which constraint a concurrent insert or update
 * violated.
 */
public final class WalletConstraints {

  public static final String DEVICE_KEY_KID = "wallets_device_key_kid_key";
  public static final String DEVICE_KEY_THUMBPRINT = "wallets_device_key_thumbprint_key";
  public static final String HSM_CLIENT_ID = "wallets_hsm_client_id_key";

  private static final Set<String> DEVICE_KEY = Set.of(DEVICE_KEY_KID, DEVICE_KEY_THUMBPRINT);

  public static boolean isDeviceKeyViolation(DataIntegrityViolationException e) {
    return DEVICE_KEY.contains(violatedConstraint(e));
  }

  public static boolean isHsmClientIdViolation(DataIntegrityViolationException e) {
    return HSM_CLIENT_ID.equals(violatedConstraint(e));
  }

  private static String violatedConstraint(DataIntegrityViolationException e) {
    for (Throwable cause = e; cause != null; cause = cause.getCause()) {
      if (cause instanceof ConstraintViolationException violation) {
        return violation.getConstraintName();
      }
    }
    return null;
  }

  private WalletConstraints() {
    // no instantiation
  }
}
