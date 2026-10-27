package com.manager.money.exchange.manager.DTO.AuthenticationDTOS;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginDTO(@NotBlank @Pattern(regexp = "\\S+", message = "O login não pode conter espaços") String login, @NotBlank @Size(max = 128) String senha) {}
