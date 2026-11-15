package com.manager.money.exchange.manager.controller;

import com.manager.money.exchange.manager.DTO.AuthenticationDTOS.CampoErro;
import com.manager.money.exchange.manager.DTO.AuthenticationDTOS.LoginDTO;
import com.manager.money.exchange.manager.DTO.AuthenticationDTOS.TokenDTO;
import com.manager.money.exchange.manager.DTO.AuthenticationDTOS.UserDTO;
import com.manager.money.exchange.manager.Repository.UserRepository;
import com.manager.money.exchange.manager.User.User;
import com.manager.money.exchange.manager.security.LoginThrottle;
import com.manager.money.exchange.manager.security.token.TokenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

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

    @Autowired
    private LoginThrottle throttle;

    @PostMapping
    public ResponseEntity<?> login(@RequestBody @Valid LoginDTO dto) {
        if (dto.senha().contains(" "))
            return ResponseEntity.badRequest().body(new CampoErro("senha", "A senha não pode conter espaços em branco"));
        var login = dto.login().toLowerCase(Locale.ROOT);
        var wait = throttle.blockedSeconds(login);
        if (wait > 0) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(wait))
                    .body(new CampoErro(null, "Muitas tentativas para este login. Tente novamente em " + wait + "s."));
        }
        try {
            var auth = manager.authenticate(new UsernamePasswordAuthenticationToken(login, dto.senha()));
            throttle.reset(login);
            return ResponseEntity.ok(new TokenDTO(tokenService.gerarToken((User) auth.getPrincipal())));
        } catch (AuthenticationException e) {
            throttle.fail(login); // conta também login inexistente: a resposta não revela se ele existe
            throw e;
        }
    }

    @PostMapping("/cadastro")
    public ResponseEntity<?> cadastrar(@RequestBody @Valid UserDTO dto) {
        var login = dto.login().toLowerCase(Locale.ROOT);
        if (userRepository.existsByLogin(login)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new CampoErro("login", "Esse login já está em uso"));
        }
        if (dto.senha().contains(" ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new CampoErro("senha", "A senha não pode conter espaços em branco"));
        }
        try {
            userRepository.save(new User(login, encoder.encode(dto.senha())));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new CampoErro("login", "Esse login já está em uso"));
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
