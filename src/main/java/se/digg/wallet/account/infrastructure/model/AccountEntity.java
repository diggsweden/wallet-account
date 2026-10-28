// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.infrastructure.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class AccountEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @Column
  private String hsmClientId;

  @Column
  private byte[] securityEnvelope;

  @Embedded
  private DeviceKey deviceKey;


  public AccountEntity() {}

  public AccountEntity(byte[] securityEnvelope, DeviceKey deviceKey) {
    this.securityEnvelope = securityEnvelope;
    this.deviceKey = deviceKey;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getHsmClientId() {
    return hsmClientId;
  }

  public void setHsmClientId(String hsmClientId) {
    this.hsmClientId = hsmClientId;
  }

  public byte[] getSecurityEnvelope() {
    return securityEnvelope;
  }

  public void setSecurityEnvelope(byte[] securityEnvelope) {
    this.securityEnvelope = securityEnvelope;
  }

  public DeviceKey getDeviceKey() {
    return deviceKey;
  }

  public void setDeviceKey(DeviceKey deviceKey) {
    this.deviceKey = deviceKey;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, hsmClientId, Arrays.hashCode(securityEnvelope), deviceKey);
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
        && Objects.equals(hsmClientId, other.hsmClientId)
        && Arrays.equals(securityEnvelope, other.securityEnvelope)
        && Objects.equals(other.deviceKey, deviceKey);
  }

  @Override
  public String toString() {
    return "AccountEntity [id=" + id
        + ", hsmClientId=" + hsmClientId
        + ", securityEnvelope="
        + (securityEnvelope == null ? null : securityEnvelope.length + " bytes")
        + ", deviceKey=" + deviceKey + "]";
  }

}
