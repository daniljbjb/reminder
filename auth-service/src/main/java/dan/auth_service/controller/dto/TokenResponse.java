/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.auth_service.controller.dto;

/**
 *
 * @author danil
 */
public record TokenResponse(
        String accessToken,
        long expiresIn
) {}
