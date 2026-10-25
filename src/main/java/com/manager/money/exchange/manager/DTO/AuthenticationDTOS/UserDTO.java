package com.manager.money.exchange.manager.DTO.AuthenticationDTOS;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserDTO(
        @NotBlank(message = "Login é obrigatório")
        @Pattern(regexp = "^[a-zA-Z0-9._-]{3,30}$",
                message = "Use de 3 a 30 caracteres: letras sem acento, números, ponto, hífen ou underline (sem espaços)")
        String login,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, max = 64, message = "A senha deve ter de 8 a 64 caracteres")
        String senha){}
