
// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.infrastructure.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.digg.wallet.account.TestUtils;
import se.digg.wallet.account.infrastructure.SharedPostgresContainer;
import se.digg.wallet.account.infrastructure.mapper.BytesMapper;
import se.digg.wallet.account.infrastructure.model.AccountEntity;
import se.digg.wallet.account.infrastructure.model.DeviceKey;

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

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private final String SECURITY_ENVELOPE = "this is just a String";
  private final String KID = "019f6ff8-2cc5-7152-b579-4db56902bb1e";

  @Test
  void aSavedAccountCanBeRetrieved() throws SQLException {
    final byte[] securityEnvelope = BytesMapper.stringToBytes(SECURITY_ENVELOPE);

    AccountEntity entity =
        new AccountEntity(securityEnvelope,
            TestUtils.generateJwkEntity(UUID.randomUUID().toString()));

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
        BytesMapper.bytesToString(foundEntity.getSecurityEnvelope()).equals(SECURITY_ENVELOPE));
    assertThat(foundEntity.getDeviceKey())
        .isNotNull();
    assertThat(foundEntity.getDeviceKey().getKid()).isEqualTo(entity.getDeviceKey().getKid());
    assertThat(foundEntity.getDeviceKey().getThumbprint())
        .isEqualTo(entity.getDeviceKey().getThumbprint());
    assertThat(TestUtils.parseJwk(foundEntity.getDeviceKey().getJwk()))
        .isEqualTo(TestUtils.parseJwk(entity.getDeviceKey().getJwk()));
  }



  @ParameterizedTest
  @ValueSource(strings = KID)
  void kidUniqueConstraintRejectsDuplicateKid(String kid) throws SQLException {
    final byte[] securityEnvelope = BytesMapper.stringToBytes(SECURITY_ENVELOPE);

    // 1. Insert the first entity
    AccountEntity entity = new AccountEntity(securityEnvelope,
        TestUtils.generateJwkEntity(kid));

    accountRepository.save(entity);
    entityManager.flush();
    entityManager.clear();

    // 2. Try inserting a second entity with the exact same device key id
    AccountEntity entity2 = new AccountEntity(securityEnvelope,
        TestUtils.generateJwkEntity(kid));

    // 3. Assert that the database throws a ConstraintViolationException
    assertThrows(ConstraintViolationException.class, () -> {
      accountRepository.save(entity2);
      entityManager.flush();
      entityManager.clear();
    });
  }

  @Test
  void thumbprintUniqueConstraintRejectsSameKeyUnderNewKid() {
    var deviceKey = TestUtils.generateJwkEntity(UUID.randomUUID().toString());
    accountRepository.save(new AccountEntity(null, deviceKey));
    entityManager.flush();
    entityManager.clear();

    var sameKeyNewKid = new DeviceKey(deviceKey.getJwk(), UUID.randomUUID().toString(),
        deviceKey.getThumbprint());

    assertThrows(ConstraintViolationException.class, () -> {
      accountRepository.save(new AccountEntity(null, sameKeyNewKid));
      entityManager.flush();
      entityManager.clear();
    });
  }

  @ParameterizedTest
  @NullAndEmptySource
  void kidMinLengthConstraintRejectsNullOrZeroLength(String kid) throws SQLException {
    final byte[] securityEnvelope = BytesMapper.stringToBytes(SECURITY_ENVELOPE);

    AccountEntity entity = new AccountEntity(securityEnvelope,
        TestUtils.generateJwkEntity(kid));

    assertThrows(ConstraintViolationException.class, () -> {
      accountRepository.save(entity);
      entityManager.flush();
      entityManager.clear();
    });
  }

  @ParameterizedTest
  @ValueSource(strings = {"1", "123", KID})
  void kidMinLengthConstraintAcceptsLargerThanZeroLength(String kid) throws SQLException {
    final byte[] securityEnvelope = BytesMapper.stringToBytes(SECURITY_ENVELOPE);

    AccountEntity entity = new AccountEntity(securityEnvelope,
        TestUtils.generateJwkEntity(kid));

    // Assert that the database does not throw a ConstraintViolationException
    assertDoesNotThrow(() -> {
      accountRepository.save(entity);
      entityManager.flush();
      entityManager.clear();
    });
  }

  @Test
  void securityEnvelopeMaxSizeConstraintRejectsTooLargeEnvelope() {
    AccountEntity entity = new AccountEntity(new byte[262145],
        TestUtils.generateJwkEntity(UUID.randomUUID().toString()));

    assertThrows(ConstraintViolationException.class, () -> {
      accountRepository.save(entity);
      entityManager.flush();
      entityManager.clear();
    });
  }

  @Test
  void securityEnvelopeMaxSizeConstraintAcceptsMaxSizeEnvelope() {
    AccountEntity entity = new AccountEntity(new byte[262144],
        TestUtils.generateJwkEntity(UUID.randomUUID().toString()));

    assertDoesNotThrow(() -> {
      accountRepository.save(entity);
      entityManager.flush();
      entityManager.clear();
    });
  }

  @Test
  void databaseIndexForKidExists() throws Exception {
    try (Connection conn = DriverManager.getConnection(
        postgres.getJdbcUrl(),
        postgres.getUsername(),
        postgres.getPassword())) {

      DatabaseMetaData metaData = conn.getMetaData();

      try (ResultSet rs = metaData.getIndexInfo(null, null, "accounts", false, false)) {
        List<String> indexNames = new ArrayList<>();
        while (rs.next()) {
          String indexName = rs.getString("INDEX_NAME");
          if (indexName != null) {
            indexNames.add(indexName.toLowerCase());
          }
        }
        assertThat(indexNames)
            .contains("accounts_device_key_kid_key", "accounts_device_key_thumbprint_key");
      }
    }
  }

  @Test
  void databaseQueryOnKidUsesIndexScan() {
    UUID TEST_KEY_ID = UUID.randomUUID();
    String explainQuery =
        "EXPLAIN SELECT * FROM accounts WHERE device_key_kid = '" + TEST_KEY_ID + "'";

    List<String> planLines = jdbcTemplate.queryForList(explainQuery, String.class);
    String executionPlan = String.join("\n", planLines).toLowerCase();

    // Assert that the plan contains an Index Scan/Seek instead of a Seq Scan/Full Table Scan
    assertThat(executionPlan).contains("index scan", "accounts_device_key_kid_key");
  }
}
