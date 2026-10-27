package com.manager.money.exchange.manager.Strategy.Validations;

import com.manager.money.exchange.manager.DTO.MoneyDTOS.AddMoneyDTO;
import com.manager.money.exchange.manager.Repository.MoneyInfoRepository;
import com.manager.money.exchange.manager.Strategy.IValidationStrategy;
import com.manager.money.exchange.manager.Strategy.ValidationError;
import com.manager.money.exchange.manager.User.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class AddMoneyValidation implements IValidationStrategy<AddMoneyDTO> {
    //Autowired
    private final MoneyInfoRepository repository;
    //Autowired - Same thing but more secure
    public AddMoneyValidation(MoneyInfoRepository repository) {
        this.repository = repository;
    }

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000");


    @Override
    public Class<AddMoneyDTO> supports() {
        return AddMoneyDTO.class;
    }

    @Override
    public Optional<ValidationError> validate(AddMoneyDTO dto, User user) {
        if (dto.amount().signum() <= 0 || dto.amount().compareTo(MAX_AMOUNT) > 0)
            return Optional.of(new ValidationError(HttpStatus.BAD_REQUEST, "amount", "Valor inválido ou acima do limite"));


        if (repository.findByUser(user).isEmpty())
            return Optional.of(new ValidationError(HttpStatus.CONFLICT, "saldo",
                    "Saldo inicial não registrado"));

        return Optional.empty();
    }
}
