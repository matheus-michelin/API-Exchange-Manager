package com.manager.money.exchange.manager.service;

import com.manager.money.exchange.manager.DTO.MoneyDTOS.*;
import com.manager.money.exchange.manager.Model.MoneyAdd;
import com.manager.money.exchange.manager.Model.MoneyInfo;
import com.manager.money.exchange.manager.Repository.MoneyAddRepository;
import com.manager.money.exchange.manager.Repository.MoneyInfoRepository;
import com.manager.money.exchange.manager.Strategy.ValidationService;
import com.manager.money.exchange.manager.User.User;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
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

    @Autowired
    private ValidationService validationService;

    @Transactional
    public ResponseEntity<?> initialMoney(InitialMoneyDTO dto, User user) {
        var error = validationService.validate(dto, user);
        if (error.isPresent())
            return ResponseEntity.status(error.get().status()).body(error.get().message());

        var m = new MoneyInfo();
        m.setUser(user);
        m.setMoneyQnt(dto.moneyQnt());
        m.setMoneyTotal(dto.moneyQnt());
        repository.save(m);
        return ResponseEntity.status(HttpStatus.CREATED).body(new InfoMoneyResponseDTO(m.getMoneyTotal()));
    }

    public ResponseEntity<InfoMoneyResponseDTO> getMoneyInfo(User user) {

        var moneyInfo = repository.findByUserId(user.getId())
                .map(m -> ResponseEntity.ok(new InfoMoneyResponseDTO(m.getMoneyTotal())))
                .orElse(ResponseEntity.notFound().build());

        return moneyInfo;
    }

    @Transactional
    public ResponseEntity<?> addMoneyInfo(AddMoneyDTO dto, User user) {

        var error = validationService.validate(dto, user);
        if (error.isPresent())
            return ResponseEntity.status(error.get().status()).body(error.get().message());

        var moneyInfo = repository.findByUser(user).orElseThrow();


        moneyInfo.setMoneyTotal(moneyInfo.getMoneyTotal() + dto.amount());
        repository.save(moneyInfo);
        moneyAddRepository.save(new MoneyAdd(user, dto.amount(), dto.date(), moneyInfo.getMoneyTotal()));

        return ResponseEntity.ok(new InfoMoneyResponseDTO(moneyInfo.getMoneyTotal()));
    }

    public ResponseEntity<List<AddMoneyResponseDTO>> getMoneyExtract(User user) {
        var extract = moneyAddRepository.findByUserIdOrderByReceivedOnDescIdDesc(user.getId()).stream()
                .map(a -> new AddMoneyResponseDTO(a.getAmount(), a.getReceivedOn(), a.getBalanceAfter()))
                .toList();
        return ResponseEntity.ok(extract);
    }

    @Transactional
    public ResponseEntity<?> removeMoneyInfo(RemoveMoneyDTO dto, User user) {

        var error = validationService.validate(dto, user);
        if (error.isPresent())
            return ResponseEntity.status(error.get().status()).body(error.get().message());

        var moneyInfo = repository.findByUser(user).orElseThrow();

        moneyInfo.setMoneyTotal(moneyInfo.getMoneyTotal() - dto.amount());
        repository.save(moneyInfo);
        moneyAddRepository.save(new MoneyAdd(user, -dto.amount(), dto.date(), moneyInfo.getMoneyTotal()));

        return ResponseEntity.ok(new InfoMoneyResponseDTO(moneyInfo.getMoneyTotal()));
    }
}

