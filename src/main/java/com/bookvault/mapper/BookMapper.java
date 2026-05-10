package com.bookvault.mapper;

import com.bookvault.dto.BookDTO;
import com.bookvault.entity.Book;

import java.util.List;
import java.util.stream.Collectors;

public class BookMapper {

    public static BookDTO toDTO(Book entity) {
        if (entity == null) {
            return null;
        }
        return BookDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .author(entity.getAuthor())
                .genre(entity.getGenre())
                .tags(entity.getTags())
                .type(entity.getType())
                .price(entity.getPrice())
                .coverImageUrl(entity.getCoverImageUrl())
                .status(entity.getStatus())
                .isBestseller(entity.isBestseller())
                .isFeatured(entity.isFeatured())
                .createdAt(entity.getCreatedAt())
                .pdfUrl(entity.getPdfUrl())
                .readersCount(entity.getReadersCount())
                .favoritesCount(entity.getFavoritesCount())
                .contentJsonUrl(entity.getContentJsonUrl())

                .comments(CommentMapper.toDTOList(entity.getComments()))
                .build();
    }

    public static Book toEntity(BookDTO dto) {
        if (dto == null) {
            return null;
        }
        return Book.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .author(dto.getAuthor())
                .genre(dto.getGenre())
                .tags(dto.getTags())
                .type(dto.getType())
                .price(dto.getPrice())
                .coverImageUrl(dto.getCoverImageUrl())
                .status(dto.getStatus())
                .isBestseller(dto.isBestseller())
                .isFeatured(dto.isFeatured())
                .createdAt(dto.getCreatedAt())
                .pdfUrl(dto.getPdfUrl())
                .readersCount(dto.getReadersCount())
                .favoritesCount(dto.getFavoritesCount())
                .contentJsonUrl(dto.getContentJsonUrl())

                .comments(CommentMapper.toEntityList(dto.getComments()))
                .build();
    }

    public static List<BookDTO> toDTOList(List<Book> entities) {
        if (entities == null) return null;
        return entities.stream().map(BookMapper::toDTO).collect(Collectors.toList());
    }

    public static List<Book> toEntityList(List<BookDTO> dtos) {
        if (dtos == null) return null;
        return dtos.stream().map(BookMapper::toEntity).collect(Collectors.toList());
    }
}
