// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.domain.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.digg.wallet.account.application.exception.NoIntegrityTokenException;
import se.digg.wallet.account.application.exception.NoPayloadException;

@Component
public class AppIntegrityService {
  private final Logger logger = LoggerFactory.getLogger(AppIntegrityService.class);
  private static final String EXPECTED_PACKAGE_NAME_FROM_ORIGINAL_REQUEST = "my.package.name";

  // TODO: storage for the nonce values until they expire
  private static final String EXPECTED_NONCE_FROM_ORIGINAL_REQUEST = "def456";

  public String extractAndDecode(String integrityToken) throws NoIntegrityTokenException {
    logger.debug("integrity token: {}", integrityToken);

    if (integrityToken == null || integrityToken.isBlank()) {
      throw new NoIntegrityTokenException("integrityToken is missing");
    }

    // TODO: call Google Play Integrity API
    // decodeIntegrityToken(EXPECTED_PACKAGE_NAME_FROM_ORIGINAL_REQUEST, integrityToken);
    String decodedToken = "DECODED_TOKEN";

    logger.debug("decoded integrity token: {}", decodedToken);
    return decodedToken;
  }

  public String computeHash(String jsonPayload) throws NoSuchAlgorithmException {
    logger.debug("jsonPayload: {}", jsonPayload);

    if (jsonPayload == null || jsonPayload.isBlank()) {
      throw new NoPayloadException("payload is missing");
    }

    // computes an SHA-256 hash of the JSON payload to verify Content Binding
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] encodedHash = digest.digest(jsonPayload.getBytes(StandardCharsets.UTF_8));

    // returns a Base64 URL-encoded String of the hash value
    String result = Base64.getUrlEncoder().withoutPadding().encodeToString(encodedHash);

    logger.debug("result: {}", result);
    return result;
  }

  public boolean compareHashValues(String hashFromRequest, String hashComputedFromPayload) {
    logger.debug("Comparing hash from request: {}", hashFromRequest);
    logger.debug(" with computed from payload: {}", hashComputedFromPayload);
    return hashFromRequest.equals(hashComputedFromPayload);
  }

  public boolean evaluatePolicy(JSONObject playIntegrityVerdictsPayload) {
    logger.debug("playIntegrityVerdictsPayload: {}", playIntegrityVerdictsPayload);

    if (playIntegrityVerdictsPayload == null || playIntegrityVerdictsPayload.isEmpty()) {
      throw new NoPayloadException("payload is missing");
    }

    JSONObject requestDetails;
    JSONArray deviceRecognitionVerdict;
    String appLicensingVerdict;
    String appRecognitionVerdict;

    try {
      requestDetails = playIntegrityVerdictsPayload
          .getJSONObject("requestDetails");
      logger.debug("requestDetails: {}", requestDetails);

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

    if (!isRequestDetailsValidated(requestDetails)) {
      return false;
    }
    if (!isDeviceRecognized(deviceRecognitionVerdict)) {
      return false;
    }
    if (!isAppLicensed(appLicensingVerdict)) {
      return false;
    }
    if (!isAppRecognized(appRecognitionVerdict)) {
      return false;
    }

    return true;
  }

  private boolean isRequestDetailsValidated(JSONObject requestDetails) {
    String packageName = requestDetails.getString("requestPackageName");
    if (!isExpectedPackageName(packageName)) {
      logger.warn("Unexpected package name: {}", packageName);
      return false;
    }

    // we are using classic API requests which include a nonce
    String nonce = requestDetails.getString("nonce");
    if (!isExpectedNonce(nonce)) {
      logger.warn("Unexpected nonce: {}", nonce);
      return false;
    }

    long timestampMillis = requestDetails.getLong("timestampMillis");
    if (!isFreshTimestamp(timestampMillis)) {
      logger.warn("Rotten timestamp: {}", timestampMillis);
      return false;
    }

    return true;
  }

  private boolean isExpectedPackageName(String packageName) {
    return packageName.equals(EXPECTED_PACKAGE_NAME_FROM_ORIGINAL_REQUEST);
  }

  private boolean isExpectedNonce(String nonce) {
    // TODO: verify the nonce
    return nonce.equals(EXPECTED_NONCE_FROM_ORIGINAL_REQUEST);
  }

  private boolean isFreshTimestamp(long timestampMillis) {
    // Ensure the freshness of the token.

    // allow tokens to be max 10 minutes old
    long ALLOWED_WINDOW_MILLIS = 10 * 60 * 1000;
    long currentTimestampMillis = Instant.now().toEpochMilli();
    logger.debug("current: {}", currentTimestampMillis);
    logger.debug(" requested: {}", timestampMillis);
    logger.debug(" diff: {}", currentTimestampMillis - timestampMillis);
    logger.debug(" allowed max: {}", ALLOWED_WINDOW_MILLIS);
    return currentTimestampMillis - timestampMillis <= ALLOWED_WINDOW_MILLIS;
  }

  private boolean isDeviceRecognized(JSONArray deviceRecognition) {
    AtomicBoolean isDeviceApproved = new AtomicBoolean(false);

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

  private boolean isAppLicensed(String appLicensing) {
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

  private boolean isAppRecognized(String appRecognition) {
    boolean isAppRecognized = false;

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
