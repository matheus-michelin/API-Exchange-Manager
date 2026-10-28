package com.manager.money.exchange.manager.service;

import com.manager.money.exchange.manager.DTO.AuthenticationDTOS.CampoErro;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MoneyInfoService {

    //A tabela money_add também retira, não se engane quando ler o nome
    //The table money_add also subtract, don't fool yourself when you read the name

    //Autowired
    private final MoneyInfoRepository repository;
    //Autowired
    private final MoneyAddRepository moneyAddRepository;
    //Autowired
    private final ValidationService validationService;

    //Autowired - Same thing but more secure
    public MoneyInfoService(MoneyInfoRepository repository, MoneyAddRepository moneyAddRepository, ValidationService validationService) {
        this.repository = repository;
        this.moneyAddRepository = moneyAddRepository;
        this.validationService = validationService;
    }

    @Transactional
    public ResponseEntity<?> initialMoney(InitialMoneyDTO dto, User user) {
        var error = validationService.validate(dto, user);
        if (error.isPresent())
            return ResponseEntity.status(error.get().status())
                    .body(new CampoErro(error.get().campo(), error.get().message()));

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
            return ResponseEntity.status(error.get().status())
                .body(new CampoErro(error.get().campo(), error.get().message()));

        var moneyInfo = repository.findByUser(user).orElseThrow();


        moneyInfo.setMoneyTotal(moneyInfo.getMoneyTotal().add(dto.amount()));
        repository.save(moneyInfo);
        moneyAddRepository.save(new MoneyAdd(user, dto.amount(), dto.date(), moneyInfo.getMoneyTotal(), dto.description()));

        return ResponseEntity.ok(new InfoMoneyResponseDTO(moneyInfo.getMoneyTotal()));
    }

    public ResponseEntity<PageDTO<AddMoneyResponseDTO>> getMoneyExtract(User user, Pageable pageable) {
        var p = moneyAddRepository.findByUserIdOrderByReceivedOnDescIdDesc(user.getId(), pageable)
                .map(a -> new AddMoneyResponseDTO(a.getAmount(), a.getReceivedOn(), a.getBalanceAfter(), a.getDescription()));
        return ResponseEntity.ok(new PageDTO<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements()));
    }

    @Transactional
    public ResponseEntity<?> removeMoneyInfo(RemoveMoneyDTO dto, User user) {

        var error = validationService.validate(dto, user);
        if (error.isPresent())
            return ResponseEntity.status(error.get().status())
                .body(new CampoErro(error.get().campo(), error.get().message()));

        var moneyInfo = repository.findByUser(user).orElseThrow();

        moneyInfo.setMoneyTotal(moneyInfo.getMoneyTotal().subtract(dto.amount()));
        repository.save(moneyInfo);
        moneyAddRepository.save(new MoneyAdd(user, dto.amount().negate(), dto.date(), moneyInfo.getMoneyTotal(), dto.description()));

        return ResponseEntity.ok(new InfoMoneyResponseDTO(moneyInfo.getMoneyTotal()));
    }
}

