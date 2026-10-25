package com.manager.money.exchange.manager.service;

import com.manager.money.exchange.manager.DTO.MoneyDTOS.*;
import com.manager.money.exchange.manager.Model.MoneyAdd;
import com.manager.money.exchange.manager.Model.MoneyInfo;
import com.manager.money.exchange.manager.Repository.MoneyAddRepository;
import com.manager.money.exchange.manager.Repository.MoneyInfoRepository;
import com.manager.money.exchange.manager.User.User;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MoneyInfoService {

    @Autowired
    private MoneyInfoRepository repository;

    @Autowired
    private MoneyAddRepository moneyAddRepository;

    @Transactional
    public ResponseEntity<MoneyInfoResponseDTO> initialMoney(MoneyInitialDTO dto, User user) {
        if (repository.findByUser(user).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        var m = new MoneyInfo();
        m.setUser(user);
        m.setMoneyQnt(dto.moneyQnt());
        m.setMoneyTotal(dto.moneyQnt());
        repository.save(m);
        return ResponseEntity.status(HttpStatus.CREATED).body(new MoneyInfoResponseDTO(m.getMoneyTotal()));
    }

    public ResponseEntity<MoneyInfoResponseDTO> getMoneyInfo(User user) {

        var moneyInfo = repository.findByUserId(user.getId())
                .map(m -> ResponseEntity.ok(new MoneyInfoResponseDTO(m.getMoneyTotal())))
                .orElse(ResponseEntity.notFound().build());

        return moneyInfo;
    }

    @Transactional
    public ResponseEntity<MoneyInfoResponseDTO> addMoneyInfo(MoneyAddDTO dto, User user) {

        var moneyInfo = repository.findByUser(user).orElse(null);
        if (moneyInfo == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }


        moneyInfo.setMoneyTotal(moneyInfo.getMoneyTotal() + dto.amount());
        repository.save(moneyInfo);
        moneyAddRepository.save(new MoneyAdd(user, dto.amount(), dto.date(), moneyInfo.getMoneyTotal()));

        return ResponseEntity.ok(new MoneyInfoResponseDTO(moneyInfo.getMoneyTotal()));
    }

    public ResponseEntity<List<MoneyAddResponseDTO>> getMoneyExtract(User user) {
        var extract = moneyAddRepository.findByUserIdOrderByReceivedOnDescIdDesc(user.getId()).stream()
                .map(a -> new MoneyAddResponseDTO(a.getAmount(), a.getReceivedOn(), a.getBalanceAfter()))
                .toList();
        return ResponseEntity.ok(extract);
    }

    @Transactional
    public ResponseEntity<MoneyInfoResponseDTO> removeMoneyInfo(@Valid MoneyRemoveDTO dto, User user) {

        var moneyInfo = repository.findByUser(user).orElse(null);
        if (moneyInfo == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        if (dto.amount() > moneyInfo.getMoneyTotal()) {
            return ResponseEntity.badRequest().build();
        }

        moneyInfo.setMoneyTotal(moneyInfo.getMoneyTotal() - dto.amount());
        repository.save(moneyInfo);
        moneyAddRepository.save(new MoneyAdd(user, -dto.amount(), dto.date(), moneyInfo.getMoneyTotal()));

        return ResponseEntity.ok(new MoneyInfoResponseDTO(moneyInfo.getMoneyTotal()));
    }
}

