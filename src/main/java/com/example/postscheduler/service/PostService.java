package com.example.postscheduler.service;

import com.example.postscheduler.config.CacheConfig;
import com.example.postscheduler.config.PaginationProperties;
import com.example.postscheduler.dto.AuthorStatusCount;
import com.example.postscheduler.dto.PageResponse;
import com.example.postscheduler.dto.PostRequest;
import com.example.postscheduler.dto.PostResponse;
import com.example.postscheduler.dto.PostStatsResponse;
import com.example.postscheduler.dto.PostSummaryResponse;
import com.example.postscheduler.dto.ScheduleRequest;
import com.example.postscheduler.exception.DuplicateResourceException;
import com.example.postscheduler.exception.InvalidOperationException;
import com.example.postscheduler.exception.InvalidQueryParameterException;
import com.example.postscheduler.exception.ResourceNotFoundException;
import com.example.postscheduler.model.Post;
import com.example.postscheduler.model.PostStatus;
import com.example.postscheduler.repository.PostRepository;
import com.example.postscheduler.repository.PostSpecifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class PostService {

    private static final Logger log = LoggerFactory.getLogger(PostService.class);

    private static final List<String> SORTABLE_FIELDS =
            List.of("id", "title", "author", "status", "scheduledAt", "createdAt", "updatedAt");

    private final PostRepository repository;
    private final PaginationProperties pagination;

    public PostService(PostRepository repository, PaginationProperties pagination) {
        this.repository = repository;
        this.pagination = pagination;
    }

    // A new post changes the statistics, so cached stats are discarded.
    @CacheEvict(value = CacheConfig.POST_STATS, allEntries = true)
    public PostResponse create(PostRequest req) {
        log.debug("Creating post for author '{}'", req.author());
        if (repository.existsByTitleIgnoreCaseAndAuthorIgnoreCase(req.title(), req.author())) {
            throw new DuplicateResourceException(
                    "A post with this title already exists for author " + req.author());
        }
        Post post = new Post();
        apply(post, req);
        Post saved = repository.save(post);
        log.info("Post created: id={}, status={}", saved.getId(), saved.getStatus());
        return PostResponse.from(saved);
    }

    /** Paginated, sortable and filterable list (Experiment 3). Not cached because it varies per request. */
    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> search(PostStatus status, String author, String keyword,
                                                    Integer page, Integer size,
                                                    String sortBy, String direction) {
        int pageNo = (page == null) ? 0 : page;
        int pageSize = (size == null) ? pagination.getDefaultPageSize() : size;
        String sortField = isBlank(sortBy) ? pagination.getDefaultSortBy() : sortBy.trim();
        String dir = isBlank(direction) ? pagination.getDefaultDirection() : direction.trim().toLowerCase();

        if (pageNo < 0) {
            throw new InvalidQueryParameterException("Page index must not be negative");
        }
        if (pageSize < 1 || pageSize > pagination.getMaxPageSize()) {
            throw new InvalidQueryParameterException(
                    "Page size must be between 1 and " + pagination.getMaxPageSize());
        }
        if (!SORTABLE_FIELDS.contains(sortField)) {
            throw new InvalidQueryParameterException(
                    "Cannot sort by '" + sortField + "'. Allowed fields: " + SORTABLE_FIELDS);
        }
        if (!dir.equals("asc") && !dir.equals("desc")) {
            throw new InvalidQueryParameterException("Direction must be 'asc' or 'desc'");
        }

        Sort sort = Sort.by(Sort.Direction.fromString(dir), sortField);
        if (!sortField.equals("id")) {
            sort = sort.and(Sort.by(Sort.Direction.ASC, "id"));
        }
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<Post> result = repository.findAll(PostSpecifications.withFilters(status, author, keyword), pageable);
        log.info("Fetched page {} of {} (size={}, totalElements={}, sort={},{})",
                result.getNumber(), result.getTotalPages(), result.getSize(),
                result.getTotalElements(), sortField, dir);

        return PageResponse.of(result.map(PostSummaryResponse::from), sortField, dir);
    }

    /** Cached by id. The method body only runs on a cache miss. */
    @Cacheable(value = CacheConfig.POSTS, key = "#id")
    @Transactional(readOnly = true)
    public PostResponse getById(Long id) {
        log.info("CACHE MISS: loading post id={} from the database", id);
        return PostResponse.from(find(id));
    }

    /** Native SQL aggregation, cached. The method body only runs on a cache miss. */
    @Cacheable(value = CacheConfig.POST_STATS, key = "'all'")
    @Transactional(readOnly = true)
    public PostStatsResponse getStats() {
        log.info("CACHE MISS: computing post statistics with a native SQL query");
        List<AuthorStatusCount> breakdown = new ArrayList<>();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        long total = 0;

        for (Object[] row : repository.countPostsByAuthorAndStatus()) {
            String author = String.valueOf(row[0]);
            String status = String.valueOf(row[1]);
            long count = ((Number) row[2]).longValue();
            breakdown.add(new AuthorStatusCount(author, status, count));
            byStatus.merge(status, count, Long::sum);
            total += count;
        }
        return new PostStatsResponse(total, byStatus, breakdown, LocalDateTime.now());
    }

    // Update the cached copy (so the next read is still a hit) and discard the stats.
    @Caching(
            put = @CachePut(value = CacheConfig.POSTS, key = "#id"),
            evict = @CacheEvict(value = CacheConfig.POST_STATS, allEntries = true))
    public PostResponse update(Long id, PostRequest req) {
        Post post = find(id);
        if (post.getStatus() == PostStatus.PUBLISHED) {
            throw new InvalidOperationException("Published posts cannot be edited");
        }
        apply(post, req);
        Post saved = repository.save(post);
        log.info("Post updated: id={}, status={}", saved.getId(), saved.getStatus());
        return PostResponse.from(saved);
    }

    @Caching(evict = {
            @CacheEvict(value = CacheConfig.POSTS, key = "#id"),
            @CacheEvict(value = CacheConfig.POST_STATS, allEntries = true)})
    public void delete(Long id) {
        repository.delete(find(id));
        log.info("Post deleted: id={}", id);
    }

    @Caching(
            put = @CachePut(value = CacheConfig.POSTS, key = "#id"),
            evict = @CacheEvict(value = CacheConfig.POST_STATS, allEntries = true))
    public PostResponse schedule(Long id, ScheduleRequest req) {
        Post post = find(id);
        if (post.getStatus() == PostStatus.PUBLISHED) {
            throw new InvalidOperationException("Published posts cannot be rescheduled");
        }
        post.setScheduledAt(req.scheduledAt());
        post.setStatus(PostStatus.SCHEDULED);
        log.info("Post scheduled: id={}, scheduledAt={}", id, req.scheduledAt());
        return PostResponse.from(repository.save(post));
    }

    @Caching(
            put = @CachePut(value = CacheConfig.POSTS, key = "#id"),
            evict = @CacheEvict(value = CacheConfig.POST_STATS, allEntries = true))
    public PostResponse unschedule(Long id) {
        Post post = find(id);
        if (post.getStatus() != PostStatus.SCHEDULED) {
            throw new InvalidOperationException("Only scheduled posts can be unscheduled");
        }
        post.setScheduledAt(null);
        post.setStatus(PostStatus.DRAFT);
        log.info("Post unscheduled: id={}", id);
        return PostResponse.from(repository.save(post));
    }

    // ---- helpers ----
    private Post find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Post", id));
    }

    private void apply(Post post, PostRequest req) {
        post.setTitle(req.title());
        post.setContent(req.content());
        post.setAuthor(req.author());
        post.setScheduledAt(req.scheduledAt());
        post.setStatus(req.scheduledAt() != null ? PostStatus.SCHEDULED : PostStatus.DRAFT);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
