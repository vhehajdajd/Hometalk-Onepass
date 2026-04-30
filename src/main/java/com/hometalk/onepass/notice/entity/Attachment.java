package com.hometalk.onepass.notice.entity;

import com.hometalk.onepass.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Attachment extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id")
    private Notice notice;

    private String fileName;
    private String filePath;
    private long fileSize;

    public Attachment(Notice notice, String fileName, String filePath, long fileSize) {
        this.notice = notice;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
    }
}