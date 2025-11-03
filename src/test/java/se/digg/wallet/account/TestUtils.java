// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account;

import se.digg.wallet.account.application.model.PublicKeyDtoBuilder;
import se.digg.wallet.account.domain.model.AccountDtoBuilder;
import se.digg.wallet.account.infrastructure.model.PublicKeyEntity;

public class TestUtils {

  public static PublicKeyEntity generateJwkEntity(String kid) {
    return new PublicKeyEntity(
        "EC",
        kid,
        "alg",
        "enc",
        "P-256",
        "MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4",
        "4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM");
  }

  public static PublicKeyDtoBuilder publicKeyDtoBuilderWithDefaults(String kid) {
    return PublicKeyDtoBuilder.builder()
        .kty("EC")
        .crv("P-256")
        .x("MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4")
        .y("4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM")
        .alg("alg")
        .use("enc")
        .kid(kid);
  }

  public static AccountDtoBuilder accountDtoBuilderWithDefults() {
    return AccountDtoBuilder.builder()
        .emailAdress("dummy@dummy.se")
        .personalIdentityNumber("720202-0234")
        .publicKey(publicKeyDtoBuilderWithDefaults("99").build());
  }
}
