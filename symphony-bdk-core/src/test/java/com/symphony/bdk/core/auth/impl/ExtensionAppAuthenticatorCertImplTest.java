package com.symphony.bdk.core.auth.impl;

import static com.symphony.bdk.core.test.BdkRetryConfigTestHelper.ofMinimalInterval;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.symphony.bdk.core.auth.AppAuthSession;
import com.symphony.bdk.core.auth.ExtensionAppTokensRepository;
import com.symphony.bdk.core.auth.exception.AuthUnauthorizedException;
import com.symphony.bdk.core.test.MockApiClient;
import com.symphony.bdk.http.api.ApiRuntimeException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExtensionAppAuthenticatorCertImplTest {
  private static final String V1_EXTENSION_APP_AUTHENTICATE = "/sessionauth/v1/authenticate/extensionApp";
  public static final String V1_APP_POD_CERTIFICATE = "/sessionauth/v1/app/pod/certificate";

  private ExtensionAppAuthenticatorCertImpl authenticator;
  private MockApiClient mockApiClient;
  private ExtensionAppTokensRepository tokensRepository;

  @BeforeEach
  void init() {
    mockApiClient = new MockApiClient();
    tokensRepository = spy(new InMemoryTokensRepository());
    authenticator = new ExtensionAppAuthenticatorCertImpl(
        ofMinimalInterval(1),
        "appId",
        mockApiClient.getApiClient("/sessionauth"),
        tokensRepository);
  }

  @Test
  void testConstructObject() {
    assertNotNull(new ExtensionAppAuthenticatorCertImpl(ofMinimalInterval(1), "appId",
        mockApiClient.getApiClient("/sessionauth")));
  }

  @Test
  void testAuthenticateExtensionApp() throws AuthUnauthorizedException {
    final String appToken = "APP_TOKEN";
    final String symphonyToken = "SYMPHONY_TOKEN";

    mockApiClient.onPost(V1_EXTENSION_APP_AUTHENTICATE, "{\n"
        + "  \"appId\" : \"appId\",\n"
        + "  \"appToken\" : \"" + appToken + "\",\n"
        + "  \"symphonyToken\" : \"" + symphonyToken + "\",\n"
        + "  \"expireAt\" : 1539636528288\n"
        + "}");

    final AppAuthSession session = authenticator.authenticateExtensionApp(appToken);

    assertEquals(AppAuthSessionCertImpl.class, session.getClass());
    assertEquals(authenticator, ((AppAuthSessionCertImpl) session).getAuthenticator());
    assertEquals(session.getAppToken(), appToken);
    assertEquals(session.getSymphonyToken(), symphonyToken);
    assertEquals(session.expireAt(), 1539636528288L);

    verify(tokensRepository).save(eq(appToken), eq(symphonyToken));
    assertTrue(authenticator.validateTokens(appToken, symphonyToken));
    assertFalse(authenticator.validateTokens("OTHER_TOKEN", symphonyToken));
    assertFalse(authenticator.validateTokens(appToken, "OTHER_TOKEN"));
  }

  @Test
  void testRetrieveExtensionAppSessionUnauthorized() {
    mockApiClient.onPost(401, V1_EXTENSION_APP_AUTHENTICATE, "{}");

    assertThrows(AuthUnauthorizedException.class, () -> authenticator.authenticateExtensionApp("APP_TOKEN"));
  }

  @Test
  void testRetrieveExtensionAppSessionApiException() {
    mockApiClient.onPost(400, V1_EXTENSION_APP_AUTHENTICATE, "{}");

    assertThrows(ApiRuntimeException.class, () -> authenticator.authenticateExtensionApp("APP_TOKEN"));
  }

  @Test
  void testGetPodCertificate() {
    mockApiClient.onGet(200, V1_APP_POD_CERTIFICATE, "{ \"certificate\" : \"PEM_content\"}");

    assertEquals("PEM_content", authenticator.getPodCertificate().getCertificate());
  }

  @Test
  void testGetPodCertificateFailure() {
    mockApiClient.onGet(500, V1_APP_POD_CERTIFICATE, "{}");

    assertThrows(ApiRuntimeException.class, () -> authenticator.getPodCertificate().getCertificate());
  }
}
