package com.petmatch.community.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.test.context.ContextConfiguration(initializers=com.petmatch.community.TestDatabaseSafety.class)
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

    @Test
    void ownerCanReadUpdateAndDeletePetThroughRest() throws Exception {
        long id = createPetThroughApi();
        String updated = """
            {"name":"Luna actualizada","species":"Perro","age":5,"description":"Actualizada"}
            """;

        mockMvc.perform(put("/api/v1/pets/{id}", id)
                        .with(httpBasic(email, password))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updated))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Luna actualizada"))
                .andExpect(jsonPath("$.age").value(5));

        mockMvc.perform(get("/api/v1/pets/{id}", id).with(httpBasic(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Luna actualizada"));

        mockMvc.perform(delete("/api/v1/pets/{id}", id).with(httpBasic(email, password)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/pets/{id}", id).with(httpBasic(email, password)))
                .andExpect(status().isNotFound());
    }

    @Test
    void secondUserCannotReadUpdateOrDeleteAnotherUsersPet() throws Exception {
        long id = createPetThroughApi();
        String outsider = "rest-outsider-" + UUID.randomUUID() + "@example.test";
        registerUser(outsider);
        String updated = """
            {"name":"Intrusa","species":"Perro","age":1}
            """;
        mockMvc.perform(get("/api/v1/pets/{id}", id).with(httpBasic(outsider, password)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/pets/{id}", id)
                        .with(httpBasic(outsider, password))
                        .contentType(MediaType.APPLICATION_JSON).content(updated))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/pets/{id}", id)
                        .with(httpBasic(outsider, password)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/pets/{id}", id).with(httpBasic(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Luna"));
    }

    @Test
    void malformedJsonIsRejectedAndMissingPetHasNotFoundProblem() throws Exception {
        mockMvc.perform(post("/api/v1/pets")
                        .with(httpBasic(email, password))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request body"));
        mockMvc.perform(get("/api/v1/pets/{id}", 987654321L)
                        .with(httpBasic(email, password)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"));
    }

    private long createPetThroughApi() throws Exception {
        String json = """
            {"name":"Luna","species":"Perro","age":4,"description":"Mascota"}
            """;
        var created = mockMvc.perform(post("/api/v1/pets")
                        .with(httpBasic(email, password))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = com.jayway.jsonpath.JsonPath.read(
                created.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private void registerUser(String address) {
        RegistrationForm form = new RegistrationForm();
        form.setName("API Other");
        form.setEmail(address);
        form.setPassword(password);
        form.setConfirmPassword(password);
        userService.register(form);
    }
}
