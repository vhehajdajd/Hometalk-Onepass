package com.hometalk.onepass.notice.controller;

import com.hometalk.onepass.notice.dto.NoticeDetailResponseDto;
import com.hometalk.onepass.notice.dto.NoticeListResponseDto;
import com.hometalk.onepass.notice.dto.NoticeRequestDto;
import com.hometalk.onepass.notice.entity.Attachment;
import com.hometalk.onepass.notice.entity.Notice;
import com.hometalk.onepass.notice.entity.NoticeStatus;
import com.hometalk.onepass.notice.service.NoticeService;
import com.hometalk.onepass.schedule.dto.ScheduleDetailResponseDto;
import com.hometalk.onepass.schedule.entity.RepeatType;
import com.hometalk.onepass.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    // ── 작성 페이지 ───────────────────────────────────────────────────────────\

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/write")
    public String noticeWriteForm() {
        return "notice/noticeForm";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/write")
    public String noticeWrite(@ModelAttribute NoticeRequestDto noticeRequestDto,
                              @RequestParam(required = false) List<MultipartFile> files,
                              HttpServletRequest request) {

        Long noticeId = noticeService.createNotice(noticeRequestDto, files);

        if (NoticeStatus.DRAFT.equals(noticeRequestDto.getStatus())) {
            return "redirect:/notice/write?saved=true";
        }

        Notice notice = noticeService.getNoticeEntity(noticeId);
        String noticeUrl = request.getScheme() + "://" +
                request.getServerName() + ":" +
                request.getServerPort() +
                request.getContextPath() + "/notice/" + noticeId;

        // 반복일정 여부 분기
        if (noticeRequestDto.getScheduleRepeatType() != null
                && noticeRequestDto.getScheduleRepeatType() != RepeatType.NONE) {
            scheduleService.createRepeatScheduleWithNotice(
                    notice,
                    noticeRequestDto.getScheduleName(),
                    noticeRequestDto.getScheduleStartAt(),
                    noticeRequestDto.getScheduleEndAt(),
                    noticeRequestDto.getScheduleInfo(),
                    noticeRequestDto.getScheduleLocation(),
                    noticeUrl,
                    noticeRequestDto.getScheduleRepeatType(),
                    noticeRequestDto.getScheduleRepeatEndAt()
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

        return "redirect:/notice/" + noticeId;
    }

    // ── 수정 페이지 ───────────────────────────────────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/edit")
    public String noticeEditForm(@PathVariable Long id, Model model) {
        NoticeDetailResponseDto notice = noticeService.getNoticeForEdit(id);
        model.addAttribute("notice", notice);

        Notice noticeEntity = noticeService.getNoticeEntity(id);
        ScheduleDetailResponseDto schedule = scheduleService.getScheduleByNotice(noticeEntity);
        model.addAttribute("linkedSchedule", schedule);

        List<Attachment> attachments = noticeService.getAttachments(id);
        model.addAttribute("attachments", attachments);

        return "notice/noticeEdit";
    }

    // ── 수정 처리 ─────────────────────────────────────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/edit")
    public String noticeEdit(@PathVariable Long id,
                             @ModelAttribute NoticeRequestDto noticeRequestDto,
                             @RequestParam(required = false) List<MultipartFile> files,
                             HttpServletRequest request) {
        noticeService.updateNotice(id, noticeRequestDto, files);

        if (NoticeStatus.DRAFT.equals(noticeRequestDto.getStatus())) {
            return "redirect:/notice/" + id + "/edit?saved=true";
        }

        Notice notice = noticeService.getNoticeEntity(id);
        String noticeUrl = request.getScheme() + "://" +
                request.getServerName() + ":" +
                request.getServerPort() +
                request.getContextPath() + "/notice/" + id;

        ScheduleDetailResponseDto existing = scheduleService.getScheduleByNotice(notice);

        if (noticeRequestDto.getScheduleRepeatType() != null
                && noticeRequestDto.getScheduleRepeatType() != RepeatType.NONE) {
            if (existing != null) {
                scheduleService.deleteScheduleByNotice(notice);
            }
            scheduleService.createRepeatScheduleWithNotice(
                    notice,
                    noticeRequestDto.getScheduleName(),
                    noticeRequestDto.getScheduleStartAt(),
                    noticeRequestDto.getScheduleEndAt(),
                    noticeRequestDto.getScheduleInfo(),
                    noticeRequestDto.getScheduleLocation(),
                    noticeUrl,
                    noticeRequestDto.getScheduleRepeatType(),
                    noticeRequestDto.getScheduleRepeatEndAt()
            );
        } else {
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
        }

        return "redirect:/notice/" + id;
    }

    // ── 삭제 ──────────────────────────────────────────────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/delete")
    public String noticeDelete(@PathVariable Long id) {
        noticeService.deleteNotice(id);
        return "redirect:/notice";
    }

    // ── 파일 다운로드 ─────────────────────────────────────────────────────────
    @GetMapping("/download/{attachmentId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long attachmentId) throws java.io.UnsupportedEncodingException {
        Attachment attachment = noticeService.getAttachment(attachmentId);

        System.out.println("파일 경로: " + attachment.getFilePath());  // ← 추가

        Path path = Paths.get(attachment.getFilePath());
        Resource resource = new FileSystemResource(path);

        System.out.println("파일 존재 여부: " + resource.exists());  // ← 추가

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String encodedFileName = java.net.URLEncoder.encode(attachment.getFileName(), "UTF-8")
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName)
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
            String serverUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            result.put("url", serverUrl + contextPath + "/uploads/" + fileName);
            return result;
        } catch (IOException e) {
            throw new RuntimeException("이미지 업로드 실패: " + e.getMessage());
        }
    }

    @GetMapping("/api/drafts")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public List<NoticeDetailResponseDto> getDrafts() {
        return noticeService.getDraftList();
    }

    @GetMapping("/api/detail")
    @ResponseBody
    public ResponseEntity<List<NoticeListResponseDto>> getDashboardNotices() {
        List<NoticeListResponseDto> notices = noticeService.getRecentNotices(5);
        return ResponseEntity.ok(notices);
    }

    @PostMapping("/draft")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> saveDraft(
            @ModelAttribute NoticeRequestDto noticeRequestDto,
            @RequestParam(required = false) List<MultipartFile> files) {
        Long noticeId = noticeService.createNotice(noticeRequestDto, files);
        Map<String, Object> result = new HashMap<>();
        result.put("id", noticeId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/draft")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateDraft(
            @PathVariable Long id,
            @ModelAttribute NoticeRequestDto noticeRequestDto,
            @RequestParam(required = false) List<MultipartFile> files) {
        noticeService.updateNotice(id, noticeRequestDto, files);
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        return ResponseEntity.ok(result);
    }
}