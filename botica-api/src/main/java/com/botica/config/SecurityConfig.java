package com.botica.config;

import com.botica.security.JwtFilter;
import com.botica.security.JwtService;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwt;

    @Value("${app.cors.origins}")
    private List<String> origins;

    @Bean
    SecurityFilterChain chain(HttpSecurity http) throws Exception {
        String admin = "ADMINISTRADOR", emp = "EMPLEADO";
        http
            .csrf(c -> c.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(a -> a
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/auth/**").permitAll()
                // usuarios: solo ADMIN, salvo la lista de clientes (la usa el POS)
                .requestMatchers(HttpMethod.GET, "/usuarios/clientes").hasAnyRole(admin, emp)
                .requestMatchers("/usuarios/**").hasRole(admin)
                // catálogos: lectura para ADMIN y EMPLEADO, escritura solo ADMIN
                .requestMatchers(HttpMethod.GET, "/productos/**", "/laboratorios/**", "/proveedores/**", "/lotes/**")
                    .hasAnyRole(admin, emp)
                .requestMatchers("/productos/**", "/laboratorios/**", "/proveedores/**", "/lotes/**").hasRole(admin)
                // movimientos
                .requestMatchers("/ventas/**", "/compras/**").hasAnyRole(admin, emp)
                .anyRequest().denyAll())
            .addFilterBefore(new JwtFilter(jwt), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(origins);
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
