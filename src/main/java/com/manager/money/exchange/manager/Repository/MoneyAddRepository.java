package com.manager.money.exchange.manager.Repository;

import com.manager.money.exchange.manager.Model.MoneyAdd;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface MoneyAddRepository extends JpaRepository<MoneyAdd, Long> {

    Page<MoneyAdd> findByUserIdOrderByReceivedOnDescIdDesc(Long userId, Pageable pageable);
}
