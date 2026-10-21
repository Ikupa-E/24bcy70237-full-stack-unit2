package com.example.postscheduler.repository;

import com.example.postscheduler.model.Post;
import com.example.postscheduler.model.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    boolean existsByTitleIgnoreCaseAndAuthorIgnoreCase(String title, String author);

    /** JOIN FETCH: posts and their comments are loaded together in ONE query (fixes N+1). */
    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.comments ORDER BY p.id")
    List<Post> findAllWithComments();

    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.comments WHERE p.status = :status ORDER BY p.id")
    List<Post> findAllWithCommentsByStatus(@Param("status") PostStatus status);

    /** Native SQL: aggregation done by the database. Rows are [author, status, count]. */
    @Query(value = "SELECT p.author, p.status, COUNT(*) FROM posts p "
            + "GROUP BY p.author, p.status ORDER BY p.author, p.status", nativeQuery = true)
    List<Object[]> countPostsByAuthorAndStatus();
}
