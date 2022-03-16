package io.github.webauthn.domain;

import io.github.webauthn.WebAuthnConfig;
import io.github.webauthn.WebAuthnInMemoryAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(classes = {WebAuthnInMemoryAutoConfiguration.class, WebAuthnConfig.class})
class WebAuthnUserRepositoryTest {
    private static final Logger log = LoggerFactory.getLogger(WebAuthnUserRepositoryTest.class);

    @Autowired
    WebAuthnUserRepository userRepository;

    @Test
    public void test() {
        DefaultWebAuthnUser user = new DefaultWebAuthnUser();
        user.setUsername("junit");
        byte[] token = new byte[16];
        new Random().nextBytes(token);
        user.setAddToken(token);

        userRepository.save(user);

        assertNotNull(user.getId());


    }
    @Test
    public void testFind() {
        DefaultWebAuthnUser user = new DefaultWebAuthnUser();
        user.setUsername("junit");
        byte[] token = new byte[16];
        new Random().nextBytes(token);
        user.setAddToken(token);

        userRepository.save(user);

        userRepository.findByUsername("junit")
                .ifPresentOrElse(u -> log.info("found user: {}", user), () -> fail("user not found"));

        assertNotNull(user.getId());
    }

    @Test
    public void testFindByAddTokenAndRegistrationAddStartAfter() {
        DefaultWebAuthnUser user = new DefaultWebAuthnUser();
        user.setUsername("junit");
        byte[] token = new byte[16];
        new Random().nextBytes(token);
        user.setAddToken(token);
        user.setRegistrationAddStart(LocalDateTime.now());
        userRepository.save(user);

        userRepository.findByAddTokenAndRegistrationAddStartAfter(token, LocalDateTime.now().minusMinutes(10))
                .ifPresentOrElse(u -> log.info("found user: {}", user), () -> fail("user not found"));

        assertNotNull(user.getId());
    }
}
