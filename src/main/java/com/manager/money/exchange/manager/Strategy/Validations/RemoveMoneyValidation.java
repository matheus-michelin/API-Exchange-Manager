package com.manager.money.exchange.manager.Strategy.Validations;

import com.manager.money.exchange.manager.DTO.MoneyDTOS.RemoveMoneyDTO;
import com.manager.money.exchange.manager.Repository.MoneyInfoRepository;
import com.manager.money.exchange.manager.Strategy.IValidationStrategy;
import com.manager.money.exchange.manager.Strategy.ValidationError;
import com.manager.money.exchange.manager.User.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class RemoveMoneyValidation implements IValidationStrategy<RemoveMoneyDTO> {
    //Autowired
    private final MoneyInfoRepository repository;

    //Autowired - Same thing but more secure
    public RemoveMoneyValidation(MoneyInfoRepository repository) {
        this.repository = repository;
    }


    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000");


    @Override
    public Class<RemoveMoneyDTO> supports() { return RemoveMoneyDTO.class; }

    @Override
    public Optional<ValidationError> validate(RemoveMoneyDTO dto, User user) {
        var moneyInfo = repository.findByUser(user).orElse(null);
        if (moneyInfo == null)
            return Optional.of(new ValidationError(HttpStatus.CONFLICT, "saldo", "Cadastre o saldo inicial antes de movimentar"));

        if(dto.amount().compareTo(moneyInfo.getMoneyTotal()) > 0)
            return Optional.of(new ValidationError(HttpStatus.BAD_REQUEST, "saldo", "Saldo insuficiente"));

        if (dto.amount().compareTo(MAX_AMOUNT) > 0)
            return Optional.of(new ValidationError(HttpStatus.BAD_REQUEST, "amount", "Valor acima do limite por operação"));

        return Optional.empty();
    }
}