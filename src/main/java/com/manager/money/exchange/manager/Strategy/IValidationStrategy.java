package com.manager.money.exchange.manager.Strategy;

import com.manager.money.exchange.manager.User.User;

import java.util.Optional;

public interface IValidationStrategy<T> {

    Class<T> supports();
    Optional<ValidationError> validate(T dto, User user);
}
