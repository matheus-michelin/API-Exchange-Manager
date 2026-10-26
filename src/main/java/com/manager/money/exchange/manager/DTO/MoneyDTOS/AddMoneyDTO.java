package com.manager.money.exchange.manager.DTO.MoneyDTOS;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AddMoneyDTO(@Positive @Digits(integer = 10, fraction = 2) BigDecimal amount, @NotNull @PastOrPresent LocalDate date) {

}
