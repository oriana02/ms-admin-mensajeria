package com.ticketfilms.msadmin.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class CognitoGruposConverterTest {

    private final CognitoGruposConverter convertidor = new CognitoGruposConverter();

    private Jwt.Builder jwt() {
        return Jwt.withTokenValue("t").header("alg", "none").subject("sub-1");
    }

    @Test
    void gruposDeCognito_seConvierteEnRoles() {
        Jwt token = jwt().claim("cognito:groups", List.of("admin", "clientes")).build();

        Set<String> roles = convertidor.convert(token).stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toSet());

        assertEquals(Set.of("ROLE_admin", "ROLE_clientes"), roles);
    }

    @Test
    void sinClaimDeGrupos_noHayAutoridades() {
        assertTrue(convertidor.convert(jwt().claim("scope", "ticketfilms/api").build()).isEmpty());
    }
}
