/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder.service;

/**
 *
 * @author danil
 */
public interface EmailService {

    public void sendTextEmail(String to, String subject, String body);
}
