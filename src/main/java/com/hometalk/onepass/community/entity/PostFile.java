package com.hometalk.onepass.community.entity;

import com.hometalk.onepass.community.enums.PostFileType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "post_files")
public class PostFile {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 게시글의 파일인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // 사용자가 올린 원본 파일명
    @Column(name = "original_name", nullable = false)
    private String originalName;

    // 서버에 저장된 파일명
    @Column(name = "stored_name", nullable = false)
    private String storedName;

    // 브라우저에서 접근할 경로
    @Column(name = "file_path", nullable = false)
    private String filePath;

    // 썸네일 / 본문 이미지 / 첨부파일 구분
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 20)
    private PostFileType fileType;

    @Column(name = "file_size")
    private Long fileSize;

}
