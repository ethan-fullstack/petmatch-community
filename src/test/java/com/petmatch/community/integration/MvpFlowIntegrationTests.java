package com.petmatch.community.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

import com.petmatch.community.dto.auth.RegistrationForm;
import com.petmatch.community.dto.pet.PetForm;
import com.petmatch.community.dto.supportapplication.SupportApplicationForm;
import com.petmatch.community.dto.supportrequest.SupportRequestForm;
import com.petmatch.community.exception.PetNotFoundException;
import com.petmatch.community.exception.SupportApplicationRuleException;
import com.petmatch.community.exception.SupportRequestNotFoundException;
import com.petmatch.community.model.Pet;
import com.petmatch.community.model.SupportApplication;
import com.petmatch.community.model.SupportRequest;
import com.petmatch.community.model.User;
import com.petmatch.community.model.enums.SupportApplicationStatus;
import com.petmatch.community.model.enums.SupportRequestStatus;
import com.petmatch.community.model.enums.SupportType;
import com.petmatch.community.repository.SupportApplicationRepository;
import com.petmatch.community.repository.SupportRequestRepository;
import com.petmatch.community.service.PetService;
import com.petmatch.community.service.SupportApplicationService;
import com.petmatch.community.service.SupportRequestService;
import com.petmatch.community.service.UserService;

@SpringBootTest
@Transactional
class MvpFlowIntegrationTests {

    @Autowired
    private UserService userService;

    @Autowired
    private PetService petService;

    @Autowired
    private SupportRequestService supportRequestService;

    @Autowired
    private SupportApplicationService supportApplicationService;

    @Autowired
    private SupportRequestRepository supportRequestRepository;

    @Autowired
    private SupportApplicationRepository supportApplicationRepository;

    @Test
    void completeMvpFlowKeepsOwnershipVisibilityAndStatusesConsistent() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        User owner = registerUser("Owner", "owner-" + suffix + "@example.com");
        User applicantB = registerUser("Applicant B", "applicant-b-" + suffix + "@example.com");
        User applicantC = registerUser("Applicant C", "applicant-c-" + suffix + "@example.com");
        User outsider = registerUser("Outsider", "outsider-" + suffix + "@example.com");

        Authentication ownerAuth = authenticationFor(owner);
        Authentication applicantBAuth = authenticationFor(applicantB);
        Authentication applicantCAuth = authenticationFor(applicantC);
        Authentication outsiderAuth = authenticationFor(outsider);

        Pet pet = petService.create(petForm("Luna", "Perro", 4), ownerAuth);
        SupportRequest request = supportRequestService.create(requestForm(pet.getId()), ownerAuth);

        assertThrows(
            PetNotFoundException.class,
            () -> petService.findOwnedPet(pet.getId(), applicantBAuth)
        );

        assertThrows(
            SupportApplicationRuleException.class,
            () -> supportApplicationService.apply(request.getId(), applicationForm("Owner should not apply"), ownerAuth)
        );

        SupportApplication applicationB = supportApplicationService.apply(
            request.getId(),
            applicationForm("Puedo ayudar con Luna."),
            applicantBAuth
        );
        SupportApplication applicationC = supportApplicationService.apply(
            request.getId(),
            applicationForm("También tengo disponibilidad."),
            applicantCAuth
        );

        assertThrows(
            SupportApplicationRuleException.class,
            () -> supportApplicationService.apply(request.getId(), applicationForm("Duplicate"), applicantBAuth)
        );

        supportApplicationService.accept(applicationB.getId(), ownerAuth);

        SupportRequest inProgress = supportRequestRepository.findById(request.getId()).orElseThrow();
        SupportApplication accepted = supportApplicationRepository.findById(applicationB.getId()).orElseThrow();
        SupportApplication rejected = supportApplicationRepository.findById(applicationC.getId()).orElseThrow();

        assertEquals(SupportRequestStatus.IN_PROGRESS, inProgress.getStatus());
        assertEquals(SupportApplicationStatus.ACCEPTED, accepted.getStatus());
        assertEquals(SupportApplicationStatus.REJECTED, rejected.getStatus());

        assertEquals(
            request.getId(),
            supportRequestService.findVisibleRequest(request.getId(), applicantBAuth).getId()
        );
        assertThrows(
            SupportRequestNotFoundException.class,
            () -> supportRequestService.findVisibleRequest(request.getId(), outsiderAuth)
        );

        supportRequestService.complete(request.getId(), ownerAuth);

        SupportRequest completed = supportRequestRepository.findById(request.getId()).orElseThrow();
        assertEquals(SupportRequestStatus.COMPLETED, completed.getStatus());
        assertEquals(
            request.getId(),
            supportRequestService.findVisibleRequest(request.getId(), applicantBAuth).getId()
        );
    }

    private User registerUser(String name, String email) {
        RegistrationForm form = new RegistrationForm();
        form.setName(name);
        form.setEmail(email);
        form.setPassword("testing123");
        form.setConfirmPassword("testing123");
        return userService.register(form);
    }

    private Authentication authenticationFor(User user) {
        return new UsernamePasswordAuthenticationToken(
            user.getEmail(),
            "ignored",
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private PetForm petForm(String name, String species, int age) {
        PetForm form = new PetForm();
        form.setName(name);
        form.setSpecies(species);
        form.setAge(age);
        form.setDescription("Mascota de prueba para validar el flujo completo del MVP.");
        return form;
    }

    private SupportRequestForm requestForm(Long petId) {
        SupportRequestForm form = new SupportRequestForm();
        form.setTitle("Paseo para Luna");
        form.setDescription("Se necesita apoyo durante la tarde.");
        form.setSupportType(SupportType.WALK);
        form.setServiceDate(LocalDateTime.now().plusDays(2));
        form.setPetId(petId);
        return form;
    }

    private SupportApplicationForm applicationForm(String message) {
        SupportApplicationForm form = new SupportApplicationForm();
        form.setMessage(message);
        return form;
    }
}
