package com.petmatch.community.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.petmatch.community.model.User;
import com.petmatch.community.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.test.context.ContextConfiguration(initializers=com.petmatch.community.TestDatabaseSafety.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WebRegressionTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;

    @Test
    void sessionLoginAndCsrfRemainEnabled() throws Exception {
        users.saveAndFlush(new User("Session", "session@example.test", "{noop}testing123"));
        var login =
                mvc.perform(
                                post("/login")
                                        .with(csrf())
                                        .param("email", "session@example.test")
                                        .param("password", "testing123"))
                        .andExpect(status().is3xxRedirection())
                        .andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(session);
        mvc.perform(get("/pets").session(session)).andExpect(status().isOk());
        mvc.perform(
                        post("/pets")
                                .session(session)
                                .param("name", "Luna")
                                .param("species", "Perro")
                                .param("age", "2"))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/pets")
                                .session(session)
                                .with(csrf())
                                .param("name", "")
                                .param("species", "Perro")
                                .param("age", "-1"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("petForm", "name", "age"))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "aria-invalid=\"true\"")));
    }

    @Test
    void deactivatedAccountCannotContinueUsingExistingWebSession() throws Exception {
        var account = users.saveAndFlush(
                new User("Disabled session", "disabled-session@example.test", "{noop}testing123"));

        var login = mvc.perform(post("/login").with(csrf())
                        .param("email", account.getEmail())
                        .param("password", "testing123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(session);
        mvc.perform(get("/pets").session(session)).andExpect(status().isOk());

        account.setActive(false);
        users.flush();
        mvc.perform(get("/pets").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/support-requests/mine").session(session))
                .andExpect(status().isForbidden());
    }

}
