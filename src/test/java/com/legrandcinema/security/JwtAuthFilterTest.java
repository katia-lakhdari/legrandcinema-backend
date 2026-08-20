package com.legrandcinema.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    private JwtUtil jwtUtil;
    private JwtAuthFilter jwtAuthFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void initialisation() {
        jwtUtil = new JwtUtil();
        jwtAuthFilter = new JwtAuthFilter(jwtUtil);
    }

    @AfterEach
    void nettoyage() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_sansEnTete_neAuthentifiePas() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_avecTokenValide_authentifieUtilisateur() throws Exception {
        String token = jwtUtil.genererToken("katia@legrandcinema.com", "CLIENT");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentification);
        assertEquals("katia@legrandcinema.com", authentification.getPrincipal());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_avecTokenInvalide_neAuthentifiePas() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer tokenInvalide");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}