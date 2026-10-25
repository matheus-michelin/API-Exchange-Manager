package com.manager.money.exchange.manager.Model;

import com.manager.money.exchange.manager.User.User;
import jakarta.persistence.*;

@Entity(name = "MoneyInfo")
@Table(name = "money")
public class MoneyInfo {

    public MoneyInfo() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double moneyQnt;

    private double moneyTotal;

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

    public double getMoneyQnt() {
        return moneyQnt;
    }

    public void setMoneyQnt(double moneyQnt) {
        this.moneyQnt = moneyQnt;
    }

    public Long getId() {
        return id;
    }

    public double getMoneyTotal() {
        return moneyTotal;
    }

    public void setMoneyTotal(double moneyTotal) {
        this.moneyTotal = moneyTotal;
    }
}
