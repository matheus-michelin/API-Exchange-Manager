package com.manager.money.exchange.manager.security;

import com.manager.money.exchange.manager.Repository.MoneyInfoRepository;
import com.manager.money.exchange.manager.Repository.UserRepository;
import com.manager.money.exchange.manager.security.token.TokenRevocation;
import com.manager.money.exchange.manager.security.token.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    //Autowired
    private final TokenService tokenService;

    //Autowired
    private final UserRepository repository;

    //Autowired
    private final TokenRevocation revocation;

    public SecurityFilter(TokenService tokenService, UserRepository repository, TokenRevocation revocation) {
        this.tokenService = tokenService;
        this.repository = repository;
        this.revocation = revocation;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            tokenService.verificar(header.substring(7))
                    .filter(jwt -> !revocation.isRevoked(jwt.getId())) // token cancelado no "Sair"
                    .flatMap(jwt -> repository.findByLogin(jwt.getSubject())
                            // o id do token tem de ser o do usuário atual: cobre conta excluída e login recriado
                            .filter(u -> u.getId() != null && u.getId().equals(jwt.getClaim("uid").asLong())))
                    .ifPresent(user -> SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())));
        }
        chain.doFilter(request, response);
    }
}
