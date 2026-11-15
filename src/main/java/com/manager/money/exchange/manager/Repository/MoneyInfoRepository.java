package com.manager.money.exchange.manager.Repository;

import com.manager.money.exchange.manager.Model.MoneyInfo;
import com.manager.money.exchange.manager.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MoneyInfoRepository extends JpaRepository<MoneyInfo, Long> {
    Optional<MoneyInfo> findByUser(User user);

    Optional<MoneyInfo> findByUserId(Long id);

    @Modifying
    @Query("delete from MoneyInfo m where m.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

}
