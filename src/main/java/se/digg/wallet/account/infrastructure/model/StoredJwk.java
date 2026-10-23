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
 * A public JWK stored as json, with the kid and RFC 7638 thumbprint extracted into their own
 * columns for lookup and uniqueness.
 */
@Embeddable
public class StoredJwk {

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "jwk", columnDefinition = "jsonb")
  private String jwk;

  @Column(name = "kid")
  private String kid;

  @Column(name = "thumbprint")
  private String thumbprint;

  public StoredJwk() {}

  public StoredJwk(String jwk, String kid, String thumbprint) {
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

    StoredJwk other = (StoredJwk) obj;
    return Objects.equals(jwk, other.jwk)
        && Objects.equals(kid, other.kid)
        && Objects.equals(thumbprint, other.thumbprint);
  }

  @Override
  public String toString() {
    return "StoredJwk [kid=" + kid + ", thumbprint=" + thumbprint + ", jwk=" + jwk + "]";
  }
}
