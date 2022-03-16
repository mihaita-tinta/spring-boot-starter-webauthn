package io.github.webauthn.flows;

import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.UserIdentity;
import io.github.webauthn.BytesUtil;
import io.github.webauthn.config.WebAuthnOperation;
import io.github.webauthn.domain.*;
import io.github.webauthn.dto.RegistrationStartRequest;
import io.github.webauthn.dto.RegistrationStartResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import static org.springframework.util.StringUtils.hasText;

public class WebAuthnRegistrationStartStrategy {
    private static final Logger log = LoggerFactory.getLogger(WebAuthnRegistrationStartStrategy.class);

    private final WebAuthnUserRepository<WebAuthnUser> webAuthnUserRepository;
    private final WebAuthnCredentialsRepository<WebAuthnCredentials> webAuthnCredentialRepository;
    private final SecureRandom random = new SecureRandom();
    private final RelyingParty relyingParty;
    private final WebAuthnOperation registrationOperation;

    public WebAuthnRegistrationStartStrategy(WebAuthnUserRepository webAuthnUserRepository, WebAuthnCredentialsRepository webAuthnCredentialRepository, RelyingParty relyingParty, WebAuthnOperation registrationOperation) {
        this.webAuthnUserRepository = webAuthnUserRepository;
        this.webAuthnCredentialRepository = webAuthnCredentialRepository;
        this.relyingParty = relyingParty;
        this.registrationOperation = registrationOperation;
    }

    public RegistrationStartResponse registrationStart(RegistrationStartRequest request, Optional<WebAuthnUser> currentUser) {
        log.debug("registrationStart - {}", request);

        long userId = -1;
        String name = null;
        RegistrationStartResponse.Mode mode = null;

        if (currentUser.isPresent()) {

            WebAuthnUser user = currentUser.get();
            userId = user
                    .getId();
            name = user.getUsername();
            mode = RegistrationStartResponse.Mode.MIGRATE;
        } else if (hasText(request.getUsername())) {
            WebAuthnUser user = this.webAuthnUserRepository.findByUsername(request.getUsername())
                    .map(u -> {
                        if (u.isEnabled())
                            throw new UsernameAlreadyExistsException("Username taken");
                        // FIXME address race condition
                        return u;
                    })
                    .orElseGet(() -> this.webAuthnUserRepository.save(webAuthnUserRepository.newUser(request)));


            userId = user.getId();
            name = request.getUsername();
            mode = RegistrationStartResponse.Mode.NEW;
        } else if (request.getRegistrationAddToken() != null && !request.getRegistrationAddToken().isEmpty()) {
            byte[] registrationAddTokenDecoded = null;
            try {
                registrationAddTokenDecoded = Base64.getDecoder().decode(request.getRegistrationAddToken());
            } catch (Exception e) {
                throw new InvalidTokenException("Registration Add Token invalid");
            }

            WebAuthnUser user = webAuthnUserRepository.findByAddTokenAndRegistrationAddStartAfter(
                            registrationAddTokenDecoded, LocalDateTime.now().minusMinutes(10))
                    .orElseThrow(() -> new InvalidTokenException("Registration Add Token expired"));


            userId = user.getId();
            name = user.getUsername();
            mode = RegistrationStartResponse.Mode.ADD;
        } else if (request.getRecoveryToken() != null && !request.getRecoveryToken().isEmpty()) {
            byte[] recoveryTokenDecoded = null;
            try {
                recoveryTokenDecoded = Base64.getDecoder().decode(request.getRecoveryToken());
            } catch (Exception e) {
                throw new InvalidTokenException("One of the fields username, registrationAddToken, recoveryToken should be added");
            }
            WebAuthnUser user = webAuthnUserRepository.findByRecoveryToken(recoveryTokenDecoded)
                    .orElseThrow(() -> new InvalidTokenException("Recovery token not found"));

            userId = user.getId();
            name = user.getUsername();
            mode = RegistrationStartResponse.Mode.RECOVERY;
            webAuthnCredentialRepository.deleteByAppUserId(userId);
        } else {
            new InvalidTokenException("Recovery token not found");
        }

        if (mode != null) {
            PublicKeyCredentialCreationOptions credentialCreation = this.relyingParty
                    .startRegistration(StartRegistrationOptions.builder()
                            .user(UserIdentity
                                    .builder()
                                    .name(name)
                                    .displayName(name)
                                    .id(new ByteArray(BytesUtil.longToBytes(userId))).build())
                            .build());

            byte[] registrationId = new byte[16];
            this.random.nextBytes(registrationId);
            RegistrationStartResponse startResponse = new RegistrationStartResponse(mode,
                    Base64.getEncoder().encodeToString(registrationId), credentialCreation);

            registrationOperation.put(startResponse.getRegistrationId(), startResponse);

            return startResponse;
        }
        return null;
    }
}
