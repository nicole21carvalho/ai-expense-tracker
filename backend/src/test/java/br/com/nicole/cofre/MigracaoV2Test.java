package br.com.nicole.cofre;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simula um banco que ja estava na V1, com categorias sem acento, recebendo
 * a V2. Sem Spring: o contexto aplicaria todas as migracoes de uma vez e nao
 * haveria dado antigo para renomear.
 */
class MigracaoV2Test {

    private static final String URL = "jdbc:h2:mem:migracao_v2;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";

    @Test
    @DisplayName("V2 acentua as categorias padrao e respeita quem ja tinha a versao acentuada")
    void acentuaCategoriasAntigas() throws Exception {
        flyway().target("1").load().migrate();

        try (Connection c = DriverManager.getConnection(URL, "sa", "");
             Statement s = c.createStatement()) {
            s.execute("INSERT INTO usuario (id, nome, email, senha_hash) VALUES (1, 'Ana', 'ana@x.com', 'h'), (2, 'Bia', 'bia@x.com', 'h')");
            // Ana: conta antiga comum.
            s.execute("INSERT INTO categoria (nome, usuario_id) VALUES ('Alimentacao', 1), ('Saude', 1), ('Educacao', 1), ('Salario', 1), ('Lazer', 1)");
            // Bia: alem da antiga, ja criou "Saúde" a mao. Renomear violaria a chave unica.
            s.execute("INSERT INTO categoria (nome, usuario_id) VALUES ('Saude', 2), ('Saúde', 2)");
        }

        flyway().load().migrate();

        assertThat(categorias(1)).containsExactlyInAnyOrder("Alimentação", "Saúde", "Educação", "Salário", "Lazer");
        assertThat(categorias(2)).containsExactlyInAnyOrder("Saude", "Saúde");
    }

    private static org.flywaydb.core.api.configuration.FluentConfiguration flyway() {
        return Flyway.configure().dataSource(URL, "sa", "").locations("classpath:db/migration");
    }

    private static List<String> categorias(long usuarioId) throws Exception {
        List<String> nomes = new ArrayList<>();
        try (Connection c = DriverManager.getConnection(URL, "sa", "");
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT nome FROM categoria WHERE usuario_id = " + usuarioId)) {
            while (rs.next()) nomes.add(rs.getString(1));
        }
        return nomes;
    }
}
