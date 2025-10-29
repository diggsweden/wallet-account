// SPDX-FileCopyrightText: 2025 diggsweden/wallet-attribute-attestation
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.infrastructure;

import org.testcontainers.containers.PostgreSQLContainer;

public class SharedPostgresContainer {

  private static final PostgreSQLContainer<?> POSTGRES_CONTAINER;

  static {
    POSTGRES_CONTAINER = new PostgreSQLContainer<>("postgres:14-alpine");
    POSTGRES_CONTAINER.start();
  }

  public static PostgreSQLContainer<?> getContainer() {
    return POSTGRES_CONTAINER;
  }

  private SharedPostgresContainer() {
    // no instantiation
  }
}
