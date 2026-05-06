package com.hometalk.onepass.common.entity;


import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@SQLRestriction("deleted_at IS NULL") // 소프트 딜리트 구현
public abstract class BaseSoftDeleteEntity extends BaseTimeEntity {

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {    // 삭제된 상태 확인 메서드 true = deleted / false = deleted X
        return this.deletedAt != null;
    }
}
