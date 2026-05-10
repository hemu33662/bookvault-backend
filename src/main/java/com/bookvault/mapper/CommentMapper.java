package com.bookvault.mapper;

import com.bookvault.dto.CommentDTO;
import com.bookvault.entity.Comment;

import java.util.List;
import java.util.stream.Collectors;

public class CommentMapper {

    public static CommentDTO toDTO(Comment entity) {
        if (entity == null) {
            return null;
        }
        return CommentDTO.builder()
                .id(entity.getId())
                .bookId(entity.getBookId())
                .userId(entity.getUserId())
                .userName(entity.getUserName())
                .content(entity.getContent())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static Comment toEntity(CommentDTO dto) {
        if (dto == null) {
            return null;
        }
        return Comment.builder()
                .id(dto.getId())
                .bookId(dto.getBookId())
                .userId(dto.getUserId())
                .userName(dto.getUserName())
                .content(dto.getContent())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }

    public static List<CommentDTO> toDTOList(List<Comment> entities) {
        if (entities == null) return null;
        return entities.stream().map(CommentMapper::toDTO).collect(Collectors.toList());
    }

    public static List<Comment> toEntityList(List<CommentDTO> dtos) {
        if (dtos == null) return null;
        return dtos.stream().map(CommentMapper::toEntity).collect(Collectors.toList());
    }
}
