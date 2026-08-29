/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package dan.reminder_client.service;

import dan.reminder_client.dto.PagedResponse;
import dan.reminder_client.dto.ReminderPageDto;

/**
 *
 * @author danil
 */
public interface ReminderRestClient {
    PagedResponse<ReminderPageDto> findAllSortedByTitle(int page, int size);
    
    void addSomeReminders();
}
