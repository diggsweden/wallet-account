// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.application.controller;

import java.util.List;
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
import se.digg.wallet.account.application.model.PublicKeyDto;
import se.digg.wallet.account.domain.service.AccountService2;
import se.digg.wallet.account.domain.service.JwkValidationService;

@RestController
public class AccountV0Controller implements AccountApi {

  private final AccountService2 accountService;
  private final JwkValidationService jwkValidationService;

  public AccountV0Controller(AccountService2 accountService,
    JwkValidationService jwkValidationService) {
    this.accountService = accountService;
    this.jwkValidationService = jwkValidationService;
  }

  @Override
  public ResponseEntity<AccountResponse> createAccount(AccountRequest accountRequest) {

    var createAccountDto = toCreateAccountDto(accountRequest);
    if (!jwkValidationService.validateJwk(createAccountDto.publicKey())) {
      return ResponseEntity.badRequest().build();
    }

    var createdAccountDto = accountService.createAccount(createAccountDto);

    return ResponseEntity
      .status(HttpStatus.CREATED)
      .body(toAccountResponse(createdAccountDto));
  }

  @Override
  public ResponseEntity<AccountResponse> getAccount(UUID id) {

    var accountDto = accountService.getAccountById(id);

    return accountDto.map(AccountV0Controller::toAccountResponse)
      .map(ResponseEntity::ok)
      .orElse(ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity<KeyResponse> addAccountWalletKey(UUID id, KeyRequest keyRequest) {

    var publicKeyDto = toPublicKeyDto(keyRequest);
    var accountDto = accountService.createWalletKeys(id, List.of(publicKeyDto));
    var keyResponse = toKeyResponse(accountDto.publicKey());

    return ResponseEntity
      .status(HttpStatus.CREATED)
      .body(keyResponse);
  }

  @Override
  public ResponseEntity<KeysResponse> getAccountWalletKey(UUID id, Optional<String> kid) {

    var accountDto = accountService.getAccountById(id);
    if (accountDto.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    var publicKeyDto = accountService.getWalletKey(id);
    return publicKeyDto
      .filter(key -> kid.map(s -> s.equals(key.kid())).orElse(true))
      .map(AccountV0Controller::toKeyResponse)
      .map(key -> KeysResponse.builder().items(List.of(key)).build())
      .map(ResponseEntity::ok)
      .orElse(ResponseEntity.ok(KeysResponse.builder().build()));
  }

  @Override
  public ResponseEntity<SecurityEnvelopeResponse> addAccountSecurityEnvelope(UUID id,
    SecurityEnvelopeRequest securityEnvelopeRequest) {

    // TODO convert to dto with type enum
    var content = List.of(securityEnvelopeRequest.getContent());
    var accountDto = accountService.createSecurityEnvelopes(id, content);
    var securityEnvelopesResponse = toSecurityEnvelopeResponse(content.getFirst());

    return ResponseEntity.status(HttpStatus.CREATED).body(securityEnvelopesResponse);
  }

  @Override
  public ResponseEntity<SecurityEnvelopesResponse> getAccountSecurityEnvelope(UUID id,
    Optional<SecurityEnvelopeType> type) {

    var accountDto = accountService.getAccountById(id);
    if (accountDto.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    var securityEnvelopesDto = accountService.getSecurityEnvelopes(id);
    var filteredResponse = securityEnvelopesDto.stream()
      // TODO adapt filter to dto
      //.filter(key -> Optional.of(type).map(s -> s.equals(key)).orElse(true))
      .map(AccountV0Controller::toSecurityEnvelopeResponse)
      .toList();

    var securityEnvelopesResponse = SecurityEnvelopesResponse.builder()
      .items(filteredResponse)
      .build();
    return ResponseEntity.ok(securityEnvelopesResponse);
  }

  private static se.digg.wallet.account.application.model.CreateAccountRequestDto toCreateAccountDto(
    AccountRequest accountRequest) {

    var deviceKey = accountRequest.getDeviceKey();

    return new se.digg.wallet.account.application.model.CreateAccountRequestDto(
      accountRequest.getPersonalIdentityNumber(),
      accountRequest.getEmail(),
      accountRequest.getPhoneNumber(),
      Optional.of(deviceKey).map(dk -> new se.digg.wallet.account.application.model.PublicKeyDto(
          dk.getKty(),
          dk.getKid(),
          dk.getAlg().orElse(null),
          dk.getUse().orElse(null),
          dk.getCrv(),
          dk.getX(),
          dk.getY()))
        .orElseThrow());
  }

  private static AccountResponse toAccountResponse(se.digg.wallet.account.domain.model.AccountDto accountDto) {

    var publicKey = accountDto.publicKey();

    return AccountResponse.builder()
      .id(accountDto.id())
      .personalIdentityNumber(accountDto.personalIdentityNumber())
      .email(accountDto.emailAdress())
      .phoneNumber(accountDto.telephoneNumber().orElse(null))
      .deviceKey(se.digg.wallet.account.api.v0.model.KeyResponse.builder()
        .kty(publicKey.kty())
        .kid(publicKey.kid())
        .alg(publicKey.alg())
        .use(publicKey.use())
        .x(publicKey.x())
        .y(publicKey.y())
        .build())
      .build();
  }

  private static PublicKeyDto toPublicKeyDto(KeyRequest keyRequest) {
    return new se.digg.wallet.account.application.model.PublicKeyDto(
      keyRequest.getKty(),
      keyRequest.getKid(),
      keyRequest.getAlg().orElse(null),
      keyRequest.getUse().orElse(null),
      keyRequest.getCrv(),
      keyRequest.getX(),
      keyRequest.getY());
  }

  private static KeyResponse toKeyResponse(PublicKeyDto publicKeyDto) {
    return KeyResponse.builder()
      .kty(publicKeyDto.kty())
      .kid(publicKeyDto.kid())
      .alg(publicKeyDto.alg())
      .use(publicKeyDto.use())
      .crv(publicKeyDto.crv())
      .x(publicKeyDto.x())
      .y(publicKeyDto.y())
      .build();
  }

  // TODO adapt to dto
  private static String toSecurityEnvelopeDto(SecurityEnvelopeRequest securityEnvelopeRequest) {
    return securityEnvelopeRequest.getContent();
  }

  // TODO take dto as input parameter
  private static SecurityEnvelopeResponse toSecurityEnvelopeResponse(String content) {
    return SecurityEnvelopeResponse.builder()
      // TODO add type enum
      .content(content)
      .build();
  }
}
