package io.github.webauthn.webflux;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import io.github.webauthn.BytesUtil;
import io.github.webauthn.JsonConfig;
import io.github.webauthn.WebAuthnInMemoryAutoConfiguration;
import io.github.webauthn.config.WebAuthnOperation;
import io.github.webauthn.domain.DefaultWebAuthnCredentials;
import io.github.webauthn.domain.DefaultWebAuthnUser;
import io.github.webauthn.domain.WebAuthnCredentialsRepository;
import io.github.webauthn.domain.WebAuthnUser;
import io.github.webauthn.domain.WebAuthnUserRepository;
import io.github.webauthn.dto.AssertionStartRequest;
import io.github.webauthn.dto.AssertionStartResponse;
import io.github.webauthn.dto.RegistrationFinishRequest;
import io.github.webauthn.dto.RegistrationStartRequest;
import io.github.webauthn.dto.RegistrationStartResponse;
import io.github.webauthn.events.NewRecoveryTokenCreated;
import io.github.webauthn.events.WebAuthnEventPublisher;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {
        SpringWebFluxTestConfig.class, JsonConfig.class, WebAuthnWebFluxConfig.class, WebAuthnInMemoryAutoConfiguration.class
},
        properties = {
                "webauthn.relyingPartyId=localhost",
                "webauthn.relyingPartyName=localhost",
                "webauthn.registrationNewUsers.enabled=true",
                "webauthn.relyingPartyOrigins=http://localhost:8080",
                "spring.main.web-application-type=reactive"})
@AutoConfigureWebTestClient
class WebAuthnWebFilterTest {
    private static final Logger log = LoggerFactory.getLogger(WebAuthnWebFluxConfigTest.class);

    @Autowired
    private WebTestClient client;

    @Autowired
    WebAuthnUserRepository webAuthnUserRepository;
    @Autowired
    WebAuthnCredentialsRepository credentialsRepository;

    @Autowired
    ObjectMapper mapper;

    @MockBean
    WebAuthnOperation assertionOperation;
    @MockBean
    WebAuthnEventPublisher eventPublisher;

