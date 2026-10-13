/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.auth_service.exception;

/**
 *
 * @author danil
 */
public class CodeExpiredException extends RuntimeException {

    public CodeExpiredException(String message) {
        super(message);
    }

}
