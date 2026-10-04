-- Migracao versionada: o Flyway roda isto uma vez e registra na tabela
-- flyway_schema_history. Nunca edite um arquivo ja aplicado, crie um V2.

CREATE TABLE usuario (
    id          BIGSERIAL PRIMARY KEY,
    nome        VARCHAR(120)  NOT NULL,
    email       VARCHAR(180)  NOT NULL UNIQUE,
    senha_hash  VARCHAR(100)  NOT NULL,
    criado_em   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE categoria (
    id         BIGSERIAL PRIMARY KEY,
    nome       VARCHAR(80) NOT NULL,
    cor        VARCHAR(7)  NOT NULL DEFAULT '#6B7280',
    usuario_id BIGINT      NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    CONSTRAINT uk_categoria_nome_usuario UNIQUE (nome, usuario_id)
);

CREATE TABLE transacao (
    id              BIGSERIAL      PRIMARY KEY,
    descricao       VARCHAR(255)   NOT NULL,
    valor           NUMERIC(14,2)  NOT NULL,
    tipo            VARCHAR(20)    NOT NULL,
    data            DATE           NOT NULL,
    categoria_id    BIGINT         REFERENCES categoria(id) ON DELETE SET NULL,
    usuario_id      BIGINT         NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    categoria_por_ia BOOLEAN       NOT NULL DEFAULT FALSE,
    criado_em       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indice no par que toda consulta de extrato usa: usuario + periodo.
CREATE INDEX idx_transacao_usuario_data ON transacao (usuario_id, data DESC);
