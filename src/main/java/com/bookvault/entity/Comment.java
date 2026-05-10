package com.bookvault.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    private String id;
    private String bookId;
    private String userId;
    private String userName; // To display without fetching user doc every time
    private String content;
    private String createdAt;
    private String updatedAt;
}
