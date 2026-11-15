package com.manager.money.exchange.manager.Validation;

import com.manager.money.exchange.manager.DTO.MoneyDTOS.InitialMoneyDTO;
import com.manager.money.exchange.manager.Model.MoneyInfo;
import com.manager.money.exchange.manager.Repository.MoneyInfoRepository;
import com.manager.money.exchange.manager.Strategy.IValidationStrategy;
import com.manager.money.exchange.manager.Strategy.ValidationService;
import com.manager.money.exchange.manager.Strategy.Validations.AddMoneyValidation;
import com.manager.money.exchange.manager.Strategy.Validations.RemoveMoneyValidation;
import com.manager.money.exchange.manager.Strategy.Validations.ValidationInitialMoney;
import com.manager.money.exchange.manager.User.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    @Mock
    MoneyInfoRepository repository;

    private final User user = new User("tester", "password123");
    private ValidationService service;

    @BeforeEach
    void setUp() {
        service = new ValidationService(List.<IValidationStrategy<?>>of(
                new AddMoneyValidation(repository),
                new RemoveMoneyValidation(repository),
                new ValidationInitialMoney(repository)));
    }

    @Test
    void routesDtoToItsStrategy() {
        when(repository.findByUser(user)).thenReturn(Optional.of(new MoneyInfo()));

        var result = service.validate(new InitialMoneyDTO(new BigDecimal("10.00")), user);

        assertThat(result).hasValueSatisfying(error ->
                assertThat(error.status()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void failsOnDuplicatedStrategy() {
        var duplicated = List.<IValidationStrategy<?>>of(
                new AddMoneyValidation(repository),
                new AddMoneyValidation(repository));

        assertThatThrownBy(() -> new ValidationService(duplicated))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AddMoneyDTO");
    }

    @Test
    void failsWhenNoStrategySupportsTheDto() {
        assertThatThrownBy(() -> service.validate("unsupported", user))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("String");
    }

    @Test
    void failsOnNullArguments() {
        assertThatThrownBy(() -> service.validate(null, user)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> service.validate(new InitialMoneyDTO(BigDecimal.ONE), null))
                .isInstanceOf(NullPointerException.class);
    }
}
