// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.domain.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AppIntegrityService {
  private final Logger logger = LoggerFactory.getLogger(AppIntegrityService.class);
  private static final String EXPECTED_PACKAGE_NAME = "my.package.name";

  /*
   * securely parse HTTP requests, cryptographically validate Play Integrity tokens, enforce
   * business rules using a dedicated Policy file.
   */

  // Extract & Decode
  public String extractAndDecode(String integrityToken) {

    // TODO: handle null or empty input

    // TODO: call Google Play Integrity API
    // decodeIntegrityToken(EXPECTED_PACKAGE_NAME, integrityToken);
    String decodedToken = "DECODED_TOKEN";

    logger.debug("decoded integrity token: {}", decodedToken);
    return decodedToken;
  }

  public String computeHash(String jsonPayload) throws NoSuchAlgorithmException {
    logger.debug("jsonPayload: {}", jsonPayload);

    // TODO: handle null or empty input

    // computes an SHA-256 hash of the JSON payload to verify Content Binding
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] encodedHash = digest.digest(jsonPayload.getBytes(StandardCharsets.UTF_8));

    // returns a Base64 URL-encoded String of the hash value
    String result = Base64.getUrlEncoder().withoutPadding().encodeToString(encodedHash);

    logger.debug("result: {}", result);
    return result;
  }

  // Compare Hash values
  public boolean compareHashValues(String hashFromRequest, String hashComputedFromPayload) {
    logger.debug("Comparing hash from request: {}", hashFromRequest);
    logger.debug(" with computed from payload: {}", hashComputedFromPayload);
    return hashFromRequest.equals(hashComputedFromPayload);
  }

  // Inspect verdicts:
  // deviceRecognitionVerdict
  // appRecognitionVerdict
  // appLicensingVerdict
  // requestPackageName
  public boolean evaluatePolicy(JSONObject playIntegrityVerdictsPayload) {

    String requestPackageName;
    JSONArray deviceRecognitionVerdict;
    String appLicensingVerdict;
    String appRecognitionVerdict;

    try {
      requestPackageName = playIntegrityVerdictsPayload
          .getJSONObject("requestDetails")
          .getString("requestPackageName");
      logger.debug("requestPackageName: {}", requestPackageName);

      deviceRecognitionVerdict = playIntegrityVerdictsPayload
          .getJSONObject("deviceIntegrity")
          .getJSONArray("deviceRecognitionVerdict");
      logger.debug("deviceRecognitionVerdict: {}", deviceRecognitionVerdict);

      appLicensingVerdict = playIntegrityVerdictsPayload
          .getJSONObject("accountDetails")
          .getString("appLicensingVerdict");
      logger.debug("appLicensingVerdict: {}", appLicensingVerdict);

      appRecognitionVerdict = playIntegrityVerdictsPayload
          .getJSONObject("appIntegrity")
          .getString("appRecognitionVerdict");
      logger.debug("appRecognitionVerdict: {}", appRecognitionVerdict);
    } catch (JSONException e) {
      logger.warn(e.getMessage());
      return false;
    }

    boolean packageNameValidated = evaluateRequestPackageName(requestPackageName);
    boolean deviceRecognized = evaluateDeviceRecognition(deviceRecognitionVerdict);
    boolean appLicensed = evaluateAppLicensing(appLicensingVerdict);
    boolean appRecognized = evaluateAppRecognition(appRecognitionVerdict);

    return packageNameValidated && deviceRecognized && appLicensed && appRecognized;
  }

  private boolean evaluateRequestPackageName(String packageName) {
    // TODO: requestHash/nonce and timestampMillis

    return packageName.equals(EXPECTED_PACKAGE_NAME);
  }

  private boolean evaluateDeviceRecognition(JSONArray deviceRecognition) {
    AtomicBoolean isDeviceApproved = new AtomicBoolean(false);

    // one of several possible values:
    // MEETS_DEVICE_INTEGRITY, MEETS_VIRTUAL_INTEGRITY,
    // MEETS_BASIC_INTEGRITY, MEETS_STRONG_INTEGRITY
    deviceRecognition.forEach(m -> {
      logger.debug(m.toString());
      switch (m.toString()) {
        case "" -> logger.warn("empty");
        case "MEETS_DEVICE_INTEGRITY" -> {
          logger.debug("Meets device integrity");
          isDeviceApproved.set(true);
        }
        case "MEETS_VIRTUAL_INTEGRITY" -> logger.debug("Meets virtual integrity");
        case "MEETS_BASIC_INTEGRITY" -> logger.debug("Meets basic integrity");
        case "MEETS_STRONG_INTEGRITY" -> logger.debug("Meets strong integrity");
        default -> logger.error("unexpected device integrity: {}", m);
      }
    });

    return isDeviceApproved.get();
  }

  private boolean evaluateAppLicensing(String appLicensing) {
    boolean isAppLicensed = false;

    // This field can be LICENSED, UNLICENSED, or UNEVALUATED.
    switch (appLicensing) {
      case "LICENSED" -> {
        logger.debug("Licensed.");
        isAppLicensed = true;
      }
      case "UNLICENSED" -> {
        logger.warn("Unlicensed");
        // TODO: show GET_LICENSED dialog to user
      }
      case "UNEVALUATED" -> logger.warn("Unevaluated");
      default -> logger.error("unexpected licensing: {}", appLicensing);
    }
    return isAppLicensed;
  }

  private boolean evaluateAppRecognition(String appRecognition) {
    boolean isAppRecognized = false;

    // PLAY_RECOGNIZED, UNRECOGNIZED_VERSION, or UNEVALUATED.
    switch (appRecognition) {
      case "PLAY_RECOGNIZED" -> {
        logger.debug("Play recognized");
        isAppRecognized = true;
      }
      case "UNRECOGNIZED_VERSION" -> logger.warn("Unrecognized version");
      case "UNEVALUATED" -> logger.warn("App integrity unevaluated");
      default -> logger.error("unexpected app recognition: {}", appRecognition);
    }
    return isAppRecognized;
  }

  // TODO: Structured error (not just 500 but 403 Forbidden including why failed)
}
