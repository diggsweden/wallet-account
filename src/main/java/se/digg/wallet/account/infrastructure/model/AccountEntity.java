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
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

  // security envelope (implementeras med opaque)
  @JdbcTypeCode(SqlTypes.BLOB)
  @Column(columnDefinition = "blob")
  private String securityEnvelope;

  // HSM:ens nyckel (walletKey) , den publika plånboksnyckeln
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> walletKey;

  // Device:ns nyckel, enhetens publika nyckeln, deviceKey
  @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @JoinColumn(name = "device_key_id", referencedColumnName = "id")
  private PublicKeyEntity deviceKey;


  public AccountEntity() {}

  public AccountEntity(String personalIdentityNumber, String emailAdress,
      String telephoneNumber, String securityEnvelope, Map<String, Object> walletKey,
      PublicKeyEntity deviceKey) {
    this.personalIdentityNumber = personalIdentityNumber;
    this.emailAdress = emailAdress;
    this.telephoneNumber = telephoneNumber;
    this.securityEnvelope = securityEnvelope;
    this.walletKey = walletKey;
    this.deviceKey = deviceKey;
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

  public String getSecurityEnvelope() {
    return securityEnvelope;
  }

  public void setSecurityEnvelope(String securityEnvelope) {
    this.securityEnvelope = securityEnvelope;
  }

  public Map<String, Object> getWalletKey() {
    return walletKey;
  }

  public void setWalletKey(Map<String, Object> walletKey) {
    this.walletKey = walletKey;
  }

  public PublicKeyEntity getDeviceKey() {
    return deviceKey;
  }

  public void setDeviceKey(PublicKeyEntity deviceKey) {
    this.deviceKey = deviceKey;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, emailAdress, telephoneNumber, securityEnvelope, walletKey, deviceKey);
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
        && Objects.equals(securityEnvelope, other.securityEnvelope)
        && Objects.equals(walletKey, other.walletKey)
        && Objects.equals(other.deviceKey, deviceKey);
  }

  @Override
  public String toString() {
    return "AccountEntity [id=" + id + ", personalIdentityNumber=" + personalIdentityNumber
        + ", emailAdress=" + emailAdress + ", telephoneNumber=" + telephoneNumber
        + ", securityEnvelope=" + securityEnvelope + ", walletKey=" + walletKey
        + ", deviceKey=" + deviceKey + "]";
  }

}
