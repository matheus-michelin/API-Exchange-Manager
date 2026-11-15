package com.manager.money.exchange.manager.Repository;

import com.manager.money.exchange.manager.Model.MoneyAdd;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface MoneyAddRepository extends JpaRepository<MoneyAdd, Long> {

    Page<MoneyAdd> findByUserIdOrderByReceivedOnDescIdDesc(Long userId, Pageable pageable);

    @Modifying
    @Query("delete from MoneyAdd m where m.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
