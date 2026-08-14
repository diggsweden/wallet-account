// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.domain.service;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import se.digg.wallet.account.application.exception.NoIntegrityTokenException;
import se.digg.wallet.account.application.exception.NoPayloadException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AppIntegrityServiceTest {

  private final AppIntegrityService appIntegrityService = new AppIntegrityService();
  private final String MY_PACKAGE_NAME = "my.package.name";
  private final String NONCE = "def456";
  private final long FIVE_MINUTES_IN_MILLISECONDS = 5 * 60 * 1000;

  @ParameterizedTest
  @ValueSource(strings = {"someToken"})
  void integrityTokenIsExtractedAndDecoded(String integrityToken) throws Exception {
    String expectedToken = "DECODED_TOKEN";

    String result = appIntegrityService.extractAndDecode(integrityToken);

    assertThat(result).isEqualTo(expectedToken);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void missingIntegrityTokenThrowsException(String integrityToken) {
    assertThrows(NoIntegrityTokenException.class,
        () -> appIntegrityService.extractAndDecode(integrityToken));
  }

  @Test
  void payloadHashIsComputed() throws Exception {
    String expectedHash = "Ytg_1fBCKsvu1A9S2iSqyoCiQv45iq0gdzls8VbLpZI";

    long timestampMillis = 1786611850378L;
    String result = appIntegrityService.computeHash(
        createPayload(MY_PACKAGE_NAME, NONCE, timestampMillis).toString());

    assertThat(result).isEqualTo(expectedHash);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void missingPayloadThrowsException(String payload) throws Exception {
    assertThrows(NoPayloadException.class,
        () -> appIntegrityService.computeHash(payload));
  }

  @Test
  void differentHashValuesAreComparedFalse() {
    boolean result = appIntegrityService.compareHashValues("hashOne", "hash2");

    assertFalse(result);
  }

  @Test
  void sameHashValuesAreComparedTrue() {
    String aHashValue = "aHashValue";
    boolean result = appIntegrityService.compareHashValues(aHashValue, aHashValue);

    assertTrue(result);
  }

  @Test
  void payloadIsEvaluatedAccordingToPolicy() throws JSONException {
    long timestampMillis = Instant.now().toEpochMilli() - FIVE_MINUTES_IN_MILLISECONDS;
    boolean result = appIntegrityService
        .evaluatePolicy(createPayload(MY_PACKAGE_NAME, NONCE, timestampMillis));

    assertTrue(result);
  }


  // @formatter:off
  /**
   * { "requestDetails": { ... }
   *   "accountDetails": { ... },
   *   "appIntegrity": { ... },
   *   "deviceIntegrity": { ... },
   *   "environmentDetails": { ... }
   * }
   */
  // @formatter:on
  private JSONObject createPayload(String packageName, String nonce,
      long timestampMillis) throws JSONException {

    JSONObject thePayload = new JSONObject();
    thePayload.put("requestDetails",
        createRequestDetails(packageName, nonce, timestampMillis));
    thePayload.put("accountDetails", createAccountDetails());
    thePayload.put("appIntegrity", createAppIntegrity(packageName));
    thePayload.put("deviceIntegrity", createDeviceIntegrity());

    // If you have opted in to the App Access Risk verdict or the Play Protect verdict
    thePayload.put("environmentDetails", createEnvironmentDetails());

    return thePayload;
  }

  // @formatter:off
  /**
   * For classic API requests:
   * "requestDetails": {
   *   // Application package name this attestation was requested for.
   *   // Note that this field might be spoofed in the middle of the request.
   *   "requestPackageName": "com.package.name",
   *   // base64-encoded URL-safe no-wrap nonce provided by the developer.
   *   "nonce": "aGVsbG8gd29scmQgdGhlcmU",
   *   // The timestamp in milliseconds when the request was made (computed on the server).
   *   "timestampMillis": "1617893780"
   * }
   */
  // @formatter:on
  private JSONObject createRequestDetails(String packageName, String nonce,
      long timestampMillis) throws JSONException {
    JSONObject theRequestDetails = new JSONObject();
    theRequestDetails.put("requestPackageName", packageName);
    theRequestDetails.put("timestampMillis", timestampMillis);

    // for classic API requests, there is a nonce
    theRequestDetails.put("nonce", nonce);

    return theRequestDetails;
  }

  // @formatter:off
  /**
   * "accountDetails": {
   *   // This field can be LICENSED, UNLICENSED, or UNEVALUATED.
   *   "appLicensingVerdict": "LICENSED"
   * }
   */
  // @formatter:on
  private JSONObject createAccountDetails() throws JSONException {
    JSONObject theAccountDetails = new JSONObject();

    // LICENSED, UNLICENSED, or UNEVALUATED.
    String appLicensingVerdict = "LICENSED";

    theAccountDetails.put("appLicensingVerdict", appLicensingVerdict);
    return theAccountDetails;
  }

  // @formatter:off
  /**
   * "appIntegrity": {
   *   // PLAY_RECOGNIZED, UNRECOGNIZED_VERSION, or UNEVALUATED.
   *   "appRecognitionVerdict": "PLAY_RECOGNIZED",
   *   // The package name of the app.
   *   // This field is populated iff appRecognitionVerdict != UNEVALUATED.
   *   "packageName": "com.package.name",
   *   // The sha256 digest of app certificates (base64-encoded URL-safe).
   *   // This field is populated iff appRecognitionVerdict != UNEVALUATED.
   *   "certificateSha256Digest": ["6a6a1474b5cbbb2b1aa57e0bc3"],
   *   // The version of the app.
   *   // This field is populated iff appRecognitionVerdict != UNEVALUATED.
   *   "versionCode": "42"
   * }
   */
  // @formatter:on
  private JSONObject createAppIntegrity(String packageName) throws JSONException {
    JSONObject theAppIntegrity = new JSONObject();

    // PLAY_RECOGNIZED, UNRECOGNIZED_VERSION, or UNEVALUATED.
    String appRecognitionVerdict = "PLAY_RECOGNIZED";

    // iff appRecognitionVerdict != UNEVALUATED
    String certificateSha256Digest = "certificateSha256Digest";
    String versionCode = "1234";

    theAppIntegrity.put("appRecognitionVerdict", appRecognitionVerdict);
    theAppIntegrity.put("packageName", packageName);
    theAppIntegrity.put("certificateSha256Digest", certificateSha256Digest);
    theAppIntegrity.put("versionCode", versionCode);
    return theAppIntegrity;
  }

  // @formatter:off
  /**
   * "deviceIntegrity": {
   *   // "MEETS_DEVICE_INTEGRITY" is one of several possible values.
   *   "deviceRecognitionVerdict": ["MEETS_DEVICE_INTEGRITY"]
   * }
   */
  // @formatter:on
  private JSONObject createDeviceIntegrity() throws JSONException {
    JSONObject theDeviceIntegrity = new JSONObject();
    JSONArray deviceRecognitionVerdict = new JSONArray();

    // one of several possible values:
    // MEETS_DEVICE_INTEGRITY, MEETS_VIRTUAL_INTEGRITY,
    // MEETS_BASIC_INTEGRITY, MEETS_STRONG_INTEGRITY
    String meets = "MEETS_DEVICE_INTEGRITY";
    deviceRecognitionVerdict.put(meets);
    theDeviceIntegrity.put("deviceRecognitionVerdict", deviceRecognitionVerdict);

    // If you opt in to receive deviceAttributes
    theDeviceIntegrity.put("deviceAttributes", createDeviceAttributes());

    // If you opt in to receive recentDeviceActivity
    theDeviceIntegrity.put("recentDeviceActivity", createRecentDeviceActivity());

    // If you opt in to deviceRecall
    theDeviceIntegrity.put("deviceRecall", createDeviceRecall());
    return theDeviceIntegrity;
  }

  // @formatter:off
  /**
   * "deviceAttributes": {
   *   // 33 is one possible value, which represents Android 13 (Tiramisu).
   *   "sdkVersion": 33
   * }
   */
  // @formatter:on
  private JSONObject createDeviceAttributes() throws JSONException {
    JSONObject theDeviceAttributes = new JSONObject();

    theDeviceAttributes.put("sdkVersion", 33);
    return theDeviceAttributes;
  }

  // @formatter:off
  /**
   * "recentDeviceActivity": {
   *   // "LEVEL_2" is one of several possible values.
   *   "deviceActivityLevel": "LEVEL_2"
   * }
   */
  // @formatter:on
  private JSONObject createRecentDeviceActivity() throws JSONException {
    JSONObject theDRecentDeviceActivity = new JSONObject();

    // LEVEL_1 (lowest) 10 or fewer 5 or fewer
    // LEVEL_2 Between 11 and 25 Between 6 and 10
    // LEVEL_3 Between 26 and 50 Between 11 and 15
    // LEVEL_4 (highest) More than 50 More than 15
    // UNEVALUATED
    String deviceActivityLevel = "LEVEL_2";

    theDRecentDeviceActivity.put("deviceActivityLevel", deviceActivityLevel);

    return theDRecentDeviceActivity;
  }

  // @formatter:off
  /**
   * "deviceRecall": {
   *   "values": {},
   *   "writeDates": {}
   * }
   */
  // @formatter:on
  private JSONObject createDeviceRecall() throws JSONException {
    JSONObject theDeviceRecall = new JSONObject();

    theDeviceRecall.put("values", new JSONObject());
    theDeviceRecall.put("writeDates", new JSONObject());

    return theDeviceRecall;
  }

  // @formatter:off
  /**
   * "environmentDetails": {
   *   "appAccessRiskVerdict": {
   *   // This field contains one or more responses, for example the following.
   *   "appsDetected": ["KNOWN_INSTALLED", "UNKNOWN_INSTALLED", "UNKNOWN_CAPTURING"]
   *   }
   * }
   * or
   * "environmentDetails": {
   *   "playProtectVerdict": "NO_ISSUES"
   * }
   */
  // @formatter:on
  private JSONObject createEnvironmentDetails() throws JSONException {
    JSONObject theRequestDetails = new JSONObject();

    theRequestDetails.put("appAccessRiskVerdict", createAppAccessRiskVerdict());
    theRequestDetails.put("playProtectVerdict", createPlayProtectVerdict());

    return theRequestDetails;
  }

  // @formatter:off
  /**
   * "appAccessRiskVerdict": {
   *   // This field contains one or more responses, for example the following.
   *   "appsDetected": ["KNOWN_INSTALLED", "UNKNOWN_INSTALLED", "UNKNOWN_CAPTURING"]
   * }
   * KNOWN_INSTALLED, UNKNOWN_INSTALLED, KNOWN_CAPTURING, UNKNOWN_CAPTURING, KNOWN_CONTROLLING,
   * UNKNOWN_CONTROLLING, KNOWN_OVERLAYS, UNKNOWN_OVERLAYS, Empty (a blank value)
   */
  // @formatter:on
  private JSONObject createAppAccessRiskVerdict() throws JSONException {
    JSONObject theAppAccessRiskVerdict = new JSONObject();

    JSONArray appsDetected = new JSONArray();
    appsDetected.put("KNOWN_INSTALLED");
    appsDetected.put("UNKNOWN_INSTALLED");
    appsDetected.put("UNKNOWN_CAPTURING");

    theAppAccessRiskVerdict.put("appDetected", appsDetected);

    return theAppAccessRiskVerdict;
  }

  // @formatter:off
  /**
   * "playProtectVerdict": "NO_ISSUES"
   * NO_ISSUES, NO_DATA, POSSIBLE_RISK, MEDIUM_RISK, HIGH_RISK, UNEVALUATED
   */
  // @formatter:on
  private JSONObject createPlayProtectVerdict() throws JSONException {
    JSONObject thePlayProtecteVerdict = new JSONObject();

    thePlayProtecteVerdict.put("playProtectVerdict", "MEDIUM_RISK");

    return thePlayProtecteVerdict;
  }
}
