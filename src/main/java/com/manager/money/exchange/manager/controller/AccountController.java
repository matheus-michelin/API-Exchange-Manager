package com.manager.money.exchange.manager.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.manager.money.exchange.manager.DTO.AuthenticationDTOS.CampoErro;
import com.manager.money.exchange.manager.User.User;
import com.manager.money.exchange.manager.security.token.TokenRevocation;
import com.manager.money.exchange.manager.security.token.TokenService;
import com.manager.money.exchange.manager.service.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class AccountController {

    public record DeleteAccountDTO(@NotBlank @Size(max = 128) String senha) {}

    private final TokenService tokenService;
    private final TokenRevocation revocation;
    private final PasswordEncoder encoder;
    private final AccountService accountService;

    public AccountController(TokenService tokenService, TokenRevocation revocation,
                             PasswordEncoder encoder, AccountService accountService) {
        this.tokenService = tokenService;
        this.revocation = revocation;
        this.encoder = encoder;
        this.accountService = accountService;
    }

    /**
     * "Sair": cancela o token no servidor. Aberta no SecurityConfiguration (permitAll) para ser
     * idempotente: sair duas vezes com o mesmo token dá 204 nas duas. Token inválido dá 401.
     */

    @PostMapping("/sair")
    public ResponseEntity<?> sair(@RequestHeader(value = "Authorization", required = false) String header) {
        Optional<DecodedJWT> jwt = header != null && header.startsWith("Bearer ")
                ? tokenService.verificar(header.substring(7))
                : Optional.empty();
        if (jwt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new CampoErro(null, "Não autenticado"));
        }
        revocation.revoke(jwt.get().getId());
        return ResponseEntity.noContent().build();
    }

    /** Senha errada devolve 403, nunca 401: o front desloga em 401. */
    @PostMapping("/conta/excluir")
    public ResponseEntity<?> excluir(@RequestBody @Valid DeleteAccountDTO dto, @AuthenticationPrincipal User user) {
        if (!encoder.matches(dto.senha(), user.getSenha())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new CampoErro("senha", "Senha incorreta"));
        }
        accountService.delete(user.getId());
        return ResponseEntity.noContent().build();
    }
}
