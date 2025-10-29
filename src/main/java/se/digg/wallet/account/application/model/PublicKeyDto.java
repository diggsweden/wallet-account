package se.digg.wallet.account.application.model;

public record PublicKeyDto(
    String publicKeyBase64,
    String publicKeyIdentifier) {
}
