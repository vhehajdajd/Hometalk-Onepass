package com.hometalk.onepass.community.dto.response;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.community.entity.Post;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// 작성자 닉네임 표시 + 권한 체크 용도 DTO
public class PostUserRsDTO {
    private Long id;
    private String role;
    private String title;
    private String content;
    private String nickname;

    // 권한 관련
    private boolean isEditable;     // 수정, 삭제, 상태변경 버튼 노출 여부
    private boolean isAdmin;        // 관리자 전용 기능(숨김, 고정) 노출 여부

    // 마켓 게시판의 게시글 상태값
    private String marketStatus;                // 로직용: "SALE", "RESERVED"
    private String marketStatusDescription;     // 표시용: "나눔중", "예약중"

    private String postStatus;            // 로직용: "ACTIVE", "HIDDEN"
    private String postStatusDescription; // 표시용: "활성", "숨김" (관리자 기능용)

    private boolean isPinned;       // 상단 고정 여부


    public PostUserRsDTO(Post post, User currentUser) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.nickname = post.getWriter().getNickname();
        // 작성자와 현재 로그인 유저의 ID 비교
        if (currentUser != null) {
            this.isEditable = post.getWriter().getId().equals(currentUser.getId());
            // 관리자 권한 확인 (User 엔티티의 UserRole 활용)
            this.isAdmin = currentUser.getRole() == User.UserRole.ADMIN;
        }

        // 3번 기능: 마켓 상태 및 고정 여부
        this.marketStatus = post.getMarketStatus().name();
        this.isPinned = post.isPinned();
    }
}
