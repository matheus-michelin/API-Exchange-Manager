package com.manager.money.exchange.manager.Repository;

import com.manager.money.exchange.manager.Model.MoneyInfo;
import com.manager.money.exchange.manager.User.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MoneyInfoRepository extends JpaRepository<MoneyInfo, Long> {
    Optional<MoneyInfo> findByUser(User user);

    Optional<MoneyInfo> findByUserId(Long id);

}
