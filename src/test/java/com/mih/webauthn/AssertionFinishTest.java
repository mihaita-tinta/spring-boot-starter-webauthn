package com.mih.webauthn;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.mih.webauthn.config.WebAuthnOperation;
import com.mih.webauthn.domain.*;
import com.mih.webauthn.dto.AssertionStartResponse;
import com.yubico.webauthn.AssertionRequest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = {
                "webauthn.relyingPartyId=localhost",
                "webauthn.relyingPartyName=localhost",
                "webauthn.relyingPartyOrigins=http://localhost:8080"
        })
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Transactional
public class AssertionFinishTest {

    @Autowired
    ObjectMapper mapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    WebAuthnUserRepository webAuthnUserRepository;
    @Autowired
    WebAuthnCredentialsRepository credentialsRepository;

    @MockBean
    WebAuthnOperation assertionOperation;

    @Test
    public void testFinish() throws Exception {

        WebAuthnUser user = new WebAuthnUser();
        user.setUsername("junit");
        user = webAuthnUserRepository.save(user);

        WebAuthnCredentials credentials = new WebAuthnCredentials();
        credentials.setAppUserId(user.getId());
        credentials.setCredentialId(Base64.getDecoder().decode("ARgxyHfw5N83gRMl2M7vHhqkQmtHwDJ8QCciM4uWlyGivpTf00b8TIvy6BEpBAZVCA9J5w"));
        credentials.setPublicKeyCose(Base64.getDecoder().decode("pQECAyYgASFYIEayvcdalRrrCPEidpoYbZdHmNsDeIyYBoVJ6HnwmUq4IlggV4V9TNhyHSGQxDTr4+TUWWP60edcpQlybrwOlIrxacU="));
        credentials.setCount(1L);
        credentialsRepository.save(credentials);

        AssertionRequest assertionRequest = mapper.readValue("{\"assertionId\":\"bWnC7+6A/fUcwjl048iPOQ==\",\"publicKeyCredentialRequestOptions\":{\"challenge\":\"UeBYkJu4cvNqx6FFi4qSIL8KIDox0pqyMS9W6bAbTH8\",\"rpId\":\"localhost\",\"allowCredentials\":[{\"type\":\"public-key\",\"id\":\"ARgxyHfw5N83gRMl2M7vHhqkQmtHwDJ8QCciM4uWlyGivpTf00b8TIvy6BEpBAZVCA9J5w\"}],\"userVerification\":\"preferred\",\"extensions\":{}}}", AssertionRequest.class);
//        AssertionRequest assertionRequest = mapper.readValue("{\"assertionId\":\"mu3Btl2cyN/kOJYuJm3bvw==\",\"publicKeyCredentialRequestOptions\":{\"challenge\":\"UM0W1FmvQ0z2ijaGKxzobOU-NlWAaT9TW6rTIqLUXgk\",\"rpId\":\"localhost\",\"allowCredentials\":[{\"type\":\"public-key\",\"id\":\"AandphtQ5RDYYS3CkUfOLhBa2AYBVYx-oi3sd-4FdendRLYRa7lK-JEBcg7OtDTwZuh0fw\"}],\"userVerification\":\"preferred\",\"extensions\":{}}}", AssertionRequest.class);
        AssertionStartResponse startResponse = new AssertionStartResponse("obumqZhCl7CBKxpRjyMePA==", assertionRequest);
//        AssertionStartResponse startResponse = new AssertionStartResponse("BvrOasdbq3ZZTCJroVmMXw==", assertionRequest);
        when(assertionOperation.get(anyString())).thenReturn(startResponse);

        this.mockMvc.perform(
                post("/assertion/finish")
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{\n" +
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
                                "}\n")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document("assertion-finish"));
    }

}
