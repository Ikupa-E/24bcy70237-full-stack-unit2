package com.example.postscheduler.dto;

import java.io.Serializable;

public record AuthorStatusCount(String author, String status, long total) implements Serializable {
}
