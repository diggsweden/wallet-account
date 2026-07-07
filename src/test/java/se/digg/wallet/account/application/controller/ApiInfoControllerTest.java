// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.application.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.digg.wallet.account.api.v0.model.ApiInfoResponse;
import se.digg.wallet.account.infrastructure.SharedPostgresContainer;

@Testcontainers
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.liquibase.enabled=false"
    })
public class ApiInfoControllerTest {

  @Container
  @ServiceConnection
  private static PostgreSQLContainer<?> postgres = SharedPostgresContainer.getContainer();

  private static RestTestClient client;

  @BeforeAll
  static void setUp(WebApplicationContext context) { // Inject the configuration
    client = RestTestClient.bindToApplicationContext(context).build();
  }

  @Test
  void apiInfoExposeInfoValues() {

    var apiInfoResponse = client.get()
        .uri("/api-info")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(ApiInfoResponse.class)
        .returnResult()
        .getResponseBody();

    assertThat(apiInfoResponse).isNotNull();
    assertThat(apiInfoResponse.getName()).isNotEmpty();
    assertThat(apiInfoResponse.getVersion()).isNotEmpty();
    assertThat(apiInfoResponse.getReleaseDate()).isNotEmpty();
    assertThat(apiInfoResponse.getReleaseDate().get()).isAfter(LocalDate.EPOCH);
    assertThat(apiInfoResponse.getStatus()).isNotEmpty();
  }
}
