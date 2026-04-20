// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.application.controller;

import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import se.digg.wallet.account.api.v0.AccountApi;
import se.digg.wallet.account.api.v0.model.AccountRequest;
import se.digg.wallet.account.api.v0.model.AccountResponse;
import se.digg.wallet.account.api.v0.model.KeyRequest;
import se.digg.wallet.account.api.v0.model.KeyResponse;
import se.digg.wallet.account.api.v0.model.KeysResponse;
import se.digg.wallet.account.api.v0.model.SecurityEnvelopeRequest;
import se.digg.wallet.account.api.v0.model.SecurityEnvelopeResponse;
import se.digg.wallet.account.api.v0.model.SecurityEnvelopesResponse;
import se.digg.wallet.account.api.v0.model.SecurityEnvelopeType;

@RestController
public class AccountV0Controller implements AccountApi {

  @Override
  public ResponseEntity<AccountResponse> createAccount(AccountRequest accountRequest) {

    var accountResponse = AccountResponse.builder().build();
    return ResponseEntity.status(HttpStatus.CREATED).body(accountResponse);
  }

  @Override
  public ResponseEntity<AccountResponse> getAccount(UUID id) {

    var accountResponse = AccountResponse.builder().build();
    return ResponseEntity.ok(accountResponse);
  }

  @Override
  public ResponseEntity<KeyResponse> addAccountWalletKey(UUID id, KeyRequest keyRequest) {

    var keyResponse = KeyResponse.builder().build();
    return ResponseEntity.status(HttpStatus.CREATED).body(keyResponse);
  }

  @Override
  public ResponseEntity<KeysResponse> getAccountWalletKey(UUID id, Optional<String> kid) {

    var keysResponse = KeysResponse.builder().build();
    return ResponseEntity.ok(keysResponse);
  }

  @Override
  public ResponseEntity<SecurityEnvelopeResponse> addAccountSecurityEnvelope(UUID id,
    SecurityEnvelopeRequest securityEnvelopeRequest) {

    var securityEnvelopeResponse = SecurityEnvelopeResponse.builder().build();
    return ResponseEntity.status(HttpStatus.CREATED).body(securityEnvelopeResponse);
  }

  @Override
  public ResponseEntity<SecurityEnvelopesResponse> getAccountSecurityEnvelope(UUID id,
    Optional<SecurityEnvelopeType> type) {

    var securityEnvelopesResponse = SecurityEnvelopesResponse.builder().build();
    return ResponseEntity.ok(securityEnvelopesResponse);
  }
}
