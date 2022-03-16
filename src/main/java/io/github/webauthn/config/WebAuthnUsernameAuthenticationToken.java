package io.github.webauthn.config;

import io.github.webauthn.domain.DefaultWebAuthnCredentials;
import io.github.webauthn.domain.WebAuthnCredentials;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * A {@link UsernamePasswordAuthenticationToken} containing a {@link WebAuthnCredentials}
 */
public class WebAuthnUsernameAuthenticationToken extends UsernamePasswordAuthenticationToken {
    private final WebAuthnCredentials credentials;

    public WebAuthnUsernameAuthenticationToken(Object principal, WebAuthnCredentials credentials) {
        super(principal, credentials);
        this.credentials = credentials;
    }

    public WebAuthnUsernameAuthenticationToken(Object principal, WebAuthnCredentials credentials, Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
        this.credentials = credentials;
    }

    @Override
    public WebAuthnCredentials getCredentials() {
        return credentials;
    }
}
