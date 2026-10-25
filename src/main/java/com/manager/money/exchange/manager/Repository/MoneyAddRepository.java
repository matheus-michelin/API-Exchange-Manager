package com.manager.money.exchange.manager.Repository;

import com.manager.money.exchange.manager.Model.MoneyAdd;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MoneyAddRepository extends JpaRepository<MoneyAdd, Long> {

    List<MoneyAdd> findByUserIdOrderByReceivedOnDescIdDesc(Long userId);
}
