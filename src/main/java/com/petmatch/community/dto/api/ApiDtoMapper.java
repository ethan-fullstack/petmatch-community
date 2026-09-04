package com.petmatch.community.dto.api;

import com.petmatch.community.dto.pet.PetForm;
import com.petmatch.community.dto.supportapplication.SupportApplicationForm;
import com.petmatch.community.dto.supportrequest.SupportRequestForm;
import com.petmatch.community.model.Pet;
import com.petmatch.community.model.SupportApplication;
import com.petmatch.community.model.SupportRequest;

public final class ApiDtoMapper {

    private ApiDtoMapper() {
    }

    public static PetForm toPetForm(PetApiRequest request) {
        PetForm form = new PetForm();
        form.setName(request.name());
        form.setSpecies(request.species());
        form.setAge(request.age());
        form.setDescription(request.description());
        return form;
    }

    public static PetApiResponse toPetResponse(Pet pet) {
        return new PetApiResponse(
            pet.getId(),
            pet.getName(),
            pet.getSpecies(),
            pet.getAge(),
            pet.getDescription()
        );
    }

    public static SupportRequestForm toSupportRequestForm(SupportRequestApiRequest request) {
        SupportRequestForm form = new SupportRequestForm();
        form.setTitle(request.title());
        form.setDescription(request.description());
        form.setSupportType(request.supportType());
        form.setServiceDate(request.serviceDate());
        form.setPetId(request.petId());
        return form;
    }

    public static SupportRequestApiResponse toSupportRequestResponse(SupportRequest request) {
        return new SupportRequestApiResponse(
            request.getId(),
            request.getTitle(),
            request.getDescription(),
            request.getSupportType(),
            request.getCreatedAt(),
            request.getServiceDate(),
            request.getStatus(),
            request.getPet().getId(),
            request.getPet().getName(),
            request.getOwner().getId(),
            request.getOwner().getName()
        );
    }

    public static SupportApplicationForm toSupportApplicationForm(SupportApplicationApiRequest request) {
        SupportApplicationForm form = new SupportApplicationForm();
        form.setMessage(request.message());
        return form;
    }

    public static SupportApplicationApiResponse toSupportApplicationResponse(SupportApplication application) {
        return new SupportApplicationApiResponse(
            application.getId(),
            application.getMessage(),
            application.getAppliedAt(),
            application.getStatus(),
            application.getApplicant().getId(),
            application.getApplicant().getName(),
            application.getSupportRequest().getId(),
            application.getSupportRequest().getTitle()
        );
    }
}
