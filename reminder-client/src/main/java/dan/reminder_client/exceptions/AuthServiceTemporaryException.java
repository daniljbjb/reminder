/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder_client.exceptions;

/**
 *
 * @author danil
 */
public class AuthServiceTemporaryException extends RuntimeException {
    public AuthServiceTemporaryException(String message) { super(message); }
    public AuthServiceTemporaryException(String message, Throwable cause) { super(message, cause); }
}
