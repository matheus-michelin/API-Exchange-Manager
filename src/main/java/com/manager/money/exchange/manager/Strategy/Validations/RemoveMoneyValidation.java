package com.manager.money.exchange.manager.Strategy.Validations;

import com.manager.money.exchange.manager.DTO.MoneyDTOS.RemoveMoneyDTO;
import com.manager.money.exchange.manager.Repository.MoneyInfoRepository;
import com.manager.money.exchange.manager.Strategy.IValidationStrategy;
import com.manager.money.exchange.manager.Strategy.ValidationError;
import com.manager.money.exchange.manager.User.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RemoveMoneyValidation implements IValidationStrategy<RemoveMoneyDTO> {

    private final MoneyInfoRepository repository;

    private static final double MAX_AMOUNT = 1_000_000_000d;


    public RemoveMoneyValidation(MoneyInfoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Class<RemoveMoneyDTO> supports() { return RemoveMoneyDTO.class; }

    @Override
    public Optional<ValidationError> validate(RemoveMoneyDTO dto, User user) {
        var moneyInfo = repository.findByUser(user).orElse(null);
        if (moneyInfo == null)
            return Optional.of(new ValidationError(HttpStatus.CONFLICT, "Saldo inicial não registrado"));

        if (!Double.isFinite(dto.amount()) || dto.amount() > moneyInfo.getMoneyTotal())
            return Optional.of(new ValidationError(HttpStatus.BAD_REQUEST, "Saldo insuficiente"));

        return Optional.empty();
    }
}