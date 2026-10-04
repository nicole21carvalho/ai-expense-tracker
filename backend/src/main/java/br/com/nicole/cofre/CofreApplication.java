package br.com.nicole.cofre;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// Sem o exclude, o Spring cria um usuario "user" em memoria e escreve a
// senha dele no log a cada subida. Aqui ninguem usa: a autenticacao e so
// por JWT. Mas a senha ia parar em qualquer lugar que guarde log.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class CofreApplication {
    public static void main(String[] args) {
        SpringApplication.run(CofreApplication.class, args);
    }
}
