package se.digg.wallet.account.application.controller;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.digg.wallet.account.application.model.CreateAccountRequestDto;
import se.digg.wallet.account.domain.model.AccountDto;
import se.digg.wallet.account.domain.service.AccountService;

@RestController
@RequestMapping("/account")
public class AccountController {

  private final AccountService accountService;

  public AccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  @PostMapping
  public ResponseEntity<AccountDto> createAccount(
      @RequestBody CreateAccountRequestDto createAccountRequestDto) {
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(accountService.createAccount(createAccountRequestDto));
  }

  @GetMapping("/{id}")
  public ResponseEntity<AccountDto> getMethodName(@PathVariable UUID id) {
    return accountService.getAccountById(id).map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }
}
