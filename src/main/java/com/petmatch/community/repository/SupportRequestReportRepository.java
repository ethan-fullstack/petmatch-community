package com.petmatch.community.repository;

import com.petmatch.community.dto.report.*;
import com.petmatch.community.model.SupportRequest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class SupportRequestReportRepository {
    private final EntityManager entityManager;

    public SupportRequestReportRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    private Predicate[] predicates(
            CriteriaBuilder builder, Root<SupportRequest> root, SupportRequestReportFilter filter) {
        filter.validate();
        var predicates = new ArrayList<Predicate>();
        if (filter.getStatus() != null) predicates.add(builder.equal(root.get("status"), filter.getStatus()));
        if (filter.getSupportType() != null) predicates.add(builder.equal(root.get("supportType"), filter.getSupportType()));
        if (filter.start() != null) predicates.add(builder.greaterThanOrEqualTo(root.get("serviceDate"), filter.start()));
        if (filter.endExclusive() != null) predicates.add(builder.lessThan(root.get("serviceDate"), filter.endExclusive()));
        return predicates.toArray(Predicate[]::new);
    }

    private CriteriaQuery<ReportRow> detailQuery(CriteriaBuilder builder, SupportRequestReportFilter filter) {
        var query = builder.createQuery(ReportRow.class);
        var root = query.from(SupportRequest.class);
        query.select(builder.construct(
                ReportRow.class,
                root.get("id"),
                root.get("title"),
                root.get("supportType"),
                root.get("status"),
                root.join("pet").get("name"),
                root.get("serviceDate")))
                .where(predicates(builder, root, filter))
                .orderBy(builder.asc(root.get("serviceDate")), builder.asc(root.get("id")));
        return query;
    }

    public Page<ReportRow> search(SupportRequestReportFilter filter, Pageable pageable) {
        var builder = entityManager.getCriteriaBuilder();
        var count = builder.createQuery(Long.class);
        var countRoot = count.from(SupportRequest.class);
        count.select(builder.count(countRoot)).where(predicates(builder, countRoot, filter));
        return new PageImpl<>(
                entityManager.createQuery(detailQuery(builder, filter))
                        .setFirstResult(Math.toIntExact(pageable.getOffset()))
                        .setMaxResults(pageable.getPageSize())
                        .getResultList(),
                pageable,
                entityManager.createQuery(count).getSingleResult());
    }

    public List<ReportRow> findForExport(SupportRequestReportFilter filter, int maximumRows) {
        if (maximumRows < 1) throw new IllegalArgumentException("Export limit must be positive");
        return entityManager.createQuery(detailQuery(entityManager.getCriteriaBuilder(), filter))
                .setMaxResults(Math.addExact(maximumRows, 1))
                .getResultList();
    }

    public List<ReportSummary> aggregate(SupportRequestReportFilter filter) {
        filter.validate();
        var builder = entityManager.getCriteriaBuilder();
        var query = builder.createTupleQuery();
        var root = query.from(SupportRequest.class);
        var groupKey = root.get(filter.getGrouping().entityAttribute());
        query.multiselect(groupKey, builder.count(root))
                .where(predicates(builder, root, filter))
                .groupBy(groupKey)
                .orderBy(builder.asc(groupKey));
        return entityManager.createQuery(query).getResultList().stream()
                .map(tuple -> new ReportSummary(tuple.get(0).toString(), tuple.get(1, Long.class)))
                .toList();
    }
}
