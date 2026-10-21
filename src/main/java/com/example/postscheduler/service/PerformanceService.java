package com.example.postscheduler.service;

import com.example.postscheduler.dto.CommentResponse;
import com.example.postscheduler.dto.PerformanceReport;
import com.example.postscheduler.dto.PostWithCommentsResponse;
import com.example.postscheduler.model.Post;
import com.example.postscheduler.model.PostStatus;
import com.example.postscheduler.repository.PostRepository;
import com.example.postscheduler.repository.PostSpecifications;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Supplier;

/**
 * Loads posts together with their comments in two different ways and reports how many
 * SQL statements Hibernate executed for each, so the N+1 problem can be seen and compared.
 * (The counter is global, so measure while no other requests are running.)
 */
@Service
public class PerformanceService {

    private static final Logger log = LoggerFactory.getLogger(PerformanceService.class);

    private final PostRepository repository;
    private final EntityManagerFactory entityManagerFactory;

    public PerformanceService(PostRepository repository, EntityManagerFactory entityManagerFactory) {
        this.repository = repository;
        this.entityManagerFactory = entityManagerFactory;
    }

    /** BAD: 1 query for the posts + 1 extra query per post to load its comments (N+1). */
    @Transactional(readOnly = true)
    public PerformanceReport naive(PostStatus status) {
        return measure("NAIVE (N+1 problem)", () -> (status == null)
                ? repository.findAll(Sort.by("id"))
                : repository.findAll(PostSpecifications.withFilters(status, null, null), Sort.by("id")));
    }

    /** GOOD: one JOIN FETCH query loads posts and comments together. */
    @Transactional(readOnly = true)
    public PerformanceReport optimized(PostStatus status) {
        return measure("OPTIMIZED (JOIN FETCH)", () -> (status == null)
                ? repository.findAllWithComments()
                : repository.findAllWithCommentsByStatus(status));
    }

    private PerformanceReport measure(String strategy, Supplier<List<Post>> loader) {
        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        long statementsBefore = stats.getPrepareStatementCount();
        long start = System.nanoTime();

        // Mapping touches post.getComments(), which is where the lazy loads (extra queries) happen.
        List<PostWithCommentsResponse> data = loader.get().stream().map(this::toResponse).toList();

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        long statements = stats.getPrepareStatementCount() - statementsBefore;
        int commentCount = data.stream().mapToInt(p -> p.comments().size()).sum();

        log.info("{}: posts={}, comments={}, sqlStatements={}, time={}ms",
                strategy, data.size(), commentCount, statements, elapsedMs);
        return new PerformanceReport(strategy, data.size(), commentCount, statements, elapsedMs, data);
    }

    private PostWithCommentsResponse toResponse(Post p) {
        List<CommentResponse> comments = p.getComments().stream()
                .map(c -> new CommentResponse(c.getId(), c.getCommenter(), c.getBody(), c.getCreatedAt()))
                .toList();
        return new PostWithCommentsResponse(p.getId(), p.getTitle(), p.getAuthor(), p.getStatus(), comments);
    }
}
