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
  @JoinColumn(name = "public_key_id", referencedColumnName = "id")
  private PublicKeyEntity publicKey;


  public AccountEntity() {}

  public AccountEntity(String personalIdentityNumber, String emailAdress,
      String telephoneNumber, PublicKeyEntity publicKey) {
    this.personalIdentityNumber = personalIdentityNumber;
    this.emailAdress = emailAdress;
    this.telephoneNumber = telephoneNumber;
    this.publicKey = publicKey;
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

  public PublicKeyEntity getPublicKey() {
    return publicKey;
  }

  public void setPublicKey(PublicKeyEntity publicKey) {
    this.publicKey = publicKey;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, emailAdress, telephoneNumber, publicKey);
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
        && Objects.equals(other.publicKey, publicKey);
  }

  @Override
  public String toString() {
    return "AccountEntity [id=" + id + ", personalIdentityNumber=" + personalIdentityNumber
        + ", emailAdress=" + emailAdress + ", telephoneNumber=" + telephoneNumber
        + ", publicKey=" + publicKey + "]";
  }

}
