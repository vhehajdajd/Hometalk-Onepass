package com.hometalk.onepass.community.validator;

/*
    권한, 관리자, 상태 검사 로직
 */

import com.hometalk.onepass.community.dto.response.PostResponseDTO;
import com.hometalk.onepass.community.dto.response.PostUserRsDTO;
import com.hometalk.onepass.community.entity.Post;
import com.hometalk.onepass.community.exception.UnauthorizedAccessException;
import org.springframework.stereotype.Component;

@Component
public class PostValidator {

    // 이 메서드 하나로 수정 가능 여부와 관리자 여부를 한 번에 계산
    public void setAuthority(PostResponseDTO dto, Post post, PostUserRsDTO currentUser) {
        if (currentUser == null) {
            dto.setEditable(false);
            dto.setAdmin(false);
            return;
        }
        // 본인 확인
        dto.setEditable(post.getWriter().getId().equals(currentUser.getId()));
        // 관리자 확인
        dto.setAdmin(currentUser.isAdmin());
    }

    // 삭제/수정 시 권한이 있는지 확인하고 없으면 에러 발생
    public void validateOwner(Post post, Long currentUserId) {
        if (currentUserId == null || !post.getWriter().getId().equals(currentUserId)) {
            throw new UnauthorizedAccessException("해당 권한이 없습니다.");
        }
    }
}
