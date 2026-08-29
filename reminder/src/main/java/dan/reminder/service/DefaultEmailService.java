/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 *
 * @author danil
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendTextEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject); // title
        message.setText(body); // description
        message.setFrom("danil18124@gmail.com");

        mailSender.send(message);
        log.info("Email reminder sent to {}", to);
    }
}
