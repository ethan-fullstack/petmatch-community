package com.petmatch.community.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.petmatch.community.model.*;
import com.petmatch.community.model.enums.*;
import com.petmatch.community.repository.*;
import com.petmatch.community.service.AdminUserService;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.test.context.ContextConfiguration(initializers=com.petmatch.community.TestDatabaseSafety.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RoleChangeRepository changes;
    @Autowired AdminUserService service;
    @Autowired EntityManager em;
    User admin, member;

    @BeforeEach
    void setup() {
        admin = users.saveAndFlush(new User("Admin", "admin@example.test", "{noop}unused"));
        admin.setRole(Role.ADMIN);
        member = users.saveAndFlush(new User("Member", "member@example.test", "{noop}unused"));
        users.flush();
    }

    @Test
    void adminScreensRenderLocalStylesAndAccessibleNavigation() throws Exception {
        mvc.perform(get("/admin/users").with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/css/application.css")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("pm-nav__links")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("pm-table")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Usuarios registrados")));
        mvc.perform(get("/js/theme.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("localStorage")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("aria-pressed")));
        mvc.perform(get("/js/navigation.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("aria-current")));
        mvc.perform(get("/css/theme.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-theme")));
        mvc.perform(get("/admin/users").with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/js/theme.js")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/css/theme.css")));
        mvc.perform(get("/css/application.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(".pm-filter-grid")));
    }

    @Test
    void anonymousCannotReadAdminPages() throws Exception {
        mvc.perform(get("/admin/users")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/reports")).andExpect(status().is3xxRedirection());
    }

    @Test
    void userCannotReadOrChangeThirdPartyRoles() throws Exception {
        mvc.perform(get("/admin/users").with(user(member.getEmail())))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin/reports").with(user(member.getEmail())))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/admin/users/{id}/role", member.getId())
                                .with(user(member.getEmail()))
                                .with(csrf())
                                .param("role", "ADMIN"))
                .andExpect(status().isForbidden());
        assertEquals(Role.USER, member.getRole());
    }

    @Test
    void adminRequiresCsrf() throws Exception {
        mvc.perform(
                        post("/admin/users/{id}/role", member.getId())
                                .with(user(admin.getEmail()).roles("ADMIN"))
                                .param("role", "ADMIN"))
                .andExpect(status().isForbidden());
    }

    @Test
    void promotionPersistsAndAuditsThenCanBeRevoked() throws Exception {
        mvc.perform(get("/admin/users").with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Rol de Member")));
        mvc.perform(
                        post("/admin/users/{id}/role", member.getId())
                                .with(user(admin.getEmail()).roles("ADMIN"))
                                .with(csrf())
                                .param("role", "ADMIN"))
                .andExpect(status().is3xxRedirection());
        em.flush();
        em.clear();
        assertEquals(Role.ADMIN, users.findById(member.getId()).orElseThrow().getRole());
        var audit = changes.findAll();
        assertEquals(1, audit.size());
        assertEquals(admin.getId(), audit.getFirst().getActorId());
        assertEquals(Role.USER, audit.getFirst().getPreviousRole());
        service.changeRole(admin.getEmail(), member.getId(), Role.USER);
        em.flush();
        em.clear();
        assertEquals(Role.USER, users.findById(member.getId()).orElseThrow().getRole());
    }

    @Test
    void lastAdminRejectionIsShownByController() throws Exception {
        mvc.perform(post("/admin/users/{id}/role", admin.getId())
                        .with(user(admin.getEmail()).roles("ADMIN")).with(csrf()).param("role", "USER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage", "No se puede degradar al último administrador activo"));
    }

    @Test
    void lastAdminCannotBeDemoted() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.changeRole(admin.getEmail(), admin.getId(), Role.USER));
        assertEquals(Role.ADMIN, admin.getRole());
        assertEquals(0, changes.count());
    }

    @Test
    void invalidAndMissingRolesAreRejected() throws Exception {
        mvc.perform(
                        post("/admin/users/{id}/role", member.getId())
                                .with(user(admin.getEmail()).roles("ADMIN"))
                                .with(csrf())
                                .param("role", "SUPERUSER"))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/admin/users/{id}/role", member.getId())
                                .with(user(admin.getEmail()).roles("ADMIN"))
                                .with(csrf()))
                .andExpect(status().isBadRequest());
        assertThrows(
                IllegalArgumentException.class,
                () -> service.changeRole(admin.getEmail(), -1L, Role.ADMIN));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.changeRole(admin.getEmail(), 999999L, Role.ADMIN));
    }

    @Test
    void staleAdminSessionCannotMutateRoles() {
        assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> service.changeRole(member.getEmail(), admin.getId(), Role.USER));
    }

    @Test
    void publicRegistrationCannotEscalateRole() throws Exception {
        mvc.perform(
                        post("/register")
                                .with(csrf())
                                .param("name", "Public")
                                .param("email", "public@example.test")
                                .param("password", "testing123")
                                .param("confirmPassword", "testing123")
                                .param("role", "ADMIN")
                                .param("active", "true"))
                .andExpect(status().is3xxRedirection());
        assertEquals(
                Role.USER,
                users.findByEmailIgnoreCase("public@example.test").orElseThrow().getRole());
    }

    @Test
    void unauthenticatedPostIsNotAnAdminOperation() throws Exception {
        mvc.perform(
                        post("/admin/users/{id}/role", member.getId())
                                .with(csrf())
                                .param("role", "ADMIN"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void noOpDoesNotCreateAudit() {
        service.changeRole(admin.getEmail(), member.getId(), Role.USER);
        assertEquals(0, changes.count());
    }

    @Test
    void rolePromotionNeedsFreshLoginAndDemotionRevokesExistingAdminSession() throws Exception {
        member.setPasswordHash("{noop}testing123");
        users.flush();

        var firstLogin = mvc.perform(post("/login").with(csrf())
                        .param("email", member.getEmail())
                        .param("password", "testing123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        var initialSession = (MockHttpSession) firstLogin.getRequest().getSession(false);
        assertNotNull(initialSession);
        mvc.perform(get("/admin/users").session(initialSession))
                .andExpect(status().isForbidden());

        service.changeRole(admin.getEmail(), member.getId(), Role.ADMIN);
        // Old session has ROLE_USER: a promotion does not silently grant privileges.
        mvc.perform(get("/admin/users").session(initialSession))
                .andExpect(status().isForbidden());

        var secondLogin = mvc.perform(post("/login").with(csrf())
                        .param("email", member.getEmail())
                        .param("password", "testing123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        var adminSession = (MockHttpSession) secondLogin.getRequest().getSession(false);
        assertNotNull(adminSession);
        mvc.perform(get("/admin/users").session(adminSession))
                .andExpect(status().isOk());
        mvc.perform(get("/admin/reports").session(adminSession))
                .andExpect(status().isOk());

        service.changeRole(admin.getEmail(), member.getId(), Role.USER);
        // The authenticated session still has ROLE_ADMIN, but DB authority was revoked.
        mvc.perform(get("/admin/users").session(adminSession))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin/reports").session(adminSession))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/users/{id}/role", admin.getId())
                        .session(adminSession).with(csrf()).param("role", "USER"))
                .andExpect(status().isForbidden());
        assertEquals(Role.ADMIN, admin.getRole());
    }

    @Test
    void persistedRoleAndActiveFlagAreRequiredEvenForAnAdminClaim() throws Exception {
        mvc.perform(get("/admin/reports").with(user(member.getEmail()).roles("ADMIN")))
                .andExpect(status().isForbidden());
        admin.setActive(false);
        users.flush();
        mvc.perform(get("/admin/users").with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isForbidden());
    }
}
