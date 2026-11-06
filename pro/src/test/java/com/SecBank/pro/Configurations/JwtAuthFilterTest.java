package com.SecBank.pro.Configurations;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

class JwtAuthFilterTest {

    /**
     * Le renvoi ASYNC qui termine un flux (export CSV) repasse dans la chaine de
     * securite. S'il n'est pas re-authentifie, Spring Security le refuse apres
     * commit et la reponse est tronquee (ERR_INCOMPLETE_CHUNKED_ENCODING).
     */
    @Test
    void reauthentifieLesRenvoisAsync() {
        JwtAuthFilter filtre = new JwtAuthFilter(mock(JwtService.class));

        assertFalse(filtre.shouldNotFilterAsyncDispatch(),
                "le renvoi async doit re-authentifier pour ne pas tronquer les flux");
    }
}
