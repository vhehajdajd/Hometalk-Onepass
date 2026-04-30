package com.hometalk.onepass.dashboard.service.notification.impl;


import com.hometalk.onepass.dashboard.dto.notification.response.NotificationCommonResponseDto;
import com.hometalk.onepass.dashboard.repository.notification.NotificationRepository;
import com.hometalk.onepass.dashboard.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService{

    private final NotificationRepository notificationRepository;

    @Override
    public List<NotificationCommonResponseDto> findByIsReadFalseOrderByCreatedAtDesc() {

        // false를 전달하여 '읽지 않은' 알림 조회
        return notificationRepository.findByIsReadOrderByCreatedAtDesc(false)
                .stream()
                .map(NotificationCommonResponseDto::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationCommonResponseDto> findByIsReadTrueOrderByCreatedAtDesc() {


        // true를 전달하여 '읽은' 알림 조회
        return notificationRepository.findByIsReadOrderByCreatedAtDesc(true)
                .stream()
                .map(NotificationCommonResponseDto::from)
                .collect(Collectors.toList());
    }
}
