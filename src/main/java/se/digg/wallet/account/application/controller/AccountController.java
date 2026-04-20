// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.application.controller;

import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import se.digg.wallet.account.api.origin.AccountControllerApi;
import se.digg.wallet.account.api.origin.model.AccountDto;
import se.digg.wallet.account.api.origin.model.CreateAccountRequestDto;
import se.digg.wallet.account.domain.service.AccountService;
import se.digg.wallet.account.domain.service.JwkValidationService;

@RestController
public class AccountController implements AccountControllerApi {

  private final AccountService accountService;

  private final JwkValidationService jwkValidationService;

  public AccountController(AccountService accountService,
      JwkValidationService jwkValidationService) {
    this.accountService = accountService;
    this.jwkValidationService = jwkValidationService;
  }

  @Override
  public ResponseEntity<AccountDto> createAccount(@Valid @RequestBody CreateAccountRequestDto createAccountRequestDto) {

    var createAccountDto = toCreateAccountDto(createAccountRequestDto);
    if (!jwkValidationService.validateJwk(createAccountDto.publicKey())) {
      return ResponseEntity.badRequest().build();
    }

    var createdAccountDto = accountService.createAccount(createAccountDto);
    return ResponseEntity
      .status(HttpStatus.CREATED)
      .body(toAccountResponse(createdAccountDto));
  }

  @Override
  public ResponseEntity<AccountDto> getAccount(UUID id) {
    var accountDto = accountService.getAccountById(id);
    return accountDto.map(AccountController::toAccountResponse)
      .map(ResponseEntity::ok)
      .orElse(ResponseEntity.notFound().build());
  }

  private static se.digg.wallet.account.application.model.CreateAccountRequestDto toCreateAccountDto(CreateAccountRequestDto createAccountRequestDto) {

    var publicKey = createAccountRequestDto.getPublicKey();

    return new se.digg.wallet.account.application.model.CreateAccountRequestDto(
      createAccountRequestDto.getPersonalIdentityNumber(),
      createAccountRequestDto.getEmailAdress(),
      createAccountRequestDto.getTelephoneNumber(),
      publicKey.map(pk -> new se.digg.wallet.account.application.model.PublicKeyDto(
          pk.getKty(),
          pk.getKid().orElse(null),
          pk.getAlg().orElse(null),
          pk.getUse().orElse(null),
          pk.getCrv(),
          pk.getX(),
          pk.getY()))
        .orElse(null));
  }

  private static AccountDto toAccountResponse(se.digg.wallet.account.domain.model.AccountDto accountDto) {

    var publicKey = accountDto.publicKey();

    return AccountDto.builder()
      .id(accountDto.id())
      .personalIdentityNumber(accountDto.personalIdentityNumber())
      .emailAdress(accountDto.emailAdress())
      .telephoneNumber(accountDto.telephoneNumber().orElse(null))
      .publicKey(se.digg.wallet.account.api.origin.model.PublicKeyDto.builder()
        .kty(publicKey.kty())
        .kid(publicKey.kid())
        .alg(publicKey.alg())
        .use(publicKey.use())
        .x(publicKey.x())
        .y(publicKey.y())
        .build())
      .build();
  }
}
