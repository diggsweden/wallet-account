
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

  @Test
  void testSaveAndRetrieveAccount() throws SQLException {
    final String securityEnvelope = "this is just a String";
    final Blob securityEnvelopeBlob = BlobMapper.stringToBlob(securityEnvelope);


    AccountEntity entity =
        new AccountEntity("770101-1234",
            "none@business.se",
            "070-123 123 123",
            securityEnvelopeBlob,
            TestUtils.generateJwkEntity(null),
            TestUtils.generateJwkEntity(UUID.randomUUID().toString()));

    AccountEntity storedEntity = accountRepository.save(entity);
    entityManager.flush();
    entityManager.clear();

    AccountEntity foundEntity = accountRepository.findById(storedEntity.getId()).get();

    assertThat(foundEntity)
        .isNotNull();
    // .isEqualTo(storedEntity);
    assertThat(foundEntity.getSecurityEnvelope())
        .isNotNull();
    assertThat(
        BlobMapper.blobToString(foundEntity.getSecurityEnvelope()).equals(securityEnvelope));
    assertThat(foundEntity.getWalletKey())
        .isNotNull();
    assertThat(foundEntity.getWalletKey().getId()).isNotNull();
    assertThat(foundEntity.getDeviceKey())
        .isNotNull();
    assertThat(foundEntity.getDeviceKey().getId()).isNotNull();
  }
}
