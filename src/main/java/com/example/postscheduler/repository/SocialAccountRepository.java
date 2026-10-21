package com.example.postscheduler.repository;

import com.example.postscheduler.model.Platform;
import com.example.postscheduler.model.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    @Query("SELECT s FROM SocialAccount s WHERE LOWER(s.owner.username) = LOWER(:username) ORDER BY s.id")
    List<SocialAccount> findAllByOwner(@Param("username") String username);

    /** Ownership is part of the query, so one user can never load another user's account. */
    @Query("SELECT s FROM SocialAccount s WHERE s.id = :id AND LOWER(s.owner.username) = LOWER(:username)")
    Optional<SocialAccount> findByIdAndOwner(@Param("id") Long id, @Param("username") String username);

    @Query("SELECT COUNT(s) FROM SocialAccount s WHERE LOWER(s.owner.username) = LOWER(:username) "
            + "AND s.platform = :platform AND LOWER(s.accountHandle) = LOWER(:handle)")
    long countDuplicates(@Param("username") String username, @Param("platform") Platform platform,
                         @Param("handle") String handle);
}
