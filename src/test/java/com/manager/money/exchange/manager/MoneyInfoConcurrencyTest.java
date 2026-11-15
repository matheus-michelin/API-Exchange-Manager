package com.manager.money.exchange.manager;

import com.manager.money.exchange.manager.DTO.MoneyDTOS.AddMoneyDTO;
import com.manager.money.exchange.manager.DTO.MoneyDTOS.InitialMoneyDTO;
import com.manager.money.exchange.manager.DTO.MoneyDTOS.RemoveMoneyDTO;
import com.manager.money.exchange.manager.Model.MoneyInfo;
import com.manager.money.exchange.manager.Repository.MoneyAddRepository;
import com.manager.money.exchange.manager.Repository.MoneyInfoRepository;
import com.manager.money.exchange.manager.Repository.UserRepository;
import com.manager.money.exchange.manager.User.User;
import com.manager.money.exchange.manager.service.MoneyInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class MoneyInfoConcurrencyTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("api.security.token.secret", () -> "test-secret-test-secret-test-secret");
    }

    @Autowired
    UserRepository userRepository;

    @Autowired
    MoneyInfoRepository moneyInfoRepository;

    @Autowired
    MoneyAddRepository moneyAddRepository;

    @Autowired
    MoneyInfoService service;

    private User newUser() {
        return userRepository.save(new User("u" + UUID.randomUUID().toString().substring(0, 12), "password123"));
    }

    @Test
    void staleWriteIsRejected() {
        var user = newUser();
        var info = new MoneyInfo();
        info.setUser(user);
        info.setMoneyQnt(new BigDecimal("100.00"));
        info.setMoneyTotal(new BigDecimal("100.00"));
        moneyInfoRepository.save(info);

        var first = moneyInfoRepository.findByUser(user).orElseThrow();
        var second = moneyInfoRepository.findByUser(user).orElseThrow();

        first.setMoneyTotal(new BigDecimal("150.00"));
        moneyInfoRepository.save(first);

        second.setMoneyTotal(new BigDecimal("150.00"));
        assertThatThrownBy(() -> moneyInfoRepository.save(second))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void concurrentCreditsNeverLoseAnUpdate() throws Exception {
        var user = newUser();
        service.initialMoney(new InitialMoneyDTO(new BigDecimal("100.00")), user);

        int threads = 8;
        var pool = Executors.newFixedThreadPool(threads);
        var barrier = new CyclicBarrier(threads);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                barrier.await();
                try {
                    var response = service.addMoneyInfo(
                            new AddMoneyDTO(new BigDecimal("50.00"), LocalDate.now(), "deposit"), user);
                    return response.getStatusCode().is2xxSuccessful();
                } catch (OptimisticLockingFailureException e) {
                    return false;
                }
            }));
        }

        long succeeded = 0;
        for (var future : results) {
            if (future.get(30, TimeUnit.SECONDS)) succeeded++;
        }
        pool.shutdown();

        var total = moneyInfoRepository.findByUserId(user.getId()).orElseThrow().getMoneyTotal();
        var expected = new BigDecimal("100.00")
                .add(new BigDecimal("50.00").multiply(BigDecimal.valueOf(succeeded)));

        assertThat(succeeded).isGreaterThanOrEqualTo(1);
        assertThat(total).isEqualByComparingTo(expected);
    }

    @Test
    void statementStoresSignedAmountsAndRunningBalance() {
        var user = newUser();
        service.initialMoney(new InitialMoneyDTO(new BigDecimal("100.00")), user);
        service.addMoneyInfo(new AddMoneyDTO(new BigDecimal("50.00"), LocalDate.now(), "deposit"), user);
        service.removeMoneyInfo(new RemoveMoneyDTO(new BigDecimal("30.00"), LocalDate.now(), "withdrawal"), user);

        var page = moneyAddRepository.findByUserIdOrderByReceivedOnDescIdDesc(user.getId(), PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getAmount()).isEqualByComparingTo("-30.00");
        assertThat(page.getContent().get(0).getBalanceAfter()).isEqualByComparingTo("120.00");
        assertThat(page.getContent().get(1).getAmount()).isEqualByComparingTo("50.00");
        assertThat(page.getContent().get(1).getBalanceAfter()).isEqualByComparingTo("150.00");
    }
}
