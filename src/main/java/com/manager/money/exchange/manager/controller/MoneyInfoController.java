package com.manager.money.exchange.manager.controller;

import com.manager.money.exchange.manager.DTO.MoneyDTOS.AddMoneyDTO;
import com.manager.money.exchange.manager.DTO.MoneyDTOS.AddMoneyResponseDTO;
import com.manager.money.exchange.manager.DTO.MoneyDTOS.InitialMoneyDTO;
import com.manager.money.exchange.manager.DTO.MoneyDTOS.RemoveMoneyDTO;
import com.manager.money.exchange.manager.User.User;
import com.manager.money.exchange.manager.service.MoneyInfoService;
import jakarta.validation.Valid;
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
    public ResponseEntity<?> postMoneyInfo(@RequestBody @Valid InitialMoneyDTO dto, @AuthenticationPrincipal User user) {
        return service.initialMoney(dto, user);
    }

    @GetMapping
    public ResponseEntity<?> getMoneyInfo(@AuthenticationPrincipal User user) {

        return service.getMoneyInfo(user);
    }

    @PostMapping("/adicionar")
    public ResponseEntity<?> addMoneyInfo(@RequestBody @Valid AddMoneyDTO dto, @AuthenticationPrincipal User user) {

        return service.addMoneyInfo(dto, user);
    }

    @PostMapping("/subtrair")
    public ResponseEntity<?> removeMoneyInfo(@RequestBody @Valid RemoveMoneyDTO dto, @AuthenticationPrincipal User user) {

        return service.removeMoneyInfo(dto, user);
    }

    @GetMapping("/extrato")
    public ResponseEntity<List<AddMoneyResponseDTO>> getMoneyExtact(@AuthenticationPrincipal User user) {
        return service.getMoneyExtract(user);
    }

}