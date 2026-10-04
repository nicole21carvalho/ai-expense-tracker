package br.com.nicole.cofre.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF protege formulario com cookie de sessao. Aqui a API e
                // stateless e o token vai no header, entao nao se aplica.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // So o que existe e precisa ser publico. Liberar rota que
                        // nao existe e armadilha: o dia em que alguem ligar o
                        // console do H2 (que executa SQL e ate codigo no servidor)
                        // ele ja nasceria aberto.
                        .requestMatchers("/api/auth/**").permitAll()
                        // Quando algo falha, o Tomcat reencaminha a requisicao para
                        // /error, e esse encaminhamento nao carrega o usuario do
                        // token. Bloqueado, todo erro (JSON malformado, 500) virava
                        // 401 e o front deslogava o usuario. Nao abre rota nova: so
                        // vale para o reencaminhamento interno, nao para quem chama
                        // /error de fora.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .anyRequest().authenticated())
                // Sem isto, o Spring responde 403 a quem nao tem token valido. 403
                // quer dizer "sei quem voce e e nao pode"; aqui o certo e 401, "nao
                // sei quem voce e". O front depende disso para encerrar a sessao
                // quando o token expira.
                .exceptionHandling(e -> e.authenticationEntryPoint(SecurityConfig::naoAutenticado))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private static void naoAutenticado(HttpServletRequest request, HttpServletResponse response,
                                       AuthenticationException e) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        // RFC 6750: um 401 de API com token Bearer indica o esquema esperado.
        response.setHeader("WWW-Authenticate", "Bearer");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // Mesmo formato do TratadorDeErros, para o front tratar um formato so.
        response.getWriter().write("{\"mensagem\":\"Autenticação necessária\",\"detalhes\":[]}");
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        // BCrypt tem custo de calculo proposital. Isso torna ataque de forca
        // bruta caro mesmo se o banco inteiro vazar.
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsSource() {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
