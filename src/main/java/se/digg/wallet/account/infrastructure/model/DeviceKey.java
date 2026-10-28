// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.infrastructure.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * The public key of the device a wallet runs on.
 *
 * <p>
 * Stored as three columns in {@code accounts}:
 * <ul>
 * <li>{@code device_key}: the whole public JWK as json. This is the key itself.</li>
 * <li>{@code device_key_kid}: the JWK's {@code kid}, copied out so it can be looked up and kept
 * unique.</li>
 * <li>{@code device_key_thumbprint}: the RFC 7638 SHA-256 thumbprint of the key, unique so the same
 * key can't be registered twice under different kids. Same value as DPoP {@code cnf.jkt}.</li>
 * </ul>
 *
 * <p>
 * The kid and thumbprint are derived from the JWK and must always match it. Create instances with
 * {@link se.digg.wallet.account.infrastructure.mapper.JwkMapper#toDeviceKey}, which computes both.
 */
@Embeddable
public class DeviceKey {

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "device_key", columnDefinition = "jsonb")
  private String jwk;

  @Column(name = "device_key_kid")
  private String kid;

  @Column(name = "device_key_thumbprint")
  private String thumbprint;

  public DeviceKey() {}

  public DeviceKey(String jwk, String kid, String thumbprint) {
    this.jwk = jwk;
    this.kid = kid;
    this.thumbprint = thumbprint;
  }

  public String getJwk() {
    return jwk;
  }

  public String getKid() {
    return kid;
  }

  public String getThumbprint() {
    return thumbprint;
  }

  @Override
  public int hashCode() {
    return Objects.hash(jwk, kid, thumbprint);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }

    DeviceKey other = (DeviceKey) obj;
    return Objects.equals(jwk, other.jwk)
        && Objects.equals(kid, other.kid)
        && Objects.equals(thumbprint, other.thumbprint);
  }

  @Override
  public String toString() {
    return "DeviceKey [kid=" + kid + ", thumbprint=" + thumbprint + ", jwk=" + jwk + "]";
  }
}
