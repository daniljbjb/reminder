/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder.exception;

/**
 *
 * @author danil
 */
public class ReminderNotFoundException extends RuntimeException {
    public ReminderNotFoundException(String message) {
        super(message);
    }
}
