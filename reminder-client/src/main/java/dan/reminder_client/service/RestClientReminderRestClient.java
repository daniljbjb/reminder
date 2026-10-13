/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder_client.service;

import dan.reminder_client.dto.PagedResponse;
import dan.reminder_client.dto.ReminderPageDto;
import dan.reminder_client.exceptions.ReminderServiceException;
import dan.reminder_client.model.ReminderDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 *
 * @author danil
 */
@Service
@RequiredArgsConstructor
public class RestClientReminderRestClient implements ReminderRestClient {

    private final RestClient restClient;

    @Override
    public PagedResponse<ReminderPageDto> findAllSortedByTitle(int page, int size) {
        try {
            PagedResponse<ReminderDto> response = restClient
                    .get()
                    .uri("/api/v1/reminder/sort/title?page={page}&size={size}&sort=title,desc", page, size)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (response == null) {
                throw new ReminderServiceException(
                        "Reminder-service returned an empty response",
                        null
                );
            }

            return response.map(this::toPageDto);
        } catch (RestClientException ex) {
            throw new ReminderServiceException(
                    "Failed to load reminders from reminder-service",
                    ex
            );
        }
    }

    @Override
    public void addSomeReminders() {
        try {
            restClient
                    .post()
                    .uri("/api/v1/reminder/addsomereminders")
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new ReminderServiceException(
                    "Failed to add reminders through reminder-service",
                    ex
            );
        }
    }

    private ReminderPageDto toPageDto(ReminderDto dto) {
        return new ReminderPageDto(
                dto.id(),
                dto.title(),
                dto.description(),
                dto.remind().toString(),
                dto.userId()
        );
    }

}
