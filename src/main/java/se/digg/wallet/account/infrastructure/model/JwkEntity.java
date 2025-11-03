// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.infrastructure.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "jwk")
public class JwkEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @Column
  private String kty;
  @Column
  private String kid;
  @Column
  private String alg;
  @Column
  private String use;
  @Column
  private String crv;
  @Column(name = "x")
  private String xvalue;
  @Column(name = "y")
  private String yvalue;

  @OneToOne(mappedBy = "jwk")
  private AccountEntity accountEntity;

  public JwkEntity() {}

  public JwkEntity(String kty, String kid, String alg, String use, String crv, String x,
      String y) {
    this.kty = kty;
    this.kid = kid;
    this.alg = alg;
    this.use = use;
    this.crv = crv;

    this.xvalue = x;
    this.yvalue = y;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getKty() {
    return kty;
  }

  public void setKty(String kty) {
    this.kty = kty;
  }

  public String getKid() {
    return kid;
  }

  public void setKid(String kid) {
    this.kid = kid;
  }

  public String getAlg() {
    return alg;
  }

  public void setAlg(String alg) {
    this.alg = alg;
  }

  public String getUse() {
    return use;
  }

  public void setUse(String use) {
    this.use = use;
  }

  public String getCrv() {
    return crv;
  }

  public void setCrv(String crv) {
    this.crv = crv;
  }

  public String getX() {
    return xvalue;
  }

  public void setX(String x) {
    this.xvalue = x;
  }

  public String getY() {
    return yvalue;
  }

  public void setY(String y) {
    this.yvalue = y;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, kty, kid, alg, use, crv, xvalue, yvalue);
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

    JwkEntity other = (JwkEntity) obj;
    return Objects.equals(id, other.id)
        && Objects.equals(kty, other.kty)
        && Objects.equals(kid, other.kid)
        && Objects.equals(alg, other.alg)
        && Objects.equals(use, other.use)
        && Objects.equals(crv, other.crv)
        && Objects.equals(getX(), other.getX())
        && Objects.equals(getY(), other.getY());
  }

  @Override
  public String toString() {
    return "JWKEntity [id=" + id + ", kty=" + kty + ", kid=" + kid + ", alg=" + alg + ", use="
        + use
        + ", crv=" + crv + ", x=" + xvalue + ", y=" + yvalue + "]";
  }



}
