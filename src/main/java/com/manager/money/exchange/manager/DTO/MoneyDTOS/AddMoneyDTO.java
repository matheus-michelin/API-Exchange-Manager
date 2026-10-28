package com.manager.money.exchange.manager.DTO.MoneyDTOS;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AddMoneyDTO(
        @Positive
        @Digits(integer = 10, fraction = 2)
        BigDecimal amount,
        @NotNull
        @PastOrPresent
        LocalDate date,
        @NotBlank
        @Size(min = 3, max = 85, message = "A descrição deve ter entre 3 e 85 caracteres")
        String description) {

}
