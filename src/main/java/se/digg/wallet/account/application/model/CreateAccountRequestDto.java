package se.digg.wallet.account.application.model;

import io.soabase.recordbuilder.core.RecordBuilder;
import jakarta.validation.constraints.NotEmpty;
import java.util.Optional;

@RecordBuilder
public record CreateAccountRequestDto(
    @NotEmpty String personalIdentityNumber,
    @NotEmpty String emailAdress,
    Optional<String> telephoneNumber,
    PublicKeyDto publicKey) {

}
