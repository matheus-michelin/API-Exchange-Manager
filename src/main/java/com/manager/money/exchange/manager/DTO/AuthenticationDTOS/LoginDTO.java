package com.manager.money.exchange.manager.DTO.AuthenticationDTOS;

import jakarta.validation.constraints.NotBlank;

public record LoginDTO(@NotBlank String login, @NotBlank String senha) {}
