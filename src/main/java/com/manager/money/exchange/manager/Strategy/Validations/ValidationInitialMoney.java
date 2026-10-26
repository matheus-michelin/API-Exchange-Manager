package com.manager.money.exchange.manager.Strategy.Validations;

import com.manager.money.exchange.manager.DTO.MoneyDTOS.InitialMoneyDTO;
import com.manager.money.exchange.manager.Repository.MoneyInfoRepository;
import com.manager.money.exchange.manager.Strategy.IValidationStrategy;
import com.manager.money.exchange.manager.Strategy.ValidationError;
import com.manager.money.exchange.manager.User.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ValidationInitialMoney implements IValidationStrategy<InitialMoneyDTO> {

    //Autowired
    private final MoneyInfoRepository repository;
    //Autowired - Same thing but more secure
    public ValidationInitialMoney(MoneyInfoRepository moneyInfoRepository) {
        this.repository = moneyInfoRepository;
    };

    @Override
    public Class<InitialMoneyDTO> supports() {
        return InitialMoneyDTO.class;
    }

    @Override
    public Optional<ValidationError> validate(InitialMoneyDTO dto, User user) {
        if (dto.moneyQnt() == null || dto.moneyQnt().signum() < 0)
            return Optional.of(new ValidationError(HttpStatus.BAD_REQUEST, "Valor inválido"));

        if (repository.findByUser(user).isPresent())
            return Optional.of(new ValidationError(HttpStatus.CONFLICT, "Saldo inicial já registrado"));

        return Optional.empty();
    }
}
