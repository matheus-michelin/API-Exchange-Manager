package com.manager.money.exchange.manager.Model;

import com.manager.money.exchange.manager.User.User;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity(name = "MoneyInfo")
@Table(name = "money")
public class MoneyInfo {

    public MoneyInfo() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(precision = 19, scale = 2)
    private BigDecimal moneyQnt;

    @Column(precision = 19, scale = 2)
    private BigDecimal moneyTotal;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Version
    private Long version;


    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getMoneyQnt() { return moneyQnt; }
    public void setMoneyQnt(BigDecimal moneyQnt) { this.moneyQnt = moneyQnt; }
    public BigDecimal getMoneyTotal() { return moneyTotal; }
    public void setMoneyTotal(BigDecimal moneyTotal) { this.moneyTotal = moneyTotal; }




}
