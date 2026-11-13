// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.infrastructure.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * A wallet instance bound to an account, identified by the key of the device it runs on.
 */
@Entity
@Table(name = "wallets")
public class WalletEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private AccountEntity account;

  @Embedded
  private DeviceKey deviceKey;

  @Column
  private byte[] securityEnvelope;

  @Column
  private String hsmClientId;

  public WalletEntity() {}

  public WalletEntity(AccountEntity account, DeviceKey deviceKey, byte[] securityEnvelope) {
    this.account = account;
    this.deviceKey = deviceKey;
    this.securityEnvelope = securityEnvelope;
  }

  public UUID getId() {
    return id;
  }

  public AccountEntity getAccount() {
    return account;
  }

  public DeviceKey getDeviceKey() {
    return deviceKey;
  }

  public byte[] getSecurityEnvelope() {
    return securityEnvelope;
  }

  public void setSecurityEnvelope(byte[] securityEnvelope) {
    this.securityEnvelope = securityEnvelope;
  }

  public String getHsmClientId() {
    return hsmClientId;
  }

  public void setHsmClientId(String hsmClientId) {
    this.hsmClientId = hsmClientId;
  }

  // The account is left out of equals, hashCode and toString so that comparing or logging a wallet
  // never loads the lazy account.

  @Override
  public int hashCode() {
    return Objects.hash(id, deviceKey, Arrays.hashCode(securityEnvelope), hsmClientId);
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

    WalletEntity other = (WalletEntity) obj;
    return Objects.equals(id, other.id)
        && Objects.equals(deviceKey, other.deviceKey)
        && Arrays.equals(securityEnvelope, other.securityEnvelope)
        && Objects.equals(hsmClientId, other.hsmClientId);
  }

  @Override
  public String toString() {
    return "WalletEntity [id=" + id
        + ", deviceKey=" + deviceKey
        + ", securityEnvelope="
        + (securityEnvelope == null ? null : securityEnvelope.length + " bytes")
        + ", hsmClientId=" + hsmClientId + "]";
  }
}
