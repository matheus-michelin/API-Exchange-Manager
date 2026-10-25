package com.manager.money.exchange.manager.DTO.MoneyDTOS;

import jakarta.validation.constraints.Negative;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;


import java.time.LocalDate;

public record MoneyRemoveDTO(@Positive double amount, @NotNull @PastOrPresent LocalDate date) {
}
