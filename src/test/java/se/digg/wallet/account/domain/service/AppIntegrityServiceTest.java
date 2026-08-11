// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.domain.service;

import org.junit.jupiter.api.Test;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AppIntegrityServiceTest {

  private final AppIntegrityService appIntegrityService = new AppIntegrityService();

  @Test
  void integrityTokenIsExtractedAndDecoded() {
    appIntegrityService.extractAndDecode("");
    // TODO: verify result
  }

  @Test
  void payloadHashIsComputed() throws JSONException {
    appIntegrityService.computeHash(createPayload().toString());
    // TODO: verify result
  }

  @Test
  void hashValuesAreCompared() {
    appIntegrityService.compareHashValues("", "2");
    // TODO: verify result
  }

  @Test
  void payloadIsEvaluatedAccordingToPolicy() throws JSONException {
    appIntegrityService.evaluatePolicy(createPayload());
    // TODO: verify result
  }


  /*
   * { "requestDetails": { ... }, "accountDetails": { ... }, "appIntegrity": { ... },
   * "deviceIntegrity": { ... }, "environmentDetails": { ... } }
   */
  private JSONObject createPayload() throws JSONException {

    JSONObject thePayload = new JSONObject();
    thePayload.put("requestDetails", createRequestDetails());
    thePayload.put("accountDetails", createAccountDetails());
    thePayload.put("appIntegrity", createAppIntegrity());
    thePayload.put("deviceIntegrity", createDeviceIntegrity());

    // If you have opted in to the App Access Risk verdict or the Play Protect verdict
    thePayload.put("environmentDetails", createEnvironmentDetails());

    return thePayload;
  }

  /*
   * For standard: "requestDetails": { // Application package name this attestation was requested
   * for. // Note that this field might be spoofed in the middle of the request.
   * "requestPackageName": "com.package.name", // Request hash provided by the developer.
   * "requestHash": "aGVsbG8gd29scmQgdGhlcmU", // The timestamp in milliseconds when the integrity
   * token was requested. "timestampMillis": "1675655009345" }
   *
   * or for classic:
   *
   * "requestDetails": { // Application package name this attestation was requested for. // Note
   * that this field might be spoofed in the middle of the request. "requestPackageName":
   * "com.package.name", // base64-encoded URL-safe no-wrap nonce provided by the developer.
   * "nonce": "aGVsbG8gd29scmQgdGhlcmU", // The timestamp in milliseconds when the request was made
   * (computed on the server). "timestampMillis": "1617893780" }
   */
  private JSONObject createRequestDetails() throws JSONException {
    JSONObject theRequestDetails = new JSONObject();
    String packageName = "PACKAGE_NAME";
    String requestHash = "REQUEST_HASH";
    int timestampMillis = 1234;
    String nonce = "NONCE";
    theRequestDetails.put("requestPackageName", packageName);
    theRequestDetails.put("requestHash", requestHash);

    // for standard
    theRequestDetails.put("timestampMillis", timestampMillis);

    // or for classic
    theRequestDetails.put("nonce", nonce);

    return theRequestDetails;
  }

  /*
   * "accountDetails": { // This field can be LICENSED, UNLICENSED, or UNEVALUATED.
   * "appLicensingVerdict": "LICENSED" }
   */
  private JSONObject createAccountDetails() throws JSONException {
    JSONObject theAccountDetails = new JSONObject();

    // LICENSED, UNLICENSED, or UNEVALUATED.
    String appLicensingVerdict = "LICENSED";

    theAccountDetails.put("appLicensingVerdict", appLicensingVerdict);
    return theAccountDetails;
  }

  /*
   * "appIntegrity": { // PLAY_RECOGNIZED, UNRECOGNIZED_VERSION, or UNEVALUATED.
   * "appRecognitionVerdict": "PLAY_RECOGNIZED", // The package name of the app. // This field is
   * populated iff appRecognitionVerdict != UNEVALUATED. "packageName": "com.package.name", // The
   * sha256 digest of app certificates (base64-encoded URL-safe). // This field is populated iff
   * appRecognitionVerdict != UNEVALUATED. "certificateSha256Digest":
   * ["6a6a1474b5cbbb2b1aa57e0bc3"], // The version of the app. // This field is populated iff
   * appRecognitionVerdict != UNEVALUATED. "versionCode": "42" }
   */
  private JSONObject createAppIntegrity() throws JSONException {
    JSONObject theAppIntegrity = new JSONObject();

    // PLAY_RECOGNIZED, UNRECOGNIZED_VERSION, or UNEVALUATED.
    String appRecognitionVerdict = "PLAY_RECOGNIZED";

    // iff appRecognitionVerdict != UNEVALUATED
    String packageName = "PACKAGE_NAME";
    String certificateSha256Digest = "certificateSha256Digest";
    String versionCode = "1234";

    theAppIntegrity.put("appRecognitionVerdict", appRecognitionVerdict);
    theAppIntegrity.put("packageName", packageName);
    theAppIntegrity.put("certificateSha256Digest", certificateSha256Digest);
    theAppIntegrity.put("versionCode", versionCode);
    return theAppIntegrity;
  }

  /*
   * "deviceIntegrity": { // "MEETS_DEVICE_INTEGRITY" is one of several possible values.
   * "deviceRecognitionVerdict": ["MEETS_DEVICE_INTEGRITY"] }
   */
  private JSONObject createDeviceIntegrity() throws JSONException {
    JSONObject theDeviceIntegrity = new JSONObject();
    JSONArray deviceRecognitionVerdict = new JSONArray();

    // one of several possible values: MEETS_VIRTUAL_INTEGRITY, MEETS_BASIC_INTEGRITY,
    // MEETS_STRONG_INTEGRITY,
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

  /*
   * "deviceAttributes": { // 33 is one possible value, which represents Android 13 (Tiramisu).
   * "sdkVersion": 33 }
   */
  private JSONObject createDeviceAttributes() throws JSONException {
    JSONObject theDeviceAttributes = new JSONObject();

    theDeviceAttributes.put("sdkVersion", 33);
    return theDeviceAttributes;
  }

  /*
   * "recentDeviceActivity": { // "LEVEL_2" is one of several possible values.
   * "deviceActivityLevel": "LEVEL_2" }
   */
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

  /*
   * "deviceRecall": { "values": {}, "writeDates": {} }
   */
  private JSONObject createDeviceRecall() throws JSONException {
    JSONObject theDeviceRecall = new JSONObject();

    theDeviceRecall.put("values", new JSONObject());
    theDeviceRecall.put("writeDates", new JSONObject());

    return theDeviceRecall;
  }

  /*
   * "environmentDetails": { "appAccessRiskVerdict": { // This field contains one or more responses,
   * for example the following. "appsDetected": ["KNOWN_INSTALLED", "UNKNOWN_INSTALLED",
   * "UNKNOWN_CAPTURING"] } }
   *
   * "environmentDetails": { "playProtectVerdict": "NO_ISSUES" }
   */
  private JSONObject createEnvironmentDetails() throws JSONException {
    JSONObject theRequestDetails = new JSONObject();

    theRequestDetails.put("appAccessRiskVerdict", createAppAccessRiskVerdict());
    theRequestDetails.put("playProtectVerdict", createPlayProtectVerdict());

    return theRequestDetails;
  }

  /*
   * "appAccessRiskVerdict": { // This field contains one or more responses, for example the
   * following. "appsDetected": ["KNOWN_INSTALLED", "UNKNOWN_INSTALLED", "UNKNOWN_CAPTURING"] }
   * KNOWN_INSTALLED, UNKNOWN_INSTALLED, KNOWN_CAPTURING, UNKNOWN_CAPTURING, KNOWN_CONTROLLING,
   * UNKNOWN_CONTROLLING, KNOWN_OVERLAYS, UNKNOWN_OVERLAYS, Empty (a blank value)
   */
  private JSONObject createAppAccessRiskVerdict() throws JSONException {
    JSONObject theAppAccessRiskVerdict = new JSONObject();

    JSONArray appsDetected = new JSONArray();
    appsDetected.put("KNOWN_INSTALLED");
    appsDetected.put("UNKNOWN_INSTALLED");
    appsDetected.put("UNKNOWN_CAPTURING");

    theAppAccessRiskVerdict.put("appDetected", appsDetected);

    return theAppAccessRiskVerdict;
  }

  /*
   * "playProtectVerdict": "NO_ISSUES" NO_ISSUES, NO_DATA, POSSIBLE_RISK, MEDIUM_RISK, HIGH_RISK,
   * UNEVALUATED
   */
  private JSONObject createPlayProtectVerdict() throws JSONException {
    JSONObject thePlayProtecteVerdict = new JSONObject();

    thePlayProtecteVerdict.put("playProtectVerdict", "MEDIUM_RISK");

    return thePlayProtecteVerdict;
  }
}
