package com.manager.money.exchange.manager.security;

import com.manager.money.exchange.manager.Repository.UserRepository;
import com.manager.money.exchange.manager.User.User;
import com.manager.money.exchange.manager.security.token.TokenRevocation;
import com.manager.money.exchange.manager.security.token.TokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityFilterTest {

    private TokenService tokenService;
    private TokenRevocation revocation;
    private UserRepository repository;
    private SecurityFilter filter;
    private User user;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", "test-secret-test-secret-test-secret");
        revocation = new TokenRevocation();
        repository = mock(UserRepository.class);
        filter = new SecurityFilter(tokenService, repository, revocation);

        user = mock(User.class);
        when(user.getLogin()).thenReturn("ana");
        when(user.getId()).thenReturn(7L);
        when(repository.findByLogin("ana")).thenReturn(Optional.of(user));
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private MockFilterChain run(String authorization) throws Exception {
        var request = new MockHttpServletRequest();
        if (authorization != null) request.addHeader("Authorization", authorization);
        var chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return chain;
    }

    @Test
    void validTokenAuthenticatesTheUser() throws Exception {
        var chain = run("Bearer " + tokenService.gerarToken(user));

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isSameAs(user);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void revokedTokenIsNotAuthenticated() throws Exception {
        var token = tokenService.gerarToken(user);
        revocation.revoke(tokenService.verificar(token).orElseThrow().getId());

        var chain = run("Bearer " + token);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void tokenFromDeletedAndRecreatedLoginIsNotAuthenticated() throws Exception {
        var token = tokenService.gerarToken(user);
        var recreated = mock(User.class);
        when(recreated.getId()).thenReturn(8L);
        when(repository.findByLogin("ana")).thenReturn(Optional.of(recreated));

        run("Bearer " + token);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void tokenOfUnknownUserIsNotAuthenticated() throws Exception {
        var token = tokenService.gerarToken(user);
        when(repository.findByLogin("ana")).thenReturn(Optional.empty());

        run("Bearer " + token);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void invalidTokenIsNotAuthenticated() throws Exception {
        run("Bearer lixo");

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void missingOrNonBearerHeaderIsNotAuthenticated() throws Exception {
        var chain = run(null);
        run("Basic abc");

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }
}
