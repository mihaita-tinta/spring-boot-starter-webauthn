package io.github.webauthn.flows;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.RelyingParty;
import io.github.webauthn.BytesUtil;
import io.github.webauthn.WebAuthnInMemoryAutoConfiguration;
import io.github.webauthn.config.WebAuthnOperation;
import io.github.webauthn.domain.DefaultWebAuthnCredentials;
import io.github.webauthn.domain.DefaultWebAuthnUser;
import io.github.webauthn.domain.WebAuthnCredentialsRepository;
import io.github.webauthn.domain.WebAuthnUserRepository;
import io.github.webauthn.events.NewDeviceAddedEvent;
import io.github.webauthn.events.NewRequestToAddDeviceEvent;
import io.github.webauthn.events.WebAuthnEventPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = {
                "webauthn.relyingPartyId=localhost",
                "webauthn.relyingPartyName=localhost",
                "webauthn.relyingPartyOrigins=http://localhost:8080"
        })
@Import(WebAuthnInMemoryAutoConfiguration.class)
@AutoConfigureMockMvc
@AutoConfigureRestDocs
public class WebAuthnRegistrationAddStrategyTest {

    @Autowired
    ObjectMapper mapper;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    WebAuthnOperation registrationOperation;

    @MockBean
    WebAuthnCredentialsRepository credentialsRepository;

    @Autowired
    WebAuthnUserRepository webAuthnUserRepository;

    @Autowired
    RelyingParty relyingParty;
    @MockBean
    WebAuthnEventPublisher eventPublisher;

    @Test
    @WithMockUser("junit")
    public void testAdd() throws Exception {

        DefaultWebAuthnUser user = new DefaultWebAuthnUser();
        user.setUsername("junit");
        webAuthnUserRepository.save(user);

        DefaultWebAuthnCredentials credentials = new DefaultWebAuthnCredentials();
        credentials.setAppUserId(user.getId());
        credentials.setCredentialId(BytesUtil.longToBytes(123L));
        credentialsRepository.save(credentials);

        this.mockMvc.perform(
                        get("/registration/add")
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationAddToken").exists())
                .andDo(document("registration-add"));

        verify(eventPublisher).publishEvent(any(NewRequestToAddDeviceEvent.class));
    }

}
