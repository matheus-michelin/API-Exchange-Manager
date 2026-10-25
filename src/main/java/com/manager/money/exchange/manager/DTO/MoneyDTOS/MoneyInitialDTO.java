package com.manager.money.exchange.manager.DTO.MoneyDTOS;

import jakarta.validation.constraints.PositiveOrZero;

public record MoneyInitialDTO(@PositiveOrZero double moneyQnt) {}
