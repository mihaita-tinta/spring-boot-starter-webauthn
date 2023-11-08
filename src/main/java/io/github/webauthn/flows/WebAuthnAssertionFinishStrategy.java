package io.github.webauthn.flows;

import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.exception.AssertionFailedException;
import io.github.webauthn.BytesUtil;
import io.github.webauthn.config.WebAuthnOperation;
import io.github.webauthn.domain.*;
import io.github.webauthn.dto.AssertionFinishRequest;
import io.github.webauthn.dto.AssertionStartResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static io.github.webauthn.flows.WebAuthnAssertionFinishStrategy.AssertionSuccessResponse.of;

public class WebAuthnAssertionFinishStrategy {
    private static final Logger log = LoggerFactory.getLogger(WebAuthnAssertionFinishStrategy.class);
    private final WebAuthnUserRepository<WebAuthnUser> webAuthnUserRepository;
    private final WebAuthnCredentialsRepository<WebAuthnCredentials> webAuthnCredentialsRepository;
    private final RelyingParty relyingParty;
    private final WebAuthnOperation<AssertionStartResponse, String> operation;

    public WebAuthnAssertionFinishStrategy(WebAuthnUserRepository webAuthnUserRepository, WebAuthnCredentialsRepository webAuthnCredentialsRepository, RelyingParty relyingParty, WebAuthnOperation<AssertionStartResponse, String> operation) {
        this.webAuthnUserRepository = webAuthnUserRepository;
        this.webAuthnCredentialsRepository = webAuthnCredentialsRepository;
        this.relyingParty = relyingParty;
        this.operation = operation;
    }

    public Optional<AssertionSuccessResponse> finish(AssertionFinishRequest finishRequest) {
        log.debug("finish - {}", finishRequest);

        AssertionStartResponse startResponse = this.operation
                .get(finishRequest.getAssertionId());
        this.operation.remove(finishRequest.getAssertionId());

        if (startResponse == null) {
            throw new IllegalStateException("call start before this");

        }
        try {
            AssertionResult result = this.relyingParty.finishAssertion(
                    FinishAssertionOptions.builder().request(startResponse.getAssertionRequest())
                            .response(finishRequest.getCredential()).build());

            if (result.isSuccess()) {

                log.info("finish - user: " + result.getUserHandle());

                long appUserId = BytesUtil.bytesToLong(result.getUserHandle().getBytes());
                byte[] credentialId = result.getCredentialId().getBytes();

                WebAuthnCredentials webAuthnCredentials = webAuthnCredentialsRepository.findByCredentialIdAndAppUserId(credentialId, appUserId)
                        .map(credential -> {
                            credential.setCount(result.getSignatureCount());
                            return webAuthnCredentialsRepository.save(credential);
                        })
                        .orElseThrow();


                long userId = BytesUtil.bytesToLong(result.getUserHandle().getBytes());
                return this.webAuthnUserRepository.findById(userId)
                        .map(u -> of(u, webAuthnCredentials));
            }
        } catch (AssertionFailedException e) {
            log.warn("finish - failed with error: {}", e);
            throw new WebAuthnAssertionFailedException(e);
        }


        return Optional.empty();
    }


    public record AssertionSuccessResponse(WebAuthnUser user, WebAuthnCredentials credentials) {

        public static AssertionSuccessResponse of(WebAuthnUser user, WebAuthnCredentials credentials) {
            AssertionSuccessResponse res = new AssertionSuccessResponse(user, credentials);
            return res;
        }

        public <T extends WebAuthnUser> T getUser() {
            return (T) user;
        }
    }
}
