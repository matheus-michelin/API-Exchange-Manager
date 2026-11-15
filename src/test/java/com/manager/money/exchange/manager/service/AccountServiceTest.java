package com.manager.money.exchange.manager.service;

import com.manager.money.exchange.manager.Repository.MoneyAddRepository;
import com.manager.money.exchange.manager.Repository.MoneyInfoRepository;
import com.manager.money.exchange.manager.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock MoneyAddRepository moneyAddRepository;
    @Mock MoneyInfoRepository moneyInfoRepository;
    @Mock UserRepository userRepository;

    private AccountService service;

    @BeforeEach
    void setUp() {
        service = new AccountService(moneyAddRepository, moneyInfoRepository, userRepository);
    }

    @Test
    void deletesChildrenBeforeTheUser() {
        service.delete(7L);

        InOrder order = inOrder(moneyAddRepository, moneyInfoRepository, userRepository);
        order.verify(moneyAddRepository).deleteAllByUserId(7L);
        order.verify(moneyInfoRepository).deleteAllByUserId(7L);
        order.verify(userRepository).deleteById(7L);
    }

    @Test
    void nullUserIdDeletesNothing() {
        assertThatThrownBy(() -> service.delete(null)).isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(moneyAddRepository, moneyInfoRepository, userRepository);
    }
}
