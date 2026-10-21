package com.example.postscheduler.config;

import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.jsr107.Eh107Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.JCacheManagerCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * Enables Spring's cache abstraction and creates two Ehcache caches (heap only):
 *   posts     - single posts by id
 *   postStats - the aggregated statistics
 * Entries expire after app.cache.ttl-minutes; at most app.cache.max-entries are kept.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String POSTS = "posts";
    public static final String POST_STATS = "postStats";

    @Bean
    public JCacheManagerCustomizer ehcacheCustomizer(
            @Value("${app.cache.ttl-minutes:10}") long ttlMinutes,
            @Value("${app.cache.max-entries:500}") long maxEntries) {

        return cacheManager -> {
            for (String name : List.of(POSTS, POST_STATS)) {
                if (cacheManager.getCache(name) == null) {
                    CacheConfiguration<Object, Object> config = CacheConfigurationBuilder
                            .newCacheConfigurationBuilder(Object.class, Object.class,
                                    ResourcePoolsBuilder.heap(maxEntries))
                            .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofMinutes(ttlMinutes)))
                            .build();
                    cacheManager.createCache(name, Eh107Configuration.fromEhcacheCacheConfiguration(config));
                }
            }
        };
    }
}
