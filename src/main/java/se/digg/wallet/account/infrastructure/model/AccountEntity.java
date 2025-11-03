// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.infrastructure.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class AccountEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @Column
  private String personalIdentityNumber;
  @Column
  private String emailAdress;
  @Column
  private String telephoneNumber;

  @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @JoinColumn(name = "jwk_key_id", referencedColumnName = "id")
  private JwkEntity jwk;


  public AccountEntity() {}

  public AccountEntity(String personalIdentityNumber, String emailAdress,
      String telephoneNumber, JwkEntity jwk) {
    this.personalIdentityNumber = personalIdentityNumber;
    this.emailAdress = emailAdress;
    this.telephoneNumber = telephoneNumber;
    this.jwk = jwk;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getPersonalIdentityNumber() {
    return personalIdentityNumber;
  }

  public void setPersonalIdentityNumber(String personalIdentityNumber) {
    this.personalIdentityNumber = personalIdentityNumber;
  }

  public String getEmailAdress() {
    return emailAdress;
  }

  public void setEmailAdress(String emailAdress) {
    this.emailAdress = emailAdress;
  }

  public String getTelephoneNumber() {
    return telephoneNumber;
  }

  public void setTelephoneNumber(String telephoneNumber) {
    this.telephoneNumber = telephoneNumber;
  }

  public JwkEntity getJwk() {
    return jwk;
  }

  public void setJwk(JwkEntity jwk) {
    this.jwk = jwk;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, emailAdress, telephoneNumber, jwk);
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

    AccountEntity other = (AccountEntity) obj;
    return Objects.equals(id, other.id)
        && Objects.equals(emailAdress, other.emailAdress)
        && Objects.equals(telephoneNumber, other.telephoneNumber)
        && Objects.equals(other.jwk, jwk);
  }

  @Override
  public String toString() {
    return "AccountEntity [id=" + id + ", personalIdentityNumber=" + personalIdentityNumber
        + ", emailAdress=" + emailAdress + ", telephoneNumber=" + telephoneNumber
        + ", publicKey=" + jwk + "]";
  }

}
