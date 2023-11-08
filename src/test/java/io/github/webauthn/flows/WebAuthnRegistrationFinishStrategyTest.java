package io.github.webauthn.flows;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import io.github.webauthn.BytesUtil;
import io.github.webauthn.WebAuthnInMemoryAutoConfiguration;
import io.github.webauthn.config.WebAuthnOperation;
import io.github.webauthn.domain.DefaultWebAuthnCredentials;
import io.github.webauthn.domain.DefaultWebAuthnUser;
import io.github.webauthn.domain.WebAuthnCredentialsRepository;
import io.github.webauthn.domain.WebAuthnUserRepository;
import io.github.webauthn.dto.RegistrationStartResponse;
import io.github.webauthn.events.NewDeviceAddedEvent;
import io.github.webauthn.events.NewRecoveryTokenCreated;
import io.github.webauthn.events.NewRequestToAddDeviceEvent;
import io.github.webauthn.events.NewUserCreatedEvent;
import io.github.webauthn.events.UserMigratedEvent;
import io.github.webauthn.events.WebAuthnEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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
public class WebAuthnRegistrationFinishStrategyTest {

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

    @MockBean
    WebAuthnEventPublisher publisher;

    @Autowired
    RelyingParty relyingParty;

    @Test
    public void testNewUserFinish() throws Exception {

        DefaultWebAuthnUser user = new DefaultWebAuthnUser();
        user.setUsername("junit");
        webAuthnUserRepository.save(user);

        DefaultWebAuthnCredentials credentials = new DefaultWebAuthnCredentials();
        credentials.setAppUserId(user.getId());
        credentials.setCredentialId(BytesUtil.longToBytes(123L));
        credentialsRepository.save(credentials);

        PublicKeyCredentialCreationOptions credentialCreationOptions = mapper.readValue(
                "{\n" +
                        "    \"rp\": {\n" +
                        "      \"name\": \"Example Application\",\n" +
                        "      \"id\": \"localhost\",\n" +
                        "      \"icon\": \"http://localhost:8100/assets/logo.png\"\n" +
                        "    },\n" +
                        "    \"user\": {\n" +
                        "      \"name\": \"junit\",\n" +
                        "      \"displayName\": \"junit\",\n" +
                        "      \"id\": \"AAAAAAAAAAE\"\n" +
                        "    },\n" +
                        "    \"challenge\": \"u6oTRjH9ivNGVtNDdJgeSab-XsblKzLl5TtJi2ZRjB8\",\n" +
                        "    \"pubKeyCredParams\": [\n" +
                        "      {\n" +
                        "        \"alg\": -7,\n" +
                        "        \"type\": \"public-key\"\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"alg\": -8,\n" +
                        "        \"type\": \"public-key\"\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"alg\": -257,\n" +
                        "        \"type\": \"public-key\"\n" +
                        "      }\n" +
                        "    ],\n" +
                        "    \"excludeCredentials\": [],\n" +
                        "    \"attestation\": \"none\",\n" +
                        "    \"extensions\": {}\n" +
                        "  }", PublicKeyCredentialCreationOptions.class);
        RegistrationStartResponse startResponse = new RegistrationStartResponse(RegistrationStartResponse.Mode.NEW,
                "KukKik86leDlveDwJvGZVA==", credentialCreationOptions);
        when(registrationOperation.get(anyString())).thenReturn(startResponse);


        this.mockMvc.perform(
                        post("/registration/finish")
                                .accept(MediaType.APPLICATION_JSON)
                                .content("{\n" +
                                        "  \"registrationId\": \"KukKik86leDlveDwJvGZVA==\",\n" +
                                        "  \"credential\": {\n" +
                                        "    \"type\": \"public-key\",\n" +
                                        "    \"id\": \"ARgxyHfw5N83gRMl2M7vHhqkQmtHwDJ8QCciM4uWlyGivpTf00b8TIvy6BEpBAZVCA9J5w\",\n" +
                                        "    \"rawId\": \"ARgxyHfw5N83gRMl2M7vHhqkQmtHwDJ8QCciM4uWlyGivpTf00b8TIvy6BEpBAZVCA9J5w\",\n" +
                                        "    \"response\": {\n" +
                                        "      \"clientDataJSON\": \"eyJ0eXBlIjoid2ViYXV0aG4uY3JlYXRlIiwiY2hhbGxlbmdlIjoidTZvVFJqSDlpdk5HVnRORGRKZ2VTYWItWHNibEt6TGw1VHRKaTJaUmpCOCIsIm9yaWdpbiI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MCIsImNyb3NzT3JpZ2luIjpmYWxzZX0\",\n" +
                                        "      \"attestationObject\": \"o2NmbXRkbm9uZWdhdHRTdG10oGhhdXRoRGF0YVi4SZYN5YgOjGh0NBcPZHZgW4_krrmihjLHmVzzuoMdl2NFYQFsmK3OAAI1vMYKZIsLJfHwVQMANAEYMch38OTfN4ETJdjO7x4apEJrR8AyfEAnIjOLlpchor6U39NG_EyL8ugRKQQGVQgPSeelAQIDJiABIVggRrK9x1qVGusI8SJ2mhhtl0eY2wN4jJgGhUnoefCZSrgiWCBXhX1M2HIdIZDENOvj5NRZY_rR51ylCXJuvA6UivFpxQ\"\n" +
                                        "    },\n" +
                                        "    \"clientExtensionResults\": {}\n" +
                                        "  }\n" +
                                        "}")
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("registration-finish-new-user"));

        verify(publisher).publishEvent(any(NewUserCreatedEvent.class));
    }
    @Test
    public void testAddDeviceFinish() throws Exception {

        DefaultWebAuthnUser user = new DefaultWebAuthnUser();
        user.setUsername("junit");
        user.setAddToken("test".getBytes());
        user.setRegistrationAddStart(LocalDateTime.now().minusSeconds(10));
        webAuthnUserRepository.save(user);

        DefaultWebAuthnCredentials credentials = new DefaultWebAuthnCredentials();
        credentials.setAppUserId(user.getId());
        credentials.setCredentialId(BytesUtil.longToBytes(123L));
        credentialsRepository.save(credentials);

        PublicKeyCredentialCreationOptions credentialCreationOptions = mapper.readValue(
                "{\n" +
                        "    \"rp\": {\n" +
                        "      \"name\": \"Example Application\",\n" +
                        "      \"id\": \"localhost\",\n" +
                        "      \"icon\": \"http://localhost:8100/assets/logo.png\"\n" +
                        "    },\n" +
                        "    \"user\": {\n" +
                        "      \"name\": \"junit\",\n" +
                        "      \"displayName\": \"junit\",\n" +
                        "      \"id\": \"AAAAAAAAAAE\"\n" +
                        "    },\n" +
                        "    \"challenge\": \"u6oTRjH9ivNGVtNDdJgeSab-XsblKzLl5TtJi2ZRjB8\",\n" +
                        "    \"pubKeyCredParams\": [\n" +
                        "      {\n" +
                        "        \"alg\": -7,\n" +
                        "        \"type\": \"public-key\"\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"alg\": -8,\n" +
                        "        \"type\": \"public-key\"\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"alg\": -257,\n" +
                        "        \"type\": \"public-key\"\n" +
                        "      }\n" +
                        "    ],\n" +
                        "    \"excludeCredentials\": [],\n" +
                        "    \"attestation\": \"none\",\n" +
                        "    \"extensions\": {}\n" +
                        "  }", PublicKeyCredentialCreationOptions.class);
        RegistrationStartResponse startResponse = new RegistrationStartResponse(RegistrationStartResponse.Mode.ADD,
                "KukKik86leDlveDwJvGZVA==", credentialCreationOptions);
        when(registrationOperation.get(anyString())).thenReturn(startResponse);


        this.mockMvc.perform(
                        post("/registration/finish")
                                .accept(MediaType.APPLICATION_JSON)
                                .content("{\n" +
                                        "  \"registrationId\": \"KukKik86leDlveDwJvGZVA==\",\n" +
                                        "  \"credential\": {\n" +
                                        "    \"type\": \"public-key\",\n" +
                                        "    \"id\": \"ARgxyHfw5N83gRMl2M7vHhqkQmtHwDJ8QCciM4uWlyGivpTf00b8TIvy6BEpBAZVCA9J5w\",\n" +
                                        "    \"rawId\": \"ARgxyHfw5N83gRMl2M7vHhqkQmtHwDJ8QCciM4uWlyGivpTf00b8TIvy6BEpBAZVCA9J5w\",\n" +
                                        "    \"response\": {\n" +
                                        "      \"clientDataJSON\": \"eyJ0eXBlIjoid2ViYXV0aG4uY3JlYXRlIiwiY2hhbGxlbmdlIjoidTZvVFJqSDlpdk5HVnRORGRKZ2VTYWItWHNibEt6TGw1VHRKaTJaUmpCOCIsIm9yaWdpbiI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MCIsImNyb3NzT3JpZ2luIjpmYWxzZX0\",\n" +
                                        "      \"attestationObject\": \"o2NmbXRkbm9uZWdhdHRTdG10oGhhdXRoRGF0YVi4SZYN5YgOjGh0NBcPZHZgW4_krrmihjLHmVzzuoMdl2NFYQFsmK3OAAI1vMYKZIsLJfHwVQMANAEYMch38OTfN4ETJdjO7x4apEJrR8AyfEAnIjOLlpchor6U39NG_EyL8ugRKQQGVQgPSeelAQIDJiABIVggRrK9x1qVGusI8SJ2mhhtl0eY2wN4jJgGhUnoefCZSrgiWCBXhX1M2HIdIZDENOvj5NRZY_rR51ylCXJuvA6UivFpxQ\"\n" +
                                        "    },\n" +
                                        "    \"clientExtensionResults\": {}\n" +
                                        "  }\n" +
                                        "}")
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("registration-finish-new-device"));

        verify(publisher).publishEvent(any(NewDeviceAddedEvent.class));
    }

