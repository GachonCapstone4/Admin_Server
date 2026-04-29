package com.emailagent.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Chrome의 Private Network Access(PNA) 정책 대응 필터.
 * admin.studylink.click → 192.168.2.100(사설 IP) 구조에서 Chrome은 preflight 시
 * Access-Control-Request-Private-Network: true 를 함께 전송한다.
 * 서버가 Access-Control-Allow-Private-Network: true 를 응답에 포함하지 않으면 브라우저가 차단한다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PrivateNetworkAccessFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        response.addHeader("Access-Control-Allow-Private-Network", "true");
        filterChain.doFilter(request, response);
    }
}
