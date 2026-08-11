// SPDX-FileCopyrightText: 2025 Digg - Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.digg.wallet.account.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class AppIntegrityService {
  Logger logger = LoggerFactory.getLogger(AppIntegrityService.class);

  /*
   * securely parse HTTP requests, cryptographically validate Play Integrity tokens, enforce
   * business rules using a dedicated Policy file.
   */

  // Extract & Decode
  public void extractAndDecode(String integrityToken) {
    String packageName = "MY_WALLET_APP_PACKAGE_NAME";
    logger.debug("packageName {}", packageName);

    // TODO: call Google Play Integrity API
    // decodeIntegrityToken(packageName, integrityToken);

    logger.debug("decoded integrity token");
  }

  // Compute Hash
  public String computeHash(String jsonPayload) {
    // * Computes an SHA-256 hash of the JSON payload to verify Content Binding.

    String result = "empty";
    // TODO: * @returns {string} Base64 URL-encoded SHA-256 hash
    logger.debug("result: {}", result);
    return result;
  }

  // Compare Hash values
  public boolean compareHashValues(String hashFromRequest, String hashComputedFromPayload) {
    return hashFromRequest.equals(hashComputedFromPayload);
  }

  // Inspect verdicts:
  // deviceRecognitionVerdict
  // appRecognitionVerdict
  // appLicensingVerdict
  // requestPackageName
  public boolean evaluatePolicy(JSONObject playIntegrityVerdictsPayload) {

    try {
      JSONObject requestPackageName = playIntegrityVerdictsPayload
          .getJSONObject("requestDetails")
          .getJSONObject("requestPackageName");
      logger.debug("requestPackageName: {}", requestPackageName);
      // TODO: check
    } catch (JSONException e) {
      logger.warn(e.getMessage());
      // throw new RuntimeException(e);
    }

    try {
      JSONObject deviceRecognitionVerdict = playIntegrityVerdictsPayload
          .getJSONObject("deviceIntegrity")
          .getJSONObject("deviceRecognitionVerdict");
      logger.debug("deviceRecognitionVerdict: {}", deviceRecognitionVerdict);
      // TODO: check
    } catch (JSONException e) {
      logger.warn(e.getMessage());
      // throw new RuntimeException(e);
    }

    try {
      JSONObject appLicensingVerdict = playIntegrityVerdictsPayload
          .getJSONObject("accountDetails")
          .getJSONObject("appLicensingVerdict");
      logger.debug("appLicensingVerdict: {}", appLicensingVerdict);
      // TODO: check
    } catch (JSONException e) {
      logger.warn(e.getMessage());
      // throw new RuntimeException(e);
    }

    try {
      JSONObject appRecognitionVerdict = playIntegrityVerdictsPayload
          .getJSONObject("appIntegrity")
          .getJSONObject("appRecognitionVerdict");
      logger.debug("appRecognitionVerdict: {}", appRecognitionVerdict);
      // TODO: check
    } catch (JSONException e) {
      logger.warn(e.getMessage());
      // throw new RuntimeException(e);
    }

    // Looks good, didn't find any problems in the verdicts
    return true;
  }

  // Structured error (not just 500 but 403 Forbidden including why failed)
}
