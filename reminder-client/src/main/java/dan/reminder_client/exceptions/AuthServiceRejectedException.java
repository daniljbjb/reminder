/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder_client.exceptions;

/**
 *
 * @author danil
 */
public class AuthServiceRejectedException extends RuntimeException {
    public AuthServiceRejectedException(String message) { super(message); }
    public AuthServiceRejectedException(String message, Throwable cause) { super(message, cause); }
}
