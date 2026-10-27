package com.manager.money.exchange.manager.DTO.MoneyDTOS;

import java.util.List;

public record PageDTO<T>(List<T> content, int page, int size, long total) {
}
