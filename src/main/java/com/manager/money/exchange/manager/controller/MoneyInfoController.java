package com.manager.money.exchange.manager.controller;

import com.manager.money.exchange.manager.DTO.MoneyDTOS.*;
import com.manager.money.exchange.manager.User.User;
import com.manager.money.exchange.manager.service.MoneyInfoService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.apache.catalina.connector.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/geral")
public class MoneyInfoController {

    @Autowired
    private MoneyInfoService service;

    @PostMapping
    @Transactional
    public ResponseEntity<MoneyInfoResponseDTO> postMoneyInfo(@RequestBody @Valid MoneyInitialDTO dto, @AuthenticationPrincipal User user) {
        return service.initialMoney(dto, user);
    }

    @GetMapping
    public ResponseEntity<MoneyInfoResponseDTO> getMoneyInfo(@AuthenticationPrincipal User user) {

        return service.getMoneyInfo(user);
    }

    @PostMapping("/adicionar")
    @Transactional
    public ResponseEntity<MoneyInfoResponseDTO> addMoneyInfo(@RequestBody @Valid MoneyAddDTO dto, @AuthenticationPrincipal User user) {

        return service.addMoneyInfo(dto, user);
    }

    @PostMapping("/subtrair")
    @Transactional
    public ResponseEntity<MoneyInfoResponseDTO> removeMoneyInfo(@RequestBody @Valid MoneyRemoveDTO dto, @AuthenticationPrincipal User user) {

        return service.removeMoneyInfo(dto, user);
    }

    @GetMapping("/extrato")
    public ResponseEntity<List<MoneyAddResponseDTO>> getMoneyExtact(@AuthenticationPrincipal User user) {
        return service.getMoneyExtract(user);
    }

}