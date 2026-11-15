package com.manager.money.exchange.manager.Validation;

import com.manager.money.exchange.manager.DTO.MoneyDTOS.AddMoneyDTO;
import com.manager.money.exchange.manager.Strategy.Validations.*;
import com.manager.money.exchange.manager.Model.MoneyInfo;
import com.manager.money.exchange.manager.Repository.MoneyInfoRepository;
import com.manager.money.exchange.manager.User.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddMoneyValidationTest {

    @Mock
    MoneyInfoRepository repository;

    private AddMoneyValidation validation;
    private final User user = new User("tester", "password123");

    @BeforeEach
    void setUp() {
        validation = new AddMoneyValidation(repository);
    }

    private AddMoneyDTO dto(String amount) {
        return new AddMoneyDTO(new BigDecimal(amount), LocalDate.now(), "deposit");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "-0.01"})
    void rejectsNonPositiveAmount(String amount) {
        var result = validation.validate(dto(amount), user);

        assertThat(result).hasValueSatisfying(error -> {
            assertThat(error.status()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(error.campo()).isEqualTo("amount");
        });
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsAmountAboveLimit() {
        var result = validation.validate(dto("1000000000.01"), user);

        assertThat(result).hasValueSatisfying(error -> {
            assertThat(error.status()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(error.campo()).isEqualTo("amount");
        });
    }

    @Test
    void acceptsAmountExactlyAtLimit() {
        when(repository.findByUser(user)).thenReturn(Optional.of(new MoneyInfo()));

        assertThat(validation.validate(dto("1000000000"), user)).isEmpty();
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
}
