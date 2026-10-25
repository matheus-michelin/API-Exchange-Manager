package com.manager.money.exchange.manager.DTO.MoneyDTOS;

import java.time.LocalDate;

public record AddMoneyResponseDTO(double moneyAdd, LocalDate date, double balanceAfter) {
}
