package com.manager.money.exchange.manager.Strategy.Validations;

import com.manager.money.exchange.manager.DTO.MoneyDTOS.AddMoneyDTO;
import com.manager.money.exchange.manager.Repository.MoneyInfoRepository;
import com.manager.money.exchange.manager.Strategy.IValidationStrategy;
import com.manager.money.exchange.manager.Strategy.ValidationError;
import com.manager.money.exchange.manager.User.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AddMoneyValidation implements IValidationStrategy<AddMoneyDTO> {

    private final MoneyInfoRepository repository;

    public AddMoneyValidation(MoneyInfoRepository repository) {
        this.repository = repository;
    }

    private static final double MAX_AMOUNT = 1_000_000_000d;


    @Override
    public Class<AddMoneyDTO> supports() {
        return AddMoneyDTO.class;
    }

    @Override
    public Optional<ValidationError> validate(AddMoneyDTO dto, User user) {
        if (!Double.isFinite(dto.amount()) || dto.amount() <= 0 || dto.amount() > MAX_AMOUNT)
        return Optional.of(new ValidationError(HttpStatus.BAD_REQUEST, "Valor inválido ou acima do limite"));


        if (repository.findByUser(user).isEmpty())
            return Optional.of(new ValidationError(HttpStatus.CONFLICT,
                    "Saldo inicial não registrado"));

        return Optional.empty();
    }
}
