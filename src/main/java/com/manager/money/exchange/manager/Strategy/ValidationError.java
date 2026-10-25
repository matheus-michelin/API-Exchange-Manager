package com.manager.money.exchange.manager.Strategy;

import org.springframework.http.HttpStatus;

public record ValidationError(HttpStatus status, String message) {}
