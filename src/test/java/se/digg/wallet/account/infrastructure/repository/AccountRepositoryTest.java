
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

import com.nimbusds.jose.jwk.ECKey;
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
import se.digg.wallet.account.infrastructure.model.WalletEntity;

@DataJpaTest
@Testcontainers
class AccountRepositoryTest {

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = SharedPostgresContainer.getContainer();

  @Autowired
  AccountRepository accountRepository;

  @Autowired
  WalletRepository walletRepository;

  @Autowired
  TestEntityManager entityManager;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private final String SECURITY_ENVELOPE = "this is just a String";
  private final String KID = "019f6ff8-2cc5-7152-b579-4db56902bb1e";

  private WalletEntity newWallet(byte[] securityEnvelope, DeviceKey deviceKey) {
    return new WalletEntity(accountRepository.save(new AccountEntity()), deviceKey,
        securityEnvelope);
  }

  @Test
  void findByAccountIdReturnsOnlyThatAccountsWallets() {
    WalletEntity storedWallet = walletRepository.save(
        newWallet(null, TestUtils.generateJwkEntity(UUID.randomUUID().toString())));
    walletRepository.save(
        newWallet(null, TestUtils.generateJwkEntity(UUID.randomUUID().toString())));
    var accountId = storedWallet.getAccount().getId();
    entityManager.flush();
    entityManager.clear();

    var foundWallets = walletRepository.findByAccountId(accountId);

    assertThat(foundWallets).extracting(WalletEntity::getId).containsExactly(storedWallet.getId());
    assertThat(foundWallets.getFirst().getAccount().getId()).isEqualTo(accountId);
  }

  @Test
  void findByAccountIdReturnsNothingForUnknownAccount() {
    assertThat(walletRepository.findByAccountId(UUID.randomUUID())).isEmpty();
  }

  @Test
  void hsmClientIdUniqueConstraintRejectsSameClientIdOnTwoWallets() {
    var first = newWallet(null, TestUtils.generateJwkEntity(UUID.randomUUID().toString()));
    first.setHsmClientId("client-1");
    walletRepository.save(first);
    entityManager.flush();
    entityManager.clear();

    var second = newWallet(null, TestUtils.generateJwkEntity(UUID.randomUUID().toString()));
    second.setHsmClientId("client-1");

    assertThrows(ConstraintViolationException.class, () -> {
      walletRepository.save(second);
      entityManager.flush();
      entityManager.clear();
    });
  }

  @Test
  void hsmClientIdUniqueConstraintAcceptsSeveralWalletsWithoutClientId() {
    walletRepository.save(
        newWallet(null, TestUtils.generateJwkEntity(UUID.randomUUID().toString())));
    entityManager.flush();

    assertDoesNotThrow(() -> {
      walletRepository.save(
          newWallet(null, TestUtils.generateJwkEntity(UUID.randomUUID().toString())));
      entityManager.flush();
      entityManager.clear();
    });
  }

  @Test
  void deletingAnAccountDeletesItsWalletsInTheDatabase() {
    WalletEntity storedWallet = walletRepository.save(
        newWallet(null, TestUtils.generateJwkEntity(UUID.randomUUID().toString())));
    var walletId = storedWallet.getId();
    var accountId = storedWallet.getAccount().getId();
    entityManager.flush();
    entityManager.clear();

    // Only the account is deleted; ON DELETE CASCADE removes the wallet.
    accountRepository.deleteById(accountId);
    entityManager.flush();
    entityManager.clear();

    assertThat(walletRepository.findById(walletId)).isEmpty();
  }

  @Test
  void aSavedAccountCanBeRetrieved() throws SQLException {
    final byte[] securityEnvelope = BytesMapper.stringToBytes(SECURITY_ENVELOPE);

    WalletEntity entity =
        newWallet(securityEnvelope, TestUtils.generateJwkEntity(UUID.randomUUID().toString()));

    WalletEntity storedEntity = walletRepository.save(entity);
    entityManager.flush();
    entityManager.clear();

    WalletEntity foundEntity = walletRepository.findById(storedEntity.getId()).orElseThrow();

    assertThat(foundEntity)
        .isNotNull();
    // .isEqualTo(storedEntity);
    assertThat(foundEntity.getSecurityEnvelope())
        .isNotNull();
    assertThat(
        BytesMapper.bytesToString(foundEntity.getSecurityEnvelope())
            .equals(SECURITY_ENVELOPE));
    assertThat(foundEntity.getDeviceKey())
        .isNotNull();
    assertThat(foundEntity.getDeviceKey().getKid())
        .isEqualTo(entity.getDeviceKey().getKid());
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
    WalletEntity entity = newWallet(securityEnvelope, TestUtils.generateJwkEntity(kid));

    walletRepository.save(entity);
    entityManager.flush();
    entityManager.clear();

    // 2. Try inserting a second entity with the exact same device key id
    WalletEntity entity2 = newWallet(securityEnvelope, TestUtils.generateJwkEntity(kid));

    // 3. Assert that the database throws a ConstraintViolationException
    assertThrows(ConstraintViolationException.class, () -> {
      walletRepository.save(entity2);
      entityManager.flush();
      entityManager.clear();
    });
  }

