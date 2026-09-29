package com.legrandcinema.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void initialisation() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "cleSecrete", "cle-de-test-uniquement-pour-les-tests-unitaires-0123456789");
        jwtUtil.init();
    }

    @Test
    void genererToken_creeUnTokenNonVide() {
        String token = jwtUtil.genererToken("katia@mail.com", "CLIENT");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void extraireEmail_retourneEmailCorrect() {
        String token = jwtUtil.genererToken("katia@mail.com", "CLIENT");
        assertEquals("katia@mail.com", jwtUtil.extraireEmail(token));
    }

    @Test
    void extraireRole_retourneRoleCorrect() {
        String token = jwtUtil.genererToken("katia@mail.com", "ADMIN");
        assertEquals("ADMIN", jwtUtil.extraireRole(token));
    }

    @Test
    void tokenValide_retourneTruePourTokenValide() {
        String token = jwtUtil.genererToken("katia@mail.com", "CLIENT");
        assertTrue(jwtUtil.tokenValide(token));
    }

    @Test
    void tokenValide_retourneFalsePourTokenInvalide() {
        assertFalse(jwtUtil.tokenValide("ceci.nest.pasUnTokenValide"));
    }
}