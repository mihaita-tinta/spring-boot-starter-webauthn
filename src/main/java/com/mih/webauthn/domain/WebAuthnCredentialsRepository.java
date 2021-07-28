package com.mih.webauthn.domain;


import java.util.List;
import java.util.Optional;

public interface WebAuthnCredentialsRepository {

    List<WebAuthnCredentials> findAllByAppUserId(Long userId);

    Optional<WebAuthnCredentials> findByCredentialIdAndAppUserId(byte[] credentialId, Long userId);

    List<WebAuthnCredentials> findByCredentialId(byte[] credentialId);

    WebAuthnCredentials save(WebAuthnCredentials credentials);

    void deleteByAppUserId(Long appUserId);

    void deleteById(byte[] credentialsId);
}
