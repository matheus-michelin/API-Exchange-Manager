package com.manager.money.exchange.manager.DTO.MoneyDTOS;

import java.time.LocalDate;

public record MoneyAddResponseDTO (double moneyAdd, LocalDate date, double balanceAfter) {
}
