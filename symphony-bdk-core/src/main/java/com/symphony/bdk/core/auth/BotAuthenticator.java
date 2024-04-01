package com.symphony.bdk.core.auth;

import com.symphony.bdk.core.auth.exception.AuthUnauthorizedException;

import jakarta.annotation.Nonnull;
import org.apiguardian.api.API;

/**
 * Bot authenticator service.
 */
@API(status = API.Status.STABLE)
public interface BotAuthenticator {

  /**
   * Authenticates a Bot's service account.
   *
   * @return the authentication session.
   */
  @Nonnull AuthSession authenticateBot() throws AuthUnauthorizedException;
}
