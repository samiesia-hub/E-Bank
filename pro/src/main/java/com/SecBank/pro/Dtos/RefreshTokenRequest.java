package com.SecBank.pro.Dtos;


import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(

        @NotBlank(message = "Le Refresh Token est obligatoire")
        String refreshToken

) {}