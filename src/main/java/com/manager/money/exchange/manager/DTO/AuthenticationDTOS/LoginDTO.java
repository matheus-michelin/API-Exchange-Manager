package com.manager.money.exchange.manager.DTO.AuthenticationDTOS;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginDTO(@NotBlank String login, @NotBlank @Size(max = 128) String senha) {}
