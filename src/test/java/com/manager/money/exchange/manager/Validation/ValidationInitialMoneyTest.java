package com.manager.money.exchange.manager.Validation;

import com.manager.money.exchange.manager.DTO.MoneyDTOS.InitialMoneyDTO;
import com.manager.money.exchange.manager.Model.MoneyInfo;
import com.manager.money.exchange.manager.Repository.MoneyInfoRepository;
import com.manager.money.exchange.manager.Strategy.Validations.ValidationInitialMoney;
import com.manager.money.exchange.manager.User.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationInitialMoneyTest {

    @Mock
    MoneyInfoRepository repository;

    private ValidationInitialMoney validation;
    private final User user = new User("tester", "password123");

    @BeforeEach
    void setUp() {
        validation = new ValidationInitialMoney(repository);
    }

    @Test
    void rejectsNullAmount() {
        var result = validation.validate(new InitialMoneyDTO(null), user);

        assertThat(result).hasValueSatisfying(error -> {
            assertThat(error.status()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(error.campo()).isEqualTo("moneyQnt");
        });
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsNegativeAmount() {
        var result = validation.validate(new InitialMoneyDTO(new BigDecimal("-0.01")), user);

        assertThat(result).hasValueSatisfying(error -> {
            assertThat(error.status()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(error.campo()).isEqualTo("moneyQnt");
        });
    }

    @Test
    void acceptsZeroWhenNoBalanceExists() {
        when(repository.findByUser(user)).thenReturn(Optional.empty());

        assertThat(validation.validate(new InitialMoneyDTO(BigDecimal.ZERO), user)).isEmpty();
    }

    @Test
    void acceptsLargeAmountWithoutCap() {
        when(repository.findByUser(user)).thenReturn(Optional.empty());

        assertThat(validation.validate(new InitialMoneyDTO(new BigDecimal("99999999999999999.99")), user)).isEmpty();
    }

    @Test
    void rejectsWhenBalanceAlreadyRegistered() {
        when(repository.findByUser(user)).thenReturn(Optional.of(new MoneyInfo()));

        var result = validation.validate(new InitialMoneyDTO(new BigDecimal("10.00")), user);

        assertThat(result).hasValueSatisfying(error -> {
            assertThat(error.status()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(error.campo()).isEqualTo("saldo");
        });
    }
}
