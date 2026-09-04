package com.petmatch.community.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.petmatch.community.dto.auth.RegistrationForm;
import com.petmatch.community.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RestApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    private String email;
    private final String password = "testing123";

    @BeforeEach
    void registerApiUser() {
        email = "api-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        RegistrationForm form = new RegistrationForm();
        form.setName("API User");
        form.setEmail(email);
        form.setPassword(password);
        form.setConfirmPassword(password);
        userService.register(form);
    }

    @Test
    void webAndApiSecurityCoexist() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/pets"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/pets").with(httpBasic(email, password)))
            .andExpect(status().isOk());
    }

    @Test
    void authenticatedUserCanCreatePetThroughApiWithoutCsrfToken() throws Exception {
        String body = """
            {
              "name": "Luna",
              "species": "Perro",
              "age": 4,
              "description": "Creada desde la API REST"
            }
            """;

        mockMvc.perform(post("/api/v1/pets")
                .with(httpBasic(email, password))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.name").value("Luna"))
            .andExpect(jsonPath("$.species").value("Perro"));
    }

    @Test
    void invalidApiRequestReturnsProblemDetail() throws Exception {
        String body = """
            {
              "name": "",
              "species": "Perro",
              "age": -1
            }
            """;

        mockMvc.perform(post("/api/v1/pets")
                .with(httpBasic(email, password))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation failed"))
            .andExpect(jsonPath("$.errors.name").exists())
            .andExpect(jsonPath("$.errors.age").exists());
    }
}
