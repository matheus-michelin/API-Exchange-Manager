package com.manager.money.exchange.manager.DTO.MoneyDTOS;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record InitialMoneyDTO(
        @NotNull
        @PositiveOrZero
        @Digits(integer = 17, fraction = 2)
        BigDecimal moneyQnt) {

}
