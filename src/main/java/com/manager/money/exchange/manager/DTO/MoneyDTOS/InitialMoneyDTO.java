package com.manager.money.exchange.manager.DTO.MoneyDTOS;

import jakarta.validation.constraints.PositiveOrZero;

public record InitialMoneyDTO(@PositiveOrZero double moneyQnt) {}
