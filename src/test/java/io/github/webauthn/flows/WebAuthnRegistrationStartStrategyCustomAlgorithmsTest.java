package io.github.webauthn.flows;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.webauthn.JsonConfig;
import io.github.webauthn.WebAuthnInMemoryAutoConfiguration;
import io.github.webauthn.domain.WebAuthnUserRepository;
import io.github.webauthn.dto.RegistrationStartRequest;
import io.github.webauthn.events.WebAuthnEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = {SpringMvcTestConfig.class, JsonConfig.class, WebAuthnInMemoryAutoConfiguration.class},
        properties = {
                "webauthn.relyingPartyId=localhost",
                "webauthn.relyingPartyName=localhost",
                "webauthn.registrationNewUsers.enabled=true",
                "webauthn.relyingPartyOrigins=http://localhost:8080"
        })
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@ActiveProfiles("custom-algorithms")
public class WebAuthnRegistrationStartStrategyCustomAlgorithmsTest {

    @Autowired
    ObjectMapper mapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    WebAuthnUserRepository webAuthnUserRepository;

    @MockBean
    WebAuthnEventPublisher eventPublisher;

    @Test
    public void testCustomAlgorithms() throws Exception {

        RegistrationStartRequest request = new RegistrationStartRequest();
        request.setUsername("newjunit");
        this.mockMvc.perform(
                        post("/registration/start")
                                .accept(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicKeyCredentialCreationOptions.pubKeyCredParams.length()").value(1))
                .andExpect(jsonPath("$.publicKeyCredentialCreationOptions.pubKeyCredParams.[0].alg").value(-7))
                .andExpect(jsonPath("$.publicKeyCredentialCreationOptions.pubKeyCredParams.[0].type").value("public-key"))
                .andDo(document("registration-start-custom-algorithms"));
    }

}
