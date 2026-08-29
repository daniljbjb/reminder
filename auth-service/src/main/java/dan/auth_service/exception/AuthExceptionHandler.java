/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.auth_service.exception;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 *
 * @author danil
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class AuthExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(InvalidCodeException.class)
    public ResponseEntity<String> invalidCode(Locale locale) {

        String message = messageSource.getMessage(
                "auth.code.invalid",
                null,
                locale
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(message);
    }

    @ExceptionHandler(CodeExpiredException.class)
    public ResponseEntity<String> expired(Locale locale) {
        
        String message = messageSource.getMessage(
                "auth.code.expired",
                null,
                locale
        );
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> internal(Locale locale) {
        
        String message = messageSource.getMessage(
                "auth.internal.error",
                null,
                locale
        );
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(message);
    }

}
