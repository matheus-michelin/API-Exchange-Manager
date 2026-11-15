package com.manager.money.exchange.manager.service;

import com.manager.money.exchange.manager.Repository.MoneyAddRepository;
import com.manager.money.exchange.manager.Repository.MoneyInfoRepository;
import com.manager.money.exchange.manager.Repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private final MoneyAddRepository moneyAddRepository;
    private final MoneyInfoRepository moneyInfoRepository;
    private final UserRepository userRepository;

    public AccountService(MoneyAddRepository moneyAddRepository, MoneyInfoRepository moneyInfoRepository,
                          UserRepository userRepository) {
        this.moneyAddRepository = moneyAddRepository;
        this.moneyInfoRepository = moneyInfoRepository;
        this.userRepository = userRepository;
    }

    /** Apaga tudo do usuário numa transação só; as FKs não têm cascata, então a ordem importa. */
    @Transactional
    public void delete(Long userId) {
        if (userId == null) throw new IllegalArgumentException("userId obrigatório"); // nunca apagar "sem filtro"
        moneyAddRepository.deleteAllByUserId(userId);
        moneyInfoRepository.deleteAllByUserId(userId);
        userRepository.deleteById(userId);
    }
}
