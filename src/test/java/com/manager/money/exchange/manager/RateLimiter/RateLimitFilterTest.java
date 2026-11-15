package com.manager.money.exchange.manager.RateLimiter;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private final RateLimitFilter filter = new RateLimitFilter(List.of("127.0.0.1", "::1"), "X-Forwarded-For");

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest request(String method, String uri, String remoteAddr) {
        var request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    private MockHttpServletResponse send(HttpServletRequest request, MockFilterChain chain) throws Exception {
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    // ---- IP do cliente ----

    @Test
    void ignoresIpHeaderWhenConnectionIsNotFromATrustedProxy() {
        var request = request("POST", "/login", "198.51.100.7");
        request.addHeader("X-Forwarded-For", "1.2.3.4");

        assertThat(filter.clientIp(request)).isEqualTo("198.51.100.7");
    }

    @Test
    void usesHeaderWhenConnectionIsFromATrustedProxy() {
        var request = request("POST", "/login", "127.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.9");

        assertThat(filter.clientIp(request)).isEqualTo("203.0.113.9");
    }

    @Test
    void ignoresEntriesWrittenByTheClientOnTheLeft() {
        var request = request("POST", "/login", "127.0.0.1");
        request.addHeader("X-Forwarded-For", "6.6.6.6, 203.0.113.9");

        assertThat(filter.clientIp(request)).isEqualTo("203.0.113.9");
    }

    @Test
    void skipsTrustedProxiesOnTheRight() {
        var custom = new RateLimitFilter(List.of("127.0.0.1", "10.0.0.1"), "X-Forwarded-For");
        var request = request("POST", "/login", "127.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.1");

        assertThat(custom.clientIp(request)).isEqualTo("203.0.113.9");
    }

    @Test
    void readsIpWithPortFromACustomHeader() {
        var custom = new RateLimitFilter(List.of("127.0.0.1"), "Tailscale-Ingress-Src");
        var request = request("POST", "/login", "127.0.0.1");
        request.addHeader("Tailscale-Ingress-Src", "203.0.113.9:4455");

        assertThat(custom.clientIp(request)).isEqualTo("203.0.113.9");
    }

    @Test
    void fallsBackToRemoteAddrWhenHeaderIsMissingOrMalformed() {
        var missing = request("POST", "/login", "127.0.0.1");
        var malformed = request("POST", "/login", "127.0.0.1");
        malformed.addHeader("X-Forwarded-For", "nao-e-ip");

        assertThat(filter.clientIp(missing)).isEqualTo("127.0.0.1");
        assertThat(filter.clientIp(malformed)).isEqualTo("127.0.0.1");
    }

    @Test
    void treatsLongIpv6LoopbackAsTrusted() {
        var request = request("POST", "/login", "0:0:0:0:0:0:0:1");
        request.addHeader("X-Forwarded-For", "203.0.113.9");

        assertThat(filter.clientIp(request)).isEqualTo("203.0.113.9");
    }

    @Test
    void bucketIpGroupsIpv6ByTheNetworkPrefix() {
        assertThat(RateLimitFilter.bucketIp("2001:db8:1:2:3:4:5:6")).isEqualTo("2001:0db8:0001:0002::/64");
        assertThat(RateLimitFilter.bucketIp("2001:db8:1:2:aaaa:bbbb:cccc:dddd"))
                .isEqualTo(RateLimitFilter.bucketIp("2001:db8:1:2:3:4:5:6"));
        assertThat(RateLimitFilter.bucketIp("203.0.113.9")).isEqualTo("203.0.113.9");
    }

    // ---- Limites ----

    @Test
    void blocksAfterTheRouteCapacityAndSetsRetryAfter() throws Exception {
        for (int i = 0; i < 3; i++) { // /login/cadastro: 3 por 5 min
            var chain = new MockFilterChain();
            var response = send(request("POST", "/login/cadastro", "198.51.100.7"), chain);
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(chain.getRequest()).isNotNull();
        }

        var chain = new MockFilterChain();
        var response = send(request("POST", "/login/cadastro", "198.51.100.7"), chain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isNotNull();
        assertThat(response.getContentAsString()).contains("Muitas requisições");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void otherIpsHaveTheirOwnBucket() throws Exception {
        for (int i = 0; i < 3; i++) send(request("POST", "/login/cadastro", "198.51.100.7"), new MockFilterChain());

        var response = send(request("POST", "/login/cadastro", "198.51.100.8"), new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void trailingSlashSharesTheSameBucket() throws Exception {
        for (int i = 0; i < 3; i++) send(request("POST", "/login/cadastro/", "198.51.100.7"), new MockFilterChain());

        var response = send(request("POST", "/login/cadastro", "198.51.100.7"), new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void optionsRequestsAreNeverLimited() throws Exception {
        for (int i = 0; i < 10; i++) {
            var chain = new MockFilterChain();
            var response = send(request("OPTIONS", "/login/cadastro", "198.51.100.7"), chain);
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(chain.getRequest()).isNotNull();
        }
    }

    @Test
    void rejectsDeclaredBodyAbove8Kb() throws Exception {
        var request = request("POST", "/login", "198.51.100.7");
        request.setContent(new byte[9000]);
        var chain = new MockFilterChain();

        var response = send(request, chain);

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(chain.getRequest()).isNull();
    }
}
