package com.manager.money.exchange.manager.Strategy.Validations;

import com.manager.money.exchange.manager.DTO.MoneyDTOS.InitialMoneyDTO;
import com.manager.money.exchange.manager.Repository.MoneyInfoRepository;
import com.manager.money.exchange.manager.Strategy.IValidationStrategy;
import com.manager.money.exchange.manager.Strategy.ValidationError;
import com.manager.money.exchange.manager.User.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

public class ValidationInitialMoney implements IValidationStrategy<InitialMoneyDTO> {

    @Autowired
    private MoneyInfoRepository repository;


    @Override
    public Class<InitialMoneyDTO> supports() {
        return InitialMoneyDTO.class;
    }

    @Override
    public Optional<ValidationError> validate(InitialMoneyDTO dto, User user) {
        if (!Double.isFinite(dto.moneyQnt()))
            return Optional.of(new ValidationError(HttpStatus.BAD_REQUEST, "Valor inválido"));

        if (repository.findByUser(user).isPresent())
            return Optional.of(new ValidationError(HttpStatus.CONFLICT, "Saldo inicial já registrado"));

        return Optional.empty();
    }
}
