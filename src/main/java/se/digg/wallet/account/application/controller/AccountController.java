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
import se.digg.wallet.account.api.v0.model.EcJwkItemsResponse;
import se.digg.wallet.account.api.v0.model.EcJwkRequest;
import se.digg.wallet.account.api.v0.model.EcJwkResponse;
import se.digg.wallet.account.api.v0.model.HsmClientIdRequest;
import se.digg.wallet.account.api.v0.model.HsmClientIdResponse;
import se.digg.wallet.account.api.v0.model.SecurityEnvelopeRequest;
import se.digg.wallet.account.api.v0.model.SecurityEnvelopeResponse;
import se.digg.wallet.account.api.v0.model.SecurityEnvelopesResponse;
import se.digg.wallet.account.application.model.CreateAccountRequestDto;
import se.digg.wallet.account.application.model.PublicKeyDto;
import se.digg.wallet.account.domain.service.AccountService;
import se.digg.wallet.account.domain.service.JwkValidationService;

@RestController
public class AccountController implements AccountApi {

  private final AccountService accountService;
  private final JwkValidationService jwkValidationService;

  public AccountController(AccountService accountService,
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

    return accountDto.map(AccountController::toAccountResponse)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity<EcJwkResponse> addAccountWalletKey(UUID id, EcJwkRequest keyRequest) {

    var accountDto = accountService.getAccountById(id);
    if (accountDto.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    var publicKeyDto = toPublicKeyDto(keyRequest);
    if (!jwkValidationService.validateJwk(publicKeyDto)) {
      return ResponseEntity.badRequest().build();
    }

    var createdWalletKey = accountService.createWalletKey(id, publicKeyDto);
    var keyResponse = toKeyResponse(createdWalletKey);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(keyResponse);
  }

  @Override
  public ResponseEntity<EcJwkItemsResponse> getAccountWalletKey(UUID id, Optional<String> kid) {

    var accountDto = accountService.getAccountById(id);
    if (accountDto.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    var publicKeyDto = accountService.getWalletKey(id);
    return publicKeyDto
        .filter(key -> kid.map(s -> s.equals(key.kid())).orElse(true))
        .map(AccountController::toKeyResponse)
        .map(key -> EcJwkItemsResponse.builder().items(List.of(key)).build())
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.ok(EcJwkItemsResponse.builder().build()));
  }

  @Override
  public ResponseEntity<SecurityEnvelopeResponse> addAccountSecurityEnvelope(UUID id,
      SecurityEnvelopeRequest securityEnvelopeRequest) {

    var accountDto = accountService.getAccountById(id);
    if (accountDto.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    var content = securityEnvelopeRequest.getContent();
    var savedSecurityEnvelope = accountService.createSecurityEnvelope(id, content);
    var securityEnvelopesResponse = toSecurityEnvelopeResponse(savedSecurityEnvelope);

    return ResponseEntity.status(HttpStatus.CREATED).body(securityEnvelopesResponse);
  }

  @Override
  public ResponseEntity<SecurityEnvelopesResponse> getAccountSecurityEnvelopes(UUID id) {

    var accountDto = accountService.getAccountById(id);
    if (accountDto.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    Optional<String> securityEnvelope = accountService.getSecurityEnvelope(id);
    if (securityEnvelope.isEmpty()) {
      return ResponseEntity.ok(SecurityEnvelopesResponse.builder()
          .items(List.of())
          .build());
    }

    var theOnlyResponse = toSecurityEnvelopeResponse(securityEnvelope.get());
    var securityEnvelopesResponse = SecurityEnvelopesResponse.builder()
        .items(List.of(theOnlyResponse))
        .build();
    return ResponseEntity.ok(securityEnvelopesResponse);
  }

  @Override
  public ResponseEntity<HsmClientIdResponse> addAccountHsmClientId(UUID id,
      HsmClientIdRequest hsmClientIdRequest) {

    var accountDto = accountService.getAccountById(id);
    if (accountDto.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    var savedHsmClientId = accountService.createHsmClientId(id, hsmClientIdRequest.getClientId());

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(toHsmClientIdResponse(savedHsmClientId));
  }

  @Override
  public ResponseEntity<HsmClientIdResponse> getAccountHsmClientId(UUID id) {

    var accountDto = accountService.getAccountById(id);
    if (accountDto.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    return accountService.getHsmClientId(id)
        .map(AccountController::toHsmClientIdResponse)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  private static CreateAccountRequestDto toCreateAccountDto(
      AccountRequest accountRequest) {

    var deviceKey = accountRequest.getDeviceKey();

    return new CreateAccountRequestDto(
        accountRequest.getPersonalIdentityNumber(),
        accountRequest.getEmail(),
        accountRequest.getPhoneNumber(),
        Optional.of(deviceKey).map(dk -> new PublicKeyDto(
            dk.getKty(),
            dk.getKid(),
            dk.getAlg().orElse(null),
            dk.getUse().orElse(null),
            dk.getCrv(),
            dk.getX(),
            dk.getY()))
            .orElseThrow());
  }

  private static AccountResponse toAccountResponse(
      se.digg.wallet.account.domain.model.AccountDto accountDto) {

    var publicKey = accountDto.publicKey();

    return AccountResponse.builder()
        .id(accountDto.id())
        .personalIdentityNumber(accountDto.personalIdentityNumber().orElse(null))
        .email(accountDto.emailAdress().orElse(null))
        .phoneNumber(accountDto.telephoneNumber().orElse(null))
        .deviceKey(toKeyResponse(publicKey))
        .build();
  }

  private static PublicKeyDto toPublicKeyDto(EcJwkRequest keyRequest) {
    return new PublicKeyDto(
        keyRequest.getKty(),
        keyRequest.getKid(),
        keyRequest.getAlg().orElse(null),
        keyRequest.getUse().orElse(null),
        keyRequest.getCrv(),
        keyRequest.getX(),
        keyRequest.getY());
  }

  private static EcJwkResponse toKeyResponse(PublicKeyDto publicKeyDto) {
    return EcJwkResponse.builder()
        .kty(publicKeyDto.kty())
        .kid(publicKeyDto.kid())
        .alg(publicKeyDto.alg())
        .use(publicKeyDto.use())
        .crv(publicKeyDto.crv())
        .x(publicKeyDto.x())
        .y(publicKeyDto.y())
        .build();
  }

  private static SecurityEnvelopeResponse toSecurityEnvelopeResponse(String content) {
    return SecurityEnvelopeResponse.builder()
        .content(content)
        .build();
  }

  private static HsmClientIdResponse toHsmClientIdResponse(String clientId) {
    return HsmClientIdResponse.builder()
        .clientId(clientId)
        .build();
  }
}