    @Test
    public void testUserMigratedFinish() throws Exception {

        DefaultWebAuthnUser user = new DefaultWebAuthnUser();
        user.setUsername("junit");
        user.setAddToken("test".getBytes());
        user.setRegistrationAddStart(LocalDateTime.now().minusSeconds(10));
        webAuthnUserRepository.save(user);

        DefaultWebAuthnCredentials credentials = new DefaultWebAuthnCredentials();
        credentials.setAppUserId(user.getId());
        credentials.setCredentialId(BytesUtil.longToBytes(123L));
        credentialsRepository.save(credentials);

        PublicKeyCredentialCreationOptions credentialCreationOptions = mapper.readValue(
                "{\n" +
                        "    \"rp\": {\n" +
                        "      \"name\": \"Example Application\",\n" +
                        "      \"id\": \"localhost\",\n" +
                        "      \"icon\": \"http://localhost:8100/assets/logo.png\"\n" +
                        "    },\n" +
                        "    \"user\": {\n" +
                        "      \"name\": \"junit\",\n" +
                        "      \"displayName\": \"junit\",\n" +
                        "      \"id\": \"AAAAAAAAAAE\"\n" +
                        "    },\n" +
                        "    \"challenge\": \"u6oTRjH9ivNGVtNDdJgeSab-XsblKzLl5TtJi2ZRjB8\",\n" +
                        "    \"pubKeyCredParams\": [\n" +
                        "      {\n" +
                        "        \"alg\": -7,\n" +
                        "        \"type\": \"public-key\"\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"alg\": -8,\n" +
                        "        \"type\": \"public-key\"\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"alg\": -257,\n" +
                        "        \"type\": \"public-key\"\n" +
                        "      }\n" +
                        "    ],\n" +
                        "    \"excludeCredentials\": [],\n" +
                        "    \"attestation\": \"none\",\n" +
                        "    \"extensions\": {}\n" +
                        "  }", PublicKeyCredentialCreationOptions.class);
        RegistrationStartResponse startResponse = new RegistrationStartResponse(RegistrationStartResponse.Mode.MIGRATE,
                "KukKik86leDlveDwJvGZVA==", credentialCreationOptions);
        when(registrationOperation.get(anyString())).thenReturn(startResponse);


        this.mockMvc.perform(
                        post("/registration/finish")
                                .accept(MediaType.APPLICATION_JSON)
                                .content("{\n" +
                                        "  \"registrationId\": \"KukKik86leDlveDwJvGZVA==\",\n" +
                                        "  \"credential\": {\n" +
                                        "    \"type\": \"public-key\",\n" +
                                        "    \"id\": \"ARgxyHfw5N83gRMl2M7vHhqkQmtHwDJ8QCciM4uWlyGivpTf00b8TIvy6BEpBAZVCA9J5w\",\n" +
                                        "    \"rawId\": \"ARgxyHfw5N83gRMl2M7vHhqkQmtHwDJ8QCciM4uWlyGivpTf00b8TIvy6BEpBAZVCA9J5w\",\n" +
                                        "    \"response\": {\n" +
                                        "      \"clientDataJSON\": \"eyJ0eXBlIjoid2ViYXV0aG4uY3JlYXRlIiwiY2hhbGxlbmdlIjoidTZvVFJqSDlpdk5HVnRORGRKZ2VTYWItWHNibEt6TGw1VHRKaTJaUmpCOCIsIm9yaWdpbiI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MCIsImNyb3NzT3JpZ2luIjpmYWxzZX0\",\n" +
                                        "      \"attestationObject\": \"o2NmbXRkbm9uZWdhdHRTdG10oGhhdXRoRGF0YVi4SZYN5YgOjGh0NBcPZHZgW4_krrmihjLHmVzzuoMdl2NFYQFsmK3OAAI1vMYKZIsLJfHwVQMANAEYMch38OTfN4ETJdjO7x4apEJrR8AyfEAnIjOLlpchor6U39NG_EyL8ugRKQQGVQgPSeelAQIDJiABIVggRrK9x1qVGusI8SJ2mhhtl0eY2wN4jJgGhUnoefCZSrgiWCBXhX1M2HIdIZDENOvj5NRZY_rR51ylCXJuvA6UivFpxQ\"\n" +
                                        "    },\n" +
                                        "    \"clientExtensionResults\": {}\n" +
                                        "  }\n" +
                                        "}")
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("registration-finish-migrate-user"));

        verify(publisher).publishEvent(any(UserMigratedEvent.class));
    }

