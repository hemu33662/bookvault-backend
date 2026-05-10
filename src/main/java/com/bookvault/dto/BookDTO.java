package com.bookvault.dto;

import com.bookvault.entity.BookType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookDTO {
    private String id;
    private String title;
    private String description;
    private String author;
    private String genre;
    private String tags;
    private BookType type;
    private BigDecimal price;
    private String coverImageUrl;
    private String status;
    @JsonProperty("isBestseller")
    private boolean isBestseller;
    @JsonProperty("isFeatured")
    private boolean isFeatured;
    private String createdAt;
    private String pdfUrl;
    private int readersCount;
    private int favoritesCount;
    private String contentJsonUrl;

    
    @Builder.Default
    private List<CommentDTO> comments = new ArrayList<>();
}
