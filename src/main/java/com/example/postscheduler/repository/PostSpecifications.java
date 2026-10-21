package com.example.postscheduler.repository;

import com.example.postscheduler.model.Post;
import com.example.postscheduler.model.PostStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/** Builds the WHERE clause from whichever filters the client supplied. */
public final class PostSpecifications {

    private PostSpecifications() {}

    public static Specification<Post> withFilters(PostStatus status, String author, String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (author != null && !author.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("author")), author.trim().toLowerCase()));
            }
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("content")), like)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
