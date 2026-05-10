package com.bookvault.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Book {

    private String id; // Document ID in Firestore
    private String title;
    private String description;
    private String author;
    private String genre;
    private String tags;
    private BookType type;
    private BigDecimal price;
    private String coverImageUrl;
    private String status; // DRAFT, PUBLISHED
    private boolean isBestseller;
    private boolean isFeatured;
    private String createdAt;
    private String pdfUrl;
    private int readersCount;
    private int favoritesCount;
    private String contentJsonUrl;


    @Builder.Default
    private List<Comment> comments = new ArrayList<>();


}
