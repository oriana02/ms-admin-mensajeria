package com.ticketfilms.msadmin.security;

import java.util.Collection;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

// Cognito pone los grupos del usuario en el claim "cognito:groups".
// Cada grupo se convierte en la autoridad ROLE_<grupo> (ej. ROLE_admin).
public class CognitoGruposConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    public static final String CLAIM_GRUPOS = "cognito:groups";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<String> grupos = jwt.getClaimAsStringList(CLAIM_GRUPOS);
        if (grupos == null) {
            return List.of();
        }
        return grupos.stream()
                .<GrantedAuthority>map(g -> new SimpleGrantedAuthority("ROLE_" + g))
                .toList();
    }
}
