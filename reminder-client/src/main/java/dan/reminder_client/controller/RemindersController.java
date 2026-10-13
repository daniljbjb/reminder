/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder_client.controller;

import dan.reminder_client.dto.PagedResponse;
import dan.reminder_client.dto.ReminderPageDto;
import dan.reminder_client.exceptions.ReminderServiceException;
import dan.reminder_client.service.ReminderRestClient;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author danil
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/client/reminder")
public class RemindersController {

    private final ReminderRestClient restClient;

    @Value("${auth.public-login-url}")
    private String authPublicLoginUrl;

    // /client/reminder/main/bytitle
    @GetMapping("/main/bytitle")
    public String getRemindersSortedByTitle(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "4") int size,
            @RequestParam(defaultValue = "false") boolean addFailed,
            Authentication authentication, Model model) {

        boolean isAuthenticated = isAuthenticated(authentication);

        model.addAttribute("authenticated", isAuthenticated);
        model.addAttribute("authPublicLoginUrl", authPublicLoginUrl);

        if (isAuthenticated) {
            if (addFailed) {
                model.addAttribute(
                        "serviceError",
                        "Не удалось добавить напоминания. Попробуйте ещё раз позже."
                );
            }

            try {
                PagedResponse<ReminderPageDto> pageResponse
                        = restClient.findAllSortedByTitle(page, size);

                model.addAttribute("reminders", pageResponse.content());
                model.addAttribute("currentPage", pageResponse.number());
                model.addAttribute("totalPages", pageResponse.totalPages());
                model.addAttribute("size", pageResponse.size());
                model.addAttribute("sort", "bytitle");
            } catch (ReminderServiceException ex) {
                log.warn("Unable to load reminders", ex);
                model.addAttribute(
                        "serviceError",
                        "Сервис напоминаний временно недоступен. Попробуйте обновить страницу позже."
                );
                model.addAttribute("reminders", List.of());
                model.addAttribute("currentPage", page);
                model.addAttribute("totalPages", 0);
                model.addAttribute("size", size);
                model.addAttribute("sort", "bytitle");
            }
        }

        return "main";
    }

    @PostMapping("/addsomereminders")
    public String addSomeReminders() {
        try {
            restClient.addSomeReminders();
            return "redirect:/client/reminder/main/bytitle";
        } catch (ReminderServiceException ex) {
            log.warn("Unable to add sample reminders", ex);
            return "redirect:/client/reminder/main/bytitle?addFailed=true";
        }
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

}
