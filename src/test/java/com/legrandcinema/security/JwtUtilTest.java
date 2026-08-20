package com.legrandcinema.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil();

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