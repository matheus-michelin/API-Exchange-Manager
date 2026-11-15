package com.manager.money.exchange.manager.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.manager.money.exchange.manager.DTO.AuthenticationDTOS.CampoErro;
import com.manager.money.exchange.manager.User.User;
import com.manager.money.exchange.manager.security.token.TokenRevocation;
import com.manager.money.exchange.manager.security.token.TokenService;
import com.manager.money.exchange.manager.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AccountControllerTest {

    private TokenService tokenService;
    private TokenRevocation revocation;
    private PasswordEncoder encoder;
    private AccountService accountService;
    private AccountController controller;
    private User user;

    @BeforeEach
    void setUp() {
        tokenService = mock(TokenService.class);
        revocation = mock(TokenRevocation.class);
        encoder = mock(PasswordEncoder.class);
        accountService = mock(AccountService.class);
        controller = new AccountController(tokenService, revocation, encoder, accountService);

        user = mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(user.getSenha()).thenReturn("hash");
    }

    @Test
    void sairWithoutHeaderIsUnauthorized() {
        var response = controller.sair(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isInstanceOf(CampoErro.class);
        verifyNoInteractions(tokenService, revocation);
    }

    @Test
    void sairWithInvalidTokenIsUnauthorized() {
        when(tokenService.verificar("abc")).thenReturn(Optional.empty());

        var response = controller.sair("Bearer abc");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(revocation);
    }

    @Test
    void sairWithValidTokenRevokesItsJti() {
        var jwt = mock(DecodedJWT.class);
        when(jwt.getId()).thenReturn("jti-1");
        when(tokenService.verificar("abc")).thenReturn(Optional.of(jwt));

        var response = controller.sair("Bearer abc");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(revocation).revoke("jti-1");
    }

    @Test
    void excluirWithWrongPasswordIsForbiddenAndDeletesNothing() {
        when(encoder.matches("errada", "hash")).thenReturn(false);

        var response = controller.excluir(new AccountController.DeleteAccountDTO("errada"), user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(((CampoErro) response.getBody()).campo()).isEqualTo("senha");
        verify(accountService, never()).delete(7L);
    }

    @Test
    void excluirWithCorrectPasswordDeletesTheAccount() {
        when(encoder.matches("certa", "hash")).thenReturn(true);

        var response = controller.excluir(new AccountController.DeleteAccountDTO("certa"), user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(accountService).delete(7L);
    }
}
