package com.catedra.catalogo.security;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

public class RolesJwtAuthenticationConverter extends JwtAuthenticationConverter {

    public RolesJwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");

        setJwtGrantedAuthoritiesConverter(authorities);
        setPrincipalClaimName("sub");
    }
}
