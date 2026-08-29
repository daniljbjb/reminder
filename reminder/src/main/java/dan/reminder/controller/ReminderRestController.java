/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder.controller;

import dan.reminder.controller.payload.UpdateReminderPayload;
import dan.reminder.dto.ReminderDto;
import dan.reminder.service.ReminderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author danil
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reminder")
public class ReminderRestController {

    private final ReminderService reminderService;

    // DELETE /api/v1/reminder/42
    @DeleteMapping("/{reminderId}")
    public ResponseEntity<Void> deleteReminder(
            @PathVariable Long reminderId,
            @AuthenticationPrincipal Jwt jwt) {
        
        UUID userId = UUID.fromString(jwt.getSubject());
        
        reminderService.deleteReminder(reminderId, userId);
        return ResponseEntity.noContent().build();
    }

    
//{
//    "title": "Buy coffee",
//    "description": "Take after gym session",
//    "remind": "2026-03-20T15:39:30" // interpreted using X-Timezone
//}
    // PATCH /api/v1/reminder/42
    @PatchMapping("/{reminderId}")
    public ResponseEntity<ReminderDto> updateReminder(
            @PathVariable Long reminderId,
            @Valid @RequestBody UpdateReminderPayload payload, 
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-Timezone", required = false) String timezone,
            @RequestHeader("Idempotency-Key")
            @NotBlank
            @Size(max = 255)
            String idempotencyKey) {
        
        UUID userId = UUID.fromString(jwt.getSubject());
        String userEmail = jwt.getClaim("email");
        
        ReminderDto updated = reminderService.updateReminder(reminderId, userId, payload, userEmail, timezone, idempotencyKey);
        
        return ResponseEntity.ok(updated);
    }
    


}