  @Test
  void thumbprintUniqueConstraintRejectsSameKeyUnderNewKid() {
    var deviceKey = TestUtils.generateJwkEntity(UUID.randomUUID().toString());
    walletRepository.save(newWallet(null, deviceKey));
    entityManager.flush();
    entityManager.clear();

    // The thumbprint does not cover the kid, so the same key under a new kid keeps its thumbprint.
    var newKid = UUID.randomUUID().toString();
    var sameKeyNewKid = new DeviceKey(
        new ECKey.Builder(TestUtils.parseJwk(deviceKey.getJwk()).toECKey()).keyID(newKid).build()
            .toJSONString(),
        newKid, deviceKey.getThumbprint());

    var violation = assertThrows(ConstraintViolationException.class, () -> {
      walletRepository.save(newWallet(null, sameKeyNewKid));
      entityManager.flush();
      entityManager.clear();
    });
    assertThat(violation.getConstraintName()).isEqualTo("wallets_device_key_thumbprint_key");
  }

  @Test
  void kidColumnMustMatchTheKidInTheJwk() {
    var deviceKey = TestUtils.generateJwkEntity(UUID.randomUUID().toString());
    var mismatchingKid = new DeviceKey(deviceKey.getJwk(), UUID.randomUUID().toString(),
        deviceKey.getThumbprint());

    var violation = assertThrows(ConstraintViolationException.class, () -> {
      walletRepository.save(newWallet(null, mismatchingKid));
      entityManager.flush();
      entityManager.clear();
    });
    assertThat(violation.getConstraintName()).isEqualTo("chk_device_key_kid_matches_jwk");
  }

  @ParameterizedTest
  @NullAndEmptySource
  void kidMinLengthConstraintRejectsNullOrZeroLength(String kid) throws SQLException {
    final byte[] securityEnvelope = BytesMapper.stringToBytes(SECURITY_ENVELOPE);

    WalletEntity entity = newWallet(securityEnvelope, TestUtils.generateJwkEntity(kid));

    assertThrows(ConstraintViolationException.class, () -> {
      walletRepository.save(entity);
      entityManager.flush();
      entityManager.clear();
    });
  }

  @ParameterizedTest
  @ValueSource(strings = {"1", "123", KID})
  void kidMinLengthConstraintAcceptsLargerThanZeroLength(String kid) throws SQLException {
    final byte[] securityEnvelope = BytesMapper.stringToBytes(SECURITY_ENVELOPE);

    WalletEntity entity = newWallet(securityEnvelope, TestUtils.generateJwkEntity(kid));

    // Assert that the database does not throw a ConstraintViolationException
    assertDoesNotThrow(() -> {
      walletRepository.save(entity);
      entityManager.flush();
      entityManager.clear();
    });
  }

  @Test
  void securityEnvelopeMaxSizeConstraintRejectsTooLargeEnvelope() {
    WalletEntity entity =
        newWallet(new byte[262145], TestUtils.generateJwkEntity(UUID.randomUUID().toString()));

    assertThrows(ConstraintViolationException.class, () -> {
      walletRepository.save(entity);
      entityManager.flush();
      entityManager.clear();
    });
  }

  @Test
  void securityEnvelopeMaxSizeConstraintAcceptsMaxSizeEnvelope() {
    WalletEntity entity =
        newWallet(new byte[262144], TestUtils.generateJwkEntity(UUID.randomUUID().toString()));

    assertDoesNotThrow(() -> {
      walletRepository.save(entity);
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

      try (ResultSet rs = metaData.getIndexInfo(null, null, "wallets", false, false)) {
        List<String> indexNames = new ArrayList<>();
        while (rs.next()) {
          String indexName = rs.getString("INDEX_NAME");
          if (indexName != null) {
            indexNames.add(indexName.toLowerCase());
          }
        }
        assertThat(indexNames)
            .contains("wallets_device_key_kid_key", "wallets_device_key_thumbprint_key",
                "idx_wallets_account_id");
      }
    }
  }

  @Test
  void databaseQueryOnKidUsesIndexScan() {
    UUID TEST_KEY_ID = UUID.randomUUID();
    String explainQuery =
        "EXPLAIN SELECT * FROM wallets WHERE device_key_kid = '" + TEST_KEY_ID + "'";

    List<String> planLines = jdbcTemplate.queryForList(explainQuery, String.class);
    String executionPlan = String.join("\n", planLines).toLowerCase();

    // Assert that the plan contains an Index Scan/Seek instead of a Seq Scan/Full Table Scan
    assertThat(executionPlan).contains("index scan", "wallets_device_key_kid_key");
  }
}
