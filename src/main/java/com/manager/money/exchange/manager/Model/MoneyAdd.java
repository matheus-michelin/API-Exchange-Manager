package com.manager.money.exchange.manager.Model;

import com.manager.money.exchange.manager.User.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

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

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(length = 85, nullable = false)
    @Size(min = 3, max = 85)
    private String description;

    private Instant createdAt = Instant.now();

    protected MoneyAdd() {}

    private LocalDate receivedOn;

    public MoneyAdd(User user, BigDecimal amount, LocalDate receivedOn, BigDecimal balanceAfter, String description) { /* corpo igual */
        this.user = user;
        this.amount = amount;
        this.receivedOn = receivedOn;
        this.balanceAfter = balanceAfter;
        this.description = description;
    }


    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public LocalDate getReceivedOn() {
        return receivedOn;
    }

    public User getUser() {
        return user;
    }

    public String getDescription() {
        return description;
    }
}
