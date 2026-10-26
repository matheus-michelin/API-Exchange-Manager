package com.manager.money.exchange.manager.DTO.MoneyDTOS;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AddMoneyResponseDTO(BigDecimal moneyAdd, LocalDate date, BigDecimal balanceAfter) {
}
