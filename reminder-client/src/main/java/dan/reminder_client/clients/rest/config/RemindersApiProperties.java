/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder_client.clients.rest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *
 * @author danil
 */
@ConfigurationProperties(prefix = "reminder.rest")
public record RemindersApiProperties(String baseUrl) {}