    @Test
    public void testUnauthorized() {

        AssertionStartRequest request = new AssertionStartRequest();
        request.setUsername("not-found");

        client
                .post()
                .uri("/assertion/start")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(request), AssertionStartRequest.class)
                .exchange()
                .expectStatus()
                .isUnauthorized();

    }

    @Test
    public void testStart() {

        DefaultWebAuthnUser user = new DefaultWebAuthnUser();
        user.setUsername("user-start");
        webAuthnUserRepository.save(user);

        DefaultWebAuthnCredentials credentials = new DefaultWebAuthnCredentials();
        credentials.setAppUserId(user.getId());
        credentials.setCredentialId(BytesUtil.longToBytes(123L));
        credentialsRepository.save(credentials);

        AssertionStartRequest request = new AssertionStartRequest();
        request.setUsername("user-start");

        client
                .post()
                .uri("/assertion/start")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(request), AssertionStartRequest.class)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .consumeWith(s -> log.info(s.toString()))
                .jsonPath("$.assertionId").exists()
                .jsonPath("$.publicKeyCredentialRequestOptions").exists();
    }

    @Test
    public void testAssertionFinish() throws JsonProcessingException {

        DefaultWebAuthnUser user = new DefaultWebAuthnUser();
        user.setUsername("junit");
        WebAuthnUser saved = webAuthnUserRepository.save(user);

        DefaultWebAuthnCredentials credentials = new DefaultWebAuthnCredentials();
        credentials.setAppUserId(saved.getId());
        credentials.setCredentialId(Base64.getDecoder().decode("ARgxyHfw5N83gRMl2M7vHhqkQmtHwDJ8QCciM4uWlyGivpTf00b8TIvy6BEpBAZVCA9J5w"));
        credentials.setPublicKeyCose(Base64.getDecoder().decode("pQECAyYgASFYIEayvcdalRrrCPEidpoYbZdHmNsDeIyYBoVJ6HnwmUq4IlggV4V9TNhyHSGQxDTr4+TUWWP60edcpQlybrwOlIrxacU="));
        credentials.setCount(1L);
        credentialsRepository.save(credentials);

        AssertionRequest assertionRequest = mapper.readValue("{\"assertionId\":\"bWnC7+6A/fUcwjl048iPOQ==\",\"publicKeyCredentialRequestOptions\":{\"challenge\":\"UeBYkJu4cvNqx6FFi4qSIL8KIDox0pqyMS9W6bAbTH8\",\"rpId\":\"localhost\",\"allowCredentials\":[{\"type\":\"public-key\",\"id\":\"ARgxyHfw5N83gRMl2M7vHhqkQmtHwDJ8QCciM4uWlyGivpTf00b8TIvy6BEpBAZVCA9J5w\"}],\"userVerification\":\"preferred\",\"extensions\":{}}}", AssertionRequest.class);
        AssertionStartResponse startResponse = new AssertionStartResponse("obumqZhCl7CBKxpRjyMePA==", assertionRequest);
        when(assertionOperation.get(anyString())).thenReturn(startResponse);

        AssertionStartRequest request = new AssertionStartRequest();
        request.setUsername("junit");

        client
                .post()
                .uri("/assertion/finish")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just("{\n" +
                        "  \"assertionId\": \"bWnC7+6A/fUcwjl048iPOQ==\",\n" +
                        "  \"credential\": {\n" +
                        "    \"type\": \"public-key\",\n" +
                        "    \"id\": \"ARgxyHfw5N83gRMl2M7vHhqkQmtHwDJ8QCciM4uWlyGivpTf00b8TIvy6BEpBAZVCA9J5w\",\n" +
                        "    \"rawId\": \"ARgxyHfw5N83gRMl2M7vHhqkQmtHwDJ8QCciM4uWlyGivpTf00b8TIvy6BEpBAZVCA9J5w\",\n" +
                        "    \"response\": {\n" +
                        "      \"clientDataJSON\": \"eyJ0eXBlIjoid2ViYXV0aG4uZ2V0IiwiY2hhbGxlbmdlIjoiVWVCWWtKdTRjdk5xeDZGRmk0cVNJTDhLSURveDBwcXlNUzlXNmJBYlRIOCIsIm9yaWdpbiI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MCIsImNyb3NzT3JpZ2luIjpmYWxzZX0\",\n" +
                        "      \"authenticatorData\": \"SZYN5YgOjGh0NBcPZHZgW4_krrmihjLHmVzzuoMdl2MFYQFsow\",\n" +
                        "      \"signature\": \"MEUCIFnff70nAto5eJTwyVHYgoi_E3013MOnbUVHJWIfaWbWAiEA9tw1WfZjTl1LOx3JF4-HQVPDhvVNVpRMXmtR2BN3m9I\",\n" +
                        "      \"userHandle\": \"AAAAAAAAAAE\"\n" +
                        "    },\n" +
                        "    \"clientExtensionResults\": {}\n" +
                        "  }\n" +
                        "}\n"), String.class)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .consumeWith(s -> log.info(s.toString()))
                .jsonPath("$.name").isEqualTo("junit");

        verify(eventPublisher).publishEvent(any(AuthenticationSuccessEvent.class));
    }

    @Test
    public void testRegistrationStart() {

        RegistrationStartRequest request = new RegistrationStartRequest();
        request.setUsername("newjunit");
        client
                .post()
                .uri("/registration/start")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(request), RegistrationStartRequest.class)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .consumeWith(s -> log.info(s.toString()));
    }

    @Test
    public void testRegistrationStartMissingFields() {

        RegistrationStartRequest request = new RegistrationStartRequest();
        client
                .post()
                .uri("/registration/start")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(request), RegistrationStartRequest.class)
                .exchange()
                .expectStatus()
                .isBadRequest();
    }


    @Test
    public void testAddDeviceInvalidToken() {

        RegistrationStartRequest request = new RegistrationStartRequest();
        request.setRegistrationAddToken("token-123");
        client
                .post()
                .uri("/registration/start")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(request), RegistrationStartRequest.class)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .consumeWith(s -> log.info(s.toString()));
    }

    @Test
    public void testAddDeviceInvalidTokenNotFound() {

        RegistrationStartRequest request = new RegistrationStartRequest();
        request.setRegistrationAddToken(Base64.getEncoder().encodeToString("token-123".getBytes()));
        client
                .post()
                .uri("/registration/start")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(request), RegistrationStartRequest.class)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .consumeWith(s -> log.info(s.toString()));
    }

    @Test
    public void testAddDevice() {
        byte[] bytes = "token-123".getBytes();
        String registrationAddToken = Base64.getEncoder().encodeToString(bytes);

        DefaultWebAuthnUser user = new DefaultWebAuthnUser();
        user.setUsername("username-add");
        user.setAddToken(bytes);
        user.setRegistrationAddStart(LocalDateTime.now().minusMinutes(1));
        webAuthnUserRepository.save(user);

        RegistrationStartRequest request = new RegistrationStartRequest();
        request.setRegistrationAddToken(registrationAddToken);
        client
                .post()
                .uri("/registration/start")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(request), RegistrationStartRequest.class)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .consumeWith(s -> log.info(s.toString()))
                .jsonPath("$.status").isEqualTo("OK")
                .jsonPath("$.registrationId").exists()
                .jsonPath("$.publicKeyCredentialCreationOptions.rp.id").isEqualTo("localhost")
                .jsonPath("$.publicKeyCredentialCreationOptions.user.name").isEqualTo(user.getUsername())
                .jsonPath("$.publicKeyCredentialCreationOptions.user.id").exists();
    }

    @Test
    public void testRecoveryToken() {
        byte[] bytes = "token-123".getBytes();
        String token = Base64.getEncoder().encodeToString(bytes);

        DefaultWebAuthnUser user = new DefaultWebAuthnUser();
        user.setUsername("user-recovery");
        user.setRecoveryToken(bytes);
        webAuthnUserRepository.save(user);

        RegistrationStartRequest request = new RegistrationStartRequest();
        request.setRecoveryToken(token);

        client
                .post()
                .uri("/registration/start")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(request), RegistrationStartRequest.class)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .consumeWith(s -> log.info(s.toString()))
                .jsonPath("$.status").isEqualTo("OK")
                .jsonPath("$.registrationId").exists()
                .jsonPath("$.publicKeyCredentialCreationOptions.rp.id").isEqualTo("localhost")
                .jsonPath("$.publicKeyCredentialCreationOptions.user.name").isEqualTo(user.getUsername())
                .jsonPath("$.publicKeyCredentialCreationOptions.user.id").exists();
    }

    @Test
    public void testRecoveryTokenInvalid() {

        RegistrationStartRequest request = new RegistrationStartRequest();
        request.setRecoveryToken(Base64.getEncoder().encodeToString("token-321".getBytes()));
        client
                .post()
                .uri("/registration/start")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(request), RegistrationStartRequest.class)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .consumeWith(s -> log.info(s.toString()));
    }

    @Test
    @WithMockUser("user-existing")
    public void testRegisterCredentialsForExistingUser() {

        DefaultWebAuthnUser user = new DefaultWebAuthnUser();
        user.setUsername("user-existing");
        webAuthnUserRepository.save(user);

        DefaultWebAuthnCredentials credentials = new DefaultWebAuthnCredentials();
        credentials.setAppUserId(user.getId());
        credentials.setCredentialId(BytesUtil.longToBytes(123L));
        credentialsRepository.save(credentials);

        RegistrationStartRequest request = new RegistrationStartRequest();
        client.mutateWith((c, d, e) -> {
                    c.responseTimeout(Duration.ofDays(1));
                })
                .post()
                .uri("/registration/start")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(request), RegistrationStartRequest.class)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .consumeWith(s -> log.info(s.toString()));
    }

    @Test
    public void testRegisterCredentialsNoInput() {

        RegistrationStartRequest request = new RegistrationStartRequest();
        client
                .post()
                .uri("/registration/start")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(request), RegistrationStartRequest.class)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .consumeWith(s -> log.info(s.toString()));
    }


    @Test
    public void testNewRecoveryTokenCreatedFinish() throws Exception {


        DefaultWebAuthnUser user = new DefaultWebAuthnUser();
        user.setUsername("junit");
        user.setAddToken("test".getBytes());
        user.setRegistrationAddStart(LocalDateTime.now().minusSeconds(10));
        webAuthnUserRepository.save(user);

        credentialsRepository.deleteById(1L);// FIXME isolate from other tests
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
        when(assertionOperation.get(anyString())).thenReturn(startResponse);

        client
                .post()
                .uri("/registration/finish")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue("{\n" +
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
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .consumeWith(s -> log.info(s.toString()));

        verify(eventPublisher).publishEvent(any(NewRecoveryTokenCreated.class));
    }

}
