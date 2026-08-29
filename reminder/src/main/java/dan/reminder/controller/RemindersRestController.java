/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder.controller;

import dan.reminder.controller.payload.CreateReminderPayload;
import dan.reminder.dto.ReminderDto;
import dan.reminder.dto.PageResponse;
import dan.reminder.service.ReminderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 *
 * @author danil
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reminder")
public class RemindersRestController {

    private final ReminderService reminderService;

//{
//    "title": "Buy coffee",
//    "description": "Take after gym session",
//    "remind": "2026-03-20T15:39:30" // interpreted using X-Timezone
//}
    // /api/v1/reminder
    @PostMapping
    public ResponseEntity<ReminderDto> createReminder(
            @Valid @RequestBody CreateReminderPayload payload,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Timezone", required = false) String timezone,
            @RequestHeader(value = "Idempotency-Key")
            @NotBlank
            @Size(max = 255)
            String idempotencyKey) {

        UUID userId = UUID.fromString(jwt.getSubject());
        String userEmail = jwt.getClaim("email");

        ReminderDto dto = reminderService.createReminder(payload, userId, userEmail, timezone, idempotencyKey);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(dto.id())
                .toUri();

        return ResponseEntity.created(location).body(dto);
    }

    // GET /api/v1/reminder/sort/title?page=0&size=3&sort=title,desc
    @GetMapping("/sort/title")
    public ResponseEntity<PageResponse<ReminderDto>> findAllSortedByTitle(
            @ParameterObject
            @PageableDefault(
                    page = 0,
                    size = 3,
                    sort = "title",
                    direction = Sort.Direction.DESC
            ) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Timezone", required = false) String timezone
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());

        Page<ReminderDto> page = reminderService
                .findAllSortedByTitle(pageable, userId, timezone);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    // GET /api/v1/reminder/sort/date?page=0&size=3&sort=remind,desc
    @GetMapping("/sort/date")
    public ResponseEntity<PageResponse<ReminderDto>> findAllSortedByDate(
            @ParameterObject
            @PageableDefault(
            page = 0,
            size = 3,
            sort = "remind",
            direction = Sort.Direction.DESC
    ) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        UUID userId = UUID.fromString(jwt.getSubject());

        Page<ReminderDto> page = reminderService.findAllSortedByRemindDate(pageable, userId, timezone);
        return ResponseEntity.ok(PageResponse.from(page));
    }

    // GET /api/v1/reminder?title=test&page=0&size=2&sort=remind,desc
    @GetMapping
    public ResponseEntity<PageResponse<ReminderDto>> findRemindersByTitle(
            @RequestParam("title") @NotBlank(message = "Title must not be empty") String title,
            @ParameterObject
            @PageableDefault(
                    page = 0,
                    size = 3,
                    sort = "title",
                    direction = Sort.Direction.DESC
            ) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Timezone", required = false) String timezone) {

        UUID userId = UUID.fromString(jwt.getSubject());

        Page<ReminderDto> page = reminderService.findRemindersByTitle(title, userId, pageable, timezone);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    // GET /api/v1/reminder/search-by-date?date=2025-08-28&page=0&size=3&sort=remind,asc
    @GetMapping("/search-by-date")
    public ResponseEntity<PageResponse<ReminderDto>> findRemindersByDate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @ParameterObject
            @PageableDefault(
                    page = 0,
                    size = 3,
                    sort = "title",
                    direction = Sort.Direction.DESC
            ) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Timezone", required = false) String timezone) {

        UUID userId = UUID.fromString(jwt.getSubject());

        Page<ReminderDto> page = reminderService.findRemindersByDate(userId, date, pageable, timezone);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    // GET /api/v1/reminder/filter/date?start=2026-03-20T00:00:00Z&end=2026-03-20T23:59:59Z&page=0&size=5
    @GetMapping("/filter/date")
    public ResponseEntity<PageResponse<ReminderDto>> findRemindersByDateRange(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,
            @ParameterObject
            @PageableDefault(
                    page = 0,
                    size = 3,
                    sort = "title",
                    direction = Sort.Direction.DESC
            ) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Timezone", required = false) String timezone) {

        UUID userId = UUID.fromString(jwt.getSubject());

        Page<ReminderDto> page = reminderService.findRemindersByDateRange(userId, start, end, pageable, timezone);

        return ResponseEntity.ok(PageResponse.from(page));
    }

    // POST /api/v1/reminder/addsomereminders
    @PostMapping("/addsomereminders")
    public ResponseEntity<String> addSomeReminders(@AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        String userEmail = jwt.getClaim("email");

        reminderService.addSomeReminders(userId, userEmail);

        return ResponseEntity.ok("Reminders added");
    }

}
