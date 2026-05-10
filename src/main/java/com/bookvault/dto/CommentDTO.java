package com.bookvault.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {
    private String id;
    private String bookId;
    private String userId;
    private String userName;
    private String content;
    private String createdAt;
    private String updatedAt;
}
