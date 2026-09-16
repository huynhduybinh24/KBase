package com.kbase.backend.document;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class DocumentSpecifications {

    private DocumentSpecifications() {
    }

    static Specification<Document> matching(UUID projectId, DocumentSearchCriteria filters) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("project").get("id"), projectId));
            predicates.add(builder.equal(root.get("status"), DocumentStatus.ACTIVE));
            if (filters.query() != null && !filters.query().isBlank()) {
                String pattern = "%" + filters.query().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("title")), pattern),
                        builder.like(builder.lower(root.get("description")), pattern)
                ));
            }
            if (filters.contentType() != null && !filters.contentType().isBlank()) {
                predicates.add(builder.equal(root.get("contentType"), filters.contentType().trim()));
            }
            if (filters.uploadedBy() != null) {
                predicates.add(builder.equal(root.get("uploadedBy").get("id"), filters.uploadedBy()));
            }
            if (filters.from() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), filters.from()));
            }
            if (filters.to() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), filters.to()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
