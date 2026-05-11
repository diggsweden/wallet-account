
// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.infrastructure.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Blob;
import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.digg.wallet.account.TestUtils;
import se.digg.wallet.account.infrastructure.SharedPostgresContainer;
import se.digg.wallet.account.infrastructure.mapper.BlobMapper;
import se.digg.wallet.account.infrastructure.model.AccountEntity;

@DataJpaTest
@Testcontainers
class AccountRepositoryTest {

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = SharedPostgresContainer.getContainer();

  @Autowired
  AccountRepository accountRepository;

  @Autowired
  TestEntityManager entityManager;

  final String SECURITY_ENVELOPE = "this is just a String";
  final String PERSONAL_IDENTITY_NUMBER = "770101-1234";
  final String EMAIL = "none@business.se";
  final String PHONE = "070-123 123 123";

  @Test
  void testSaveAndRetrieveAccount() throws SQLException {
    final Blob securityEnvelopeBlob = BlobMapper.stringToBlob(SECURITY_ENVELOPE);

    AccountEntity entity =
        new AccountEntity(null,
            null,
            null,
            securityEnvelopeBlob,
            TestUtils.generateJwkEntity(null),
            TestUtils.generateJwkEntity(UUID.randomUUID().toString()));

    entity.setPersonalIdentityNumber(PERSONAL_IDENTITY_NUMBER);
    entity.setPhone(PHONE);
    entity.setEmail(EMAIL);

    AccountEntity storedEntity = accountRepository.save(entity);
    entityManager.flush();
    entityManager.clear();

    AccountEntity foundEntity = accountRepository.findById(storedEntity.getId()).orElseThrow();

    assertThat(foundEntity)
        .isNotNull();
    // .isEqualTo(storedEntity);
    assertThat(foundEntity.getSecurityEnvelope())
        .isNotNull();
    assertThat(
        BlobMapper.blobToString(foundEntity.getSecurityEnvelope()).equals(SECURITY_ENVELOPE));
    assertThat(foundEntity.getWalletKey())
        .isNotNull();
    assertThat(foundEntity.getWalletKey().getId()).isNotNull();
    assertThat(foundEntity.getDeviceKey())
        .isNotNull();
    assertThat(foundEntity.getDeviceKey().getId()).isNotNull();

    assertThat(foundEntity.getPersonalIdentityNumber()).isEqualTo(PERSONAL_IDENTITY_NUMBER);
    assertThat(foundEntity.getEmail()).isEqualTo(EMAIL);
    assertThat(foundEntity.getPhone()).isEqualTo(PHONE);
  }

  @Test
  void saveAndRetrieveAccountShouldHandleNullPersonalIdentityNumber() throws SQLException {
    final Blob securityEnvelopeBlob = BlobMapper.stringToBlob(SECURITY_ENVELOPE);

    AccountEntity entity =
        new AccountEntity(null,
            EMAIL,
            PHONE,
            securityEnvelopeBlob,
            TestUtils.generateJwkEntity(null),
            TestUtils.generateJwkEntity(UUID.randomUUID().toString()));

    AccountEntity storedEntity = accountRepository.save(entity);
    entityManager.flush();
    entityManager.clear();

    AccountEntity foundEntity = accountRepository.findById(storedEntity.getId()).orElseThrow();

    assertThat(foundEntity)
        .isNotNull();
    // .isEqualTo(storedEntity);

    assertThat(foundEntity.getPersonalIdentityNumber()).isNull();
  }
}
