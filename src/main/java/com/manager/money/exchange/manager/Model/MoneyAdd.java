package com.manager.money.exchange.manager.Model;

import com.manager.money.exchange.manager.User.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "money_add")
public class MoneyAdd {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    private double amount;

    private double balanceAfter;

    private Instant createdAt = Instant.now();

    protected MoneyAdd() {}

    private LocalDate receivedOn;

    public MoneyAdd(User user, double amount, LocalDate receivedOn, double balanceAfter) {
        this.user = user;
        this.amount = amount;
        this.receivedOn = receivedOn;
        this.balanceAfter = balanceAfter;
    }

    public double getAmount() {
        return amount;
    }


    public Instant getCreatedAt() {
        return createdAt;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }

    public LocalDate getReceivedOn() {
        return receivedOn;
    }

    public User getUser() {
        return user;
    }
}