    @Test
    public void testNewRecoveryTokenCreatedFinish() throws Exception {

        DefaultWebAuthnUser user = new DefaultWebAuthnUser();
        user.setUsername("junit");
        user.setAddToken("test".getBytes());
        user.setRegistrationAddStart(LocalDateTime.now().minusSeconds(10));
        webAuthnUserRepository.save(user);

        DefaultWebAuthnCredentials credentials = new DefaultWebAuthnCredentials();
        credentials.setAppUserId(user.getId());
        credentials.setCredentialId(BytesUtil.longToBytes(123L));
        credentialsRepository.save(credentials);

        PublicKeyCredentialCreationOptions credentialCreationOptions = mapper.readValue(
                "{\n" +
                        "    \"rp\": {\n" +
                        "      \"name\": \"Example Application\",\n" +
                        "      \"id\": \"localhost\",\n" +
                        "      \"icon\": \"http://localhost:8100/assets/logo.png\"\n" +
                        "    },\n" +
                        "    \"user\": {\n" +
                        "      \"name\": \"junit\",\n" +
                        "      \"displayName\": \"junit\",\n" +
                        "      \"id\": \"AAAAAAAAAAE\"\n" +
                        "    },\n" +
                        "    \"challenge\": \"u6oTRjH9ivNGVtNDdJgeSab-XsblKzLl5TtJi2ZRjB8\",\n" +
                        "    \"pubKeyCredParams\": [\n" +
                        "      {\n" +
                        "        \"alg\": -7,\n" +
                        "        \"type\": \"public-key\"\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"alg\": -8,\n" +
                        "        \"type\": \"public-key\"\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"alg\": -257,\n" +
                        "        \"type\": \"public-key\"\n" +
                        "      }\n" +
                        "    ],\n" +
                        "    \"excludeCredentials\": [],\n" +
                        "    \"attestation\": \"none\",\n" +
                        "    \"extensions\": {}\n" +
                        "  }", PublicKeyCredentialCreationOptions.class);
        RegistrationStartResponse startResponse = new RegistrationStartResponse(RegistrationStartResponse.Mode.RECOVERY,
                "KukKik86leDlveDwJvGZVA==", credentialCreationOptions);
        when(registrationOperation.get(anyString())).thenReturn(startResponse);


        this.mockMvc.perform(
                        post("/registration/finish")
                                .accept(MediaType.APPLICATION_JSON)
                                .content("{\n" +
                                        "  \"registrationId\": \"KukKik86leDlveDwJvGZVA==\",\n" +
                                        "  \"credential\": {\n" +
                                        "    \"type\": \"public-key\",\n" +
                                        "    \"id\": \"ARgxyHfw5N83gRMl2M7vHhqkQmtHwDJ8QCciM4uWlyGivpTf00b8TIvy6BEpBAZVCA9J5w\",\n" +
                                        "    \"rawId\": \"ARgxyHfw5N83gRMl2M7vHhqkQmtHwDJ8QCciM4uWlyGivpTf00b8TIvy6BEpBAZVCA9J5w\",\n" +
                                        "    \"response\": {\n" +
                                        "      \"clientDataJSON\": \"eyJ0eXBlIjoid2ViYXV0aG4uY3JlYXRlIiwiY2hhbGxlbmdlIjoidTZvVFJqSDlpdk5HVnRORGRKZ2VTYWItWHNibEt6TGw1VHRKaTJaUmpCOCIsIm9yaWdpbiI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MCIsImNyb3NzT3JpZ2luIjpmYWxzZX0\",\n" +
                                        "      \"attestationObject\": \"o2NmbXRkbm9uZWdhdHRTdG10oGhhdXRoRGF0YVi4SZYN5YgOjGh0NBcPZHZgW4_krrmihjLHmVzzuoMdl2NFYQFsmK3OAAI1vMYKZIsLJfHwVQMANAEYMch38OTfN4ETJdjO7x4apEJrR8AyfEAnIjOLlpchor6U39NG_EyL8ugRKQQGVQgPSeelAQIDJiABIVggRrK9x1qVGusI8SJ2mhhtl0eY2wN4jJgGhUnoefCZSrgiWCBXhX1M2HIdIZDENOvj5NRZY_rR51ylCXJuvA6UivFpxQ\"\n" +
                                        "    },\n" +
                                        "    \"clientExtensionResults\": {}\n" +
                                        "  }\n" +
                                        "}")
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("registration-finish-recovery-token-user"));

        verify(publisher).publishEvent(any(NewRecoveryTokenCreated.class));
    }

}

