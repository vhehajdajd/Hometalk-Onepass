package com.hometalk.onepass.dashboard.service.notification;

import com.hometalk.onepass.dashboard.dto.notification.response.NotificationCommonResponseDto;
import java.util.List;

public interface NotificationService {

    // isRead 필드가 false(읽지 않음)인 데이터만 조회
    List<NotificationCommonResponseDto> findByIsReadFalseOrderByCreatedAtDesc();

    // 읽은(True) 데이터 + 최신순 (추가)
    List<NotificationCommonResponseDto> findByIsReadTrueOrderByCreatedAtDesc();
}