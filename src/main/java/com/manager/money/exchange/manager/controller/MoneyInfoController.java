package com.manager.money.exchange.manager.controller;

import com.manager.money.exchange.manager.DTO.MoneyDTOS.*;
import com.manager.money.exchange.manager.User.User;
import com.manager.money.exchange.manager.service.MoneyInfoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/geral")
public class MoneyInfoController {

    //Autowired
    private final MoneyInfoService service;
    //Autowired - Same thing but more secure
    public MoneyInfoController(MoneyInfoService service) {
        this.service = service;
    }

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
    public ResponseEntity<PageDTO<AddMoneyResponseDTO>> getMoneyExtact(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {
        return service.getMoneyExtract(user, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
    }

}