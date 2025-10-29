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
@Table(name = "publickeys")
public class PublicKeyEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column
  private String publicKey;
  @Column
  private String publicKeyIdentifier;

  @OneToOne(mappedBy = "publicKey")
  private AccountEntity accountEntity;

  public PublicKeyEntity() {}

  public PublicKeyEntity(String publicKey, String publicKeyIdentifier) {
    this.publicKey = publicKey;
    this.publicKeyIdentifier = publicKeyIdentifier;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getPublicKey() {
    return publicKey;
  }

  public void setPublicKey(String publicKey) {
    this.publicKey = publicKey;
  }

  public String getPublicKeyIdentifier() {
    return publicKeyIdentifier;
  }

  public void setPublicKeyIdentifier(String publicKeyIdentifier) {
    this.publicKeyIdentifier = publicKeyIdentifier;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, publicKey, publicKeyIdentifier);
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

    PublicKeyEntity other = (PublicKeyEntity) obj;
    return Objects.equals(id, other.id)
        && Objects.equals(publicKey, other.publicKey)
        && Objects.equals(publicKeyIdentifier, other.publicKeyIdentifier);
  }

  @Override
  public String toString() {
    return "PublicKeyEntity [id=" + id + ", publicKey=" + publicKey + ", publicKeyIdentifier="
        + publicKeyIdentifier + "]";
  }
}
