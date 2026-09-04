package com.petmatch.community.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.petmatch.community.model.SupportRequest;
import com.petmatch.community.model.enums.SupportRequestStatus;
import com.petmatch.community.model.enums.SupportType;

import jakarta.persistence.LockModeType;

public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> {

    @EntityGraph(attributePaths = {"pet", "owner"})
    List<SupportRequest> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    @EntityGraph(attributePaths = {"pet", "owner"})
    List<SupportRequest> findByStatusAndServiceDateAfterOrderByServiceDateAsc(
        SupportRequestStatus status,
        LocalDateTime serviceDate
    );

    @EntityGraph(attributePaths = {"pet", "owner"})
    Optional<SupportRequest> findByIdAndOwnerId(Long id, Long ownerId);

    @Override
    @EntityGraph(attributePaths = {"pet", "owner"})
    Optional<SupportRequest> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sr from SupportRequest sr where sr.id = :id")
    Optional<SupportRequest> findByIdForUpdate(@Param("id") Long id);

    List<SupportRequest> findByStatus(SupportRequestStatus status);

    List<SupportRequest> findBySupportType(SupportType supportType);

    List<SupportRequest> findByPetId(Long petId);

    boolean existsByPetId(Long petId);

    List<SupportRequest> findByStatusAndSupportType(
        SupportRequestStatus status,
        SupportType supportType
    );
}
