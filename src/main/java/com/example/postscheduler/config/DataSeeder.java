package com.example.postscheduler.config;

import com.example.postscheduler.model.Comment;
import com.example.postscheduler.model.Post;
import com.example.postscheduler.model.PostStatus;
import com.example.postscheduler.repository.CommentRepository;
import com.example.postscheduler.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Inserts 50 sample posts (and about 120 comments) on startup when app.seed.enabled=true and the table is empty. */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String[] AUTHORS = {"Asha", "Ravi", "Meera", "Karan", "Sneha"};
    private static final String[] COMMENTERS = {"Priya", "Dev", "Anil", "Zoya", "Nikhil"};
    private static final String[] TOPICS = {"Product launch", "Weekly tips", "Customer story",
            "Holiday sale", "Behind the scenes", "Industry news", "Team update", "How-to guide",
            "Event recap", "Feature spotlight"};

    private final PostRepository repository;
    private final CommentRepository commentRepository;

    public DataSeeder(PostRepository repository, CommentRepository commentRepository) {
        this.repository = repository;
        this.commentRepository = commentRepository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }

        List<Post> posts = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            String topic = TOPICS[i % TOPICS.length];
            Post p = new Post();
            p.setTitle(topic + " #" + i);
            p.setAuthor(AUTHORS[i % AUTHORS.length]);
            p.setContent("Detailed content for post " + i + " about " + topic.toLowerCase()
                    + ". This text is intentionally long so that the list endpoint returns only a short"
                    + " preview instead of the full body of the post.");
            if (i % 7 == 0) {
                p.setStatus(PostStatus.PUBLISHED);
                p.setScheduledAt(LocalDateTime.now().minusDays(i));
            } else if (i % 3 == 0) {
                p.setStatus(PostStatus.SCHEDULED);
                p.setScheduledAt(LocalDateTime.now().plusDays(i));
            } else {
                p.setStatus(PostStatus.DRAFT);
            }
            posts.add(p);
        }
        List<Post> saved = repository.saveAll(posts);

        // 0 to 5 comments per post (some posts have none, which exercises LEFT JOIN FETCH)
        List<Comment> comments = new ArrayList<>();
        for (int i = 0; i < saved.size(); i++) {
            int count = (i + 1) % 6;
            for (int j = 1; j <= count; j++) {
                Comment c = new Comment();
                c.setPost(saved.get(i));
                c.setCommenter(COMMENTERS[(i + j) % COMMENTERS.length]);
                c.setBody("Comment " + j + " on post " + saved.get(i).getId());
                comments.add(c);
            }
        }
        commentRepository.saveAll(comments);
        log.info("Seeded {} sample posts and {} comments", saved.size(), comments.size());
    }
}
