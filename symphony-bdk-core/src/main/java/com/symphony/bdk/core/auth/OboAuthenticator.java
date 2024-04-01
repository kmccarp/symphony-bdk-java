package com.symphony.bdk.core.auth;

import com.symphony.bdk.core.auth.exception.AuthUnauthorizedException;

import jakarta.annotation.Nonnull;
import org.apiguardian.api.API;

/**
 * On-behalf-of authenticator service.
 */
@API(status = API.Status.STABLE)
public interface OboAuthenticator {

  /**
   * Authenticates on-behalf-of a particular user using his username.
   *
   * @param username Username of the user.
   * @return the authentication session.
   */
  @Nonnull AuthSession authenticateByUsername(@Nonnull String username) throws AuthUnauthorizedException;

  /**
   * Authenticates on behalf of a particular user using his userId.
   *
   * @param userId Id of the user.
   * @return the authentication sessions.
   */
  @Nonnull AuthSession authenticateByUserId(@Nonnull Long userId) throws AuthUnauthorizedException;
}
