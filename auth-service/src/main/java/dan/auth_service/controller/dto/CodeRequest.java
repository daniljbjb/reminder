/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.auth_service.controller.dto;

import jakarta.validation.constraints.NotBlank;

/**
 *
 * @author danil
 */
public record CodeRequest(@NotBlank String code) {}
