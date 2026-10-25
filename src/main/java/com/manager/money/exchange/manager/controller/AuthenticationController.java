package com.manager.money.exchange.manager.controller;

import com.manager.money.exchange.manager.DTO.AuthenticationDTOS.CampoErro;
import com.manager.money.exchange.manager.DTO.AuthenticationDTOS.LoginDTO;
import com.manager.money.exchange.manager.DTO.AuthenticationDTOS.TokenDTO;
import com.manager.money.exchange.manager.DTO.AuthenticationDTOS.UserDTO;
import com.manager.money.exchange.manager.Repository.UserRepository;
import com.manager.money.exchange.manager.User.User;
import com.manager.money.exchange.manager.security.token.TokenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login")
public class AuthenticationController {

    @Autowired
    private AuthenticationManager manager;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder encoder;

    @PostMapping
    public ResponseEntity<TokenDTO> login(@RequestBody @Valid LoginDTO dto) {
        var auth = manager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.login().trim().toLowerCase(), dto.senha()));
        return ResponseEntity.ok(new TokenDTO(tokenService.gerarToken((User) auth.getPrincipal())));
    }

    @PostMapping("/cadastro")
    public ResponseEntity<?> cadastrar(@RequestBody @Valid UserDTO dto) {
        var login = dto.login().trim().toLowerCase();
        if (userRepository.existsByLogin(login)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new CampoErro("login", "Esse login já está em uso"));
        }
        userRepository.save(new User(login, encoder.encode(dto.senha())));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}