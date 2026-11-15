package com.manager.money.exchange.manager.Validation;

import com.manager.money.exchange.manager.DTO.MoneyDTOS.RemoveMoneyDTO;
import com.manager.money.exchange.manager.Model.MoneyInfo;
import com.manager.money.exchange.manager.Repository.MoneyInfoRepository;
import com.manager.money.exchange.manager.Strategy.Validations.RemoveMoneyValidation;
import com.manager.money.exchange.manager.User.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoveMoneyValidationTest {

    @Mock
    MoneyInfoRepository repository;

    private RemoveMoneyValidation validation;
    private final User user = new User("tester", "password123");

    @BeforeEach
    void setUp() {
        validation = new RemoveMoneyValidation(repository);
    }

    private RemoveMoneyDTO dto(String amount) {
        return new RemoveMoneyDTO(new BigDecimal(amount), LocalDate.now(), "withdrawal");
    }

    private MoneyInfo balance(String total) {
        var info = new MoneyInfo();
        info.setMoneyTotal(new BigDecimal(total));
        return info;
    }

    @Test
    void rejectsWhenInitialBalanceIsMissing() {
        when(repository.findByUser(user)).thenReturn(Optional.empty());

        var result = validation.validate(dto("10.00"), user);

        assertThat(result).hasValueSatisfying(error -> {
            assertThat(error.status()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(error.campo()).isEqualTo("saldo");
        });
    }

    @Test
    void rejectsAmountGreaterThanBalance() {
        when(repository.findByUser(user)).thenReturn(Optional.of(balance("100.00")));

        var result = validation.validate(dto("100.01"), user);

        assertThat(result).hasValueSatisfying(error -> {
            assertThat(error.status()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(error.campo()).isEqualTo("saldo");
            assertThat(error.message()).isEqualTo("Saldo insuficiente");
        });
    }

    @Test
    void acceptsAmountEqualToBalance() {
        when(repository.findByUser(user)).thenReturn(Optional.of(balance("100.00")));

        assertThat(validation.validate(dto("100.00"), user)).isEmpty();
    }

    @Test
    void rejectsAmountAboveLimitEvenWithEnoughBalance() {
        when(repository.findByUser(user)).thenReturn(Optional.of(balance("5000000000.00")));

        var result = validation.validate(dto("1000000000.01"), user);

        assertThat(result).hasValueSatisfying(error -> {
            assertThat(error.status()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(error.campo()).isEqualTo("amount");
        });
    }

    @Test
    void acceptsAmountExactlyAtLimit() {
        when(repository.findByUser(user)).thenReturn(Optional.of(balance("5000000000.00")));

        assertThat(validation.validate(dto("1000000000"), user)).isEmpty();
    }
}
