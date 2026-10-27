package com.manager.money.exchange.manager.ExceptionHandler;

import com.manager.money.exchange.manager.DTO.AuthenticationDTOS.CampoErro;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.security.core.AuthenticationException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<CampoErro> autenticacao(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new CampoErro("login", "Login ou senha inválidos"));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity treatmentError404(){
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity treatmentError400(MethodArgumentNotValidException ex) {
        var erros = ex.getFieldErrors().stream()
                .map(erro -> new CampoErro(erro.getField(), erro.getDefaultMessage()))
                .toList();

        return ResponseEntity.badRequest().body(erros);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity tratarErroBadCredentials() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciais inválidas");
    }

    @ExceptionHandler
    public ResponseEntity<CampoErro> treatamentJSONParseError(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(new CampoErro(null, "Erro na leitura, dados inválidos ou incompletos"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<CampoErro> tratarErroAcessoNegado() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new CampoErro(null, "Acesso negado"));
    }


    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<CampoErro> handleConflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new CampoErro(null, "Operação simultânea detectada, tente novamente"));
    }
}