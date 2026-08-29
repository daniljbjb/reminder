/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.auth_service.controller.dto;

import java.time.Instant;

/**
 *
 * @author danil
 */
public record JwtIssue(
        String jwt,
        Instant expiresAt
) {}
