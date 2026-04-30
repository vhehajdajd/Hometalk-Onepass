package com.hometalk.onepass.community.dto.request;

import com.hometalk.onepass.community.entity.Comment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentRqDTO {
    private String content;

    public Comment toEntity() {
        return Comment.builder()
                .content(this.content)
                .build();
    }
}
