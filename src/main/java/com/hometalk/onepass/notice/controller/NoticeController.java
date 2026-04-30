package com.hometalk.onepass.notice.controller;

import com.hometalk.onepass.notice.dto.NoticeDetailResponseDto;
import com.hometalk.onepass.notice.dto.NoticeListResponseDto;
import com.hometalk.onepass.notice.dto.NoticeRequestDto;
import com.hometalk.onepass.notice.entity.Attachment;
import com.hometalk.onepass.notice.entity.Notice;
import com.hometalk.onepass.notice.service.NoticeService;
import com.hometalk.onepass.schedule.dto.ScheduleDetailResponseDto;
import com.hometalk.onepass.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;
    private final ScheduleService scheduleService;

    @Value("${file.upload.path}")
    private String uploadPath;

    // ── 목록 ──────────────────────────────────────────────────────────────────
    @GetMapping
    public String noticeList(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(required = false) String keyword,
                             @RequestParam(defaultValue = "tc") String searchType,
                             Model model) {
        Page<NoticeListResponseDto> notices;

        if (keyword == null || keyword.trim().isEmpty()) {
            notices = noticeService.getNoticeList(page);
        } else {
            notices = noticeService.searchNotice(keyword, searchType, page);
        }

        model.addAttribute("notices", notices);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notices.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("searchType", searchType);
        return "notice/noticeList";
    }

    // ── 상세 ──────────────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public String noticeDetail(@PathVariable Long id, Model model) {
        NoticeDetailResponseDto notice = noticeService.getNoticeDetail(id);
        NoticeListResponseDto preNotice = noticeService.getPreNotice(id);
        NoticeListResponseDto nextNotice = noticeService.getNextNotice(id);
        List<Attachment> attachments = noticeService.getAttachments(id);

        model.addAttribute("notice", notice);
        model.addAttribute("preNotice", preNotice);
        model.addAttribute("nextNotice", nextNotice);
        model.addAttribute("attachments", attachments);
        return "notice/noticeDetail";
    }

    // ── 작성 페이지 ───────────────────────────────────────────────────────────
    @GetMapping("/write")
    public String noticeWriteForm() {
        return "notice/noticeForm";
    }

    // ── 작성 처리 ─────────────────────────────────────────────────────────────
    @PostMapping("/write")
    public String noticeWrite(@ModelAttribute NoticeRequestDto noticeRequestDto,
                              @RequestParam(required = false) MultipartFile file,
                              HttpServletRequest request) {

        Long noticeId = noticeService.createNotice(noticeRequestDto, file);

        Notice notice = noticeService.getNoticeEntity(noticeId);
        String noticeUrl = request.getScheme() + "://" +
                request.getServerName() + ":" +
                request.getServerPort() +
                request.getContextPath() + "/notice/" + noticeId;

        scheduleService.createScheduleWithNotice(
                notice,
                noticeRequestDto.getScheduleName(),
                noticeRequestDto.getScheduleStartAt(),
                noticeRequestDto.getScheduleEndAt(),
                noticeRequestDto.getScheduleInfo(),
                noticeRequestDto.getScheduleLocation(),
                noticeUrl
        );

        return "redirect:/notice/" + noticeId;
    }

    // ── 수정 페이지 ───────────────────────────────────────────────────────────
    @GetMapping("/{id}/edit")
    public String noticeEditForm(@PathVariable Long id, Model model) {
        NoticeDetailResponseDto notice = noticeService.getNoticeForEdit(id);
        model.addAttribute("notice", notice);

        Notice noticeEntity = noticeService.getNoticeEntity(id);
        ScheduleDetailResponseDto schedule = scheduleService.getScheduleByNotice(noticeEntity);
        model.addAttribute("linkedSchedule", schedule);

        return "notice/noticeEdit";
    }

    // ── 수정 처리 ─────────────────────────────────────────────────────────────
    @PostMapping("/{id}/edit")
    public String noticeEdit(@PathVariable Long id,
                             @ModelAttribute NoticeRequestDto noticeRequestDto,
                             @RequestParam(required = false) MultipartFile file,
                             HttpServletRequest request) {
        noticeService.updateNotice(id, noticeRequestDto, file);

        Notice notice = noticeService.getNoticeEntity(id);
        String noticeUrl = request.getScheme() + "://" +
                request.getServerName() + ":" +
                request.getServerPort() +
                request.getContextPath() + "/notice/" + id;

        // 연결된 일정 있으면 수정, 없으면 새로 등록
        ScheduleDetailResponseDto existing = scheduleService.getScheduleByNotice(notice);
        if (existing != null) {
            scheduleService.updateScheduleWithNotice(
                    notice,
                    noticeRequestDto.getScheduleName(),
                    noticeRequestDto.getScheduleStartAt(),
                    noticeRequestDto.getScheduleEndAt(),
                    noticeRequestDto.getScheduleInfo(),
                    noticeRequestDto.getScheduleLocation(),
                    noticeUrl
            );
        } else {
            scheduleService.createScheduleWithNotice(
                    notice,
                    noticeRequestDto.getScheduleName(),
                    noticeRequestDto.getScheduleStartAt(),
                    noticeRequestDto.getScheduleEndAt(),
                    noticeRequestDto.getScheduleInfo(),
                    noticeRequestDto.getScheduleLocation(),
                    noticeUrl
            );
        }

        return "redirect:/notice/" + id;
    }

    // ── 삭제 ──────────────────────────────────────────────────────────────────
    @PostMapping("/{id}/delete")
    public String noticeDelete(@PathVariable Long id) {
        noticeService.deleteNotice(id);
        return "redirect:/notice";
    }

    // ── 파일 다운로드 ─────────────────────────────────────────────────────────
    @GetMapping("/download/{attachmentId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long attachmentId) {
        Attachment attachment = noticeService.getAttachment(attachmentId);

        Path path = Paths.get(attachment.getFilePath());
        Resource resource = new FileSystemResource(path);

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getFileName() + "\"")
                .body(resource);
    }

    // ── 에디터 이미지 업로드 ──────────────────────────────────────────────────
    @PostMapping("/image-upload")
    @ResponseBody
    public Map<String, String> uploadImage(@RequestParam("file") MultipartFile file,
                                           HttpServletRequest request) {
        try {
            File dir = new File(uploadPath);
            if (!dir.exists()) dir.mkdirs();

            String original = file.getOriginalFilename();
            String fileName = UUID.randomUUID() + "_" + (original != null ? original : "image");
            String filePath = uploadPath + "/" + fileName;

            file.transferTo(new File(filePath).getAbsoluteFile());

            String contextPath = request.getContextPath();
            Map<String, String> result = new HashMap<>();
            result.put("url", contextPath + "/uploads/" + fileName);
            return result;
        } catch (IOException e) {
            throw new RuntimeException("이미지 업로드 실패: " + e.getMessage());
        }
    }
}