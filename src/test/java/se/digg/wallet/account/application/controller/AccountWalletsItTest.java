// SPDX-FileCopyrightText: 2026 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.application.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.digg.wallet.account.api.v0.model.AccountRequest;
import se.digg.wallet.account.api.v0.model.AccountResponse;
import se.digg.wallet.account.api.v0.model.EcJwkRequest;
import se.digg.wallet.account.infrastructure.SharedPostgresContainer;
import se.digg.wallet.account.infrastructure.repository.WalletRepository;

/**
 * Wallets put under an account id assigned elsewhere, by the authorization server.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AccountWalletsItTest {

  private static final int CONCURRENT_PUTS = 8;

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = SharedPostgresContainer.getContainer();

  @Autowired
  WalletRepository walletRepository;

  @LocalServerPort
  private int port;

  private RestTestClient client;

  @BeforeEach
  void setUp(WebApplicationContext context) {
    client = RestTestClient.bindToApplicationContext(context).build();
  }

  @Test
  void a_wallet_put_under_an_unknown_account_creates_the_account() {
    var accountId = UUID.randomUUID();
    var deviceKey = newDeviceKey();

    var response = putWallet(accountId, deviceKey)
        .expectStatus().isCreated()
        .expectBody(AccountResponse.class)
        .returnResult()
        .getResponseBody();

    assertThat(response.getId()).isEqualTo(accountId);
    assertThat(response.getDeviceKey().getKid()).isEqualTo(deviceKey.getKid());
    client.get().uri("/v0/accounts/{id}", accountId).exchange().expectStatus().isOk();
  }

  @Test
  void putting_the_same_wallet_again_returns_it_without_a_second_wallet() {
    var accountId = UUID.randomUUID();
    var deviceKey = newDeviceKey();
    putWallet(accountId, deviceKey).expectStatus().isCreated();

    var response = putWallet(accountId, deviceKey)
        .expectStatus().isOk()
        .expectBody(AccountResponse.class)
        .returnResult()
        .getResponseBody();

    assertThat(response.getId()).isEqualTo(accountId);
    assertThat(walletRepository.findByAccountId(accountId)).hasSize(1);
  }

  @Test
  void a_device_key_of_another_account_is_a_conflict() {
    var deviceKey = newDeviceKey();
    putWallet(UUID.randomUUID(), deviceKey).expectStatus().isCreated();

    putWallet(UUID.randomUUID(), deviceKey).expectStatus().isEqualTo(409);
  }

  @Test
  void a_second_device_key_for_an_account_is_a_conflict() {
    var accountId = UUID.randomUUID();
    putWallet(accountId, newDeviceKey()).expectStatus().isCreated();

    putWallet(accountId, newDeviceKey()).expectStatus().isEqualTo(409);
    assertThat(walletRepository.findByAccountId(accountId)).hasSize(1);
  }

  @Test
  void concurrent_puts_of_different_device_keys_store_one_wallet_for_the_account() {
    var accountId = UUID.randomUUID();
    var deviceKeys = Stream.generate(AccountWalletsItTest::newDeviceKey)
        .limit(CONCURRENT_PUTS).toList();

    var statuses = putConcurrently(accountId, deviceKeys);

    assertThat(statuses).containsOnlyOnce(201).containsOnly(201, 409);
    assertThat(walletRepository.findByAccountId(accountId)).hasSize(1);
  }

  @Test
  void concurrent_puts_of_the_same_device_key_store_it_once_and_return_it_to_the_rest() {
    var accountId = UUID.randomUUID();
    var deviceKey = newDeviceKey();

    var statuses = putConcurrently(accountId, Collections.nCopies(CONCURRENT_PUTS, deviceKey));

    assertThat(statuses).containsOnlyOnce(201).containsOnly(201, 200);
    assertThat(walletRepository.findByAccountId(accountId)).hasSize(1);
  }

  @Test
  void an_invalid_device_key_is_a_bad_request() {
    var invalidKey = EcJwkRequest.builder().kid("1").kty("2").crv("3").x("4").y("5").build();

    putWallet(UUID.randomUUID(), invalidKey).expectStatus().isBadRequest();
  }

  private RestTestClient.ResponseSpec putWallet(UUID accountId, EcJwkRequest deviceKey) {
    return client.put()
        .uri("/v0/accounts/{id}/wallets", accountId)
        .body(AccountRequest.builder().deviceKey(deviceKey).build())
        .exchange();
  }

  /** Puts each device key in its own request, all released at once against the real server. */
  private List<Integer> putConcurrently(UUID accountId, List<EcJwkRequest> deviceKeys) {
    var server = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    var start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(deviceKeys.size())) {
      List<Future<Integer>> responses = deviceKeys.stream()
          .map(deviceKey -> executor.submit(() -> {
            start.await();
            return server.put()
                .uri("/v0/accounts/{id}/wallets", accountId)
                .body(AccountRequest.builder().deviceKey(deviceKey).build())
                .exchange()
                .returnResult()
                .getStatus()
                .value();
          }))
          .toList();
      start.countDown();
      return responses.stream().map(AccountWalletsItTest::await).toList();
    }
  }

  private static Integer await(Future<Integer> response) {
    try {
      return response.get(30, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static EcJwkRequest newDeviceKey() {
    try {
      ECKey key = new ECKeyGenerator(Curve.P_256).keyID(UUID.randomUUID().toString()).generate();
      return EcJwkRequest.builder()
          .kid(key.getKeyID())
          .kty("EC")
          .crv("P-256")
          .x(key.getX().toString())
          .y(key.getY().toString())
          .build();
    } catch (JOSEException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
