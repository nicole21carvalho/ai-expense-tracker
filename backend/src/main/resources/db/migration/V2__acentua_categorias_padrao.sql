-- Acentua os nomes das categorias padrao que o cadastro criava sem acento.
-- O V1 ja foi aplicado em quem tem banco, por isso a mudanca vem num V2.
--
-- O NOT EXISTS pula o usuario que ja tem a versao acentuada (criada a mao):
-- renomear violaria uk_categoria_nome_usuario. Nesse caso a antiga fica, e
-- o categorizador por regras compara sem acento, entao as duas funcionam.

UPDATE categoria c SET nome = 'Alimentação'
 WHERE c.nome = 'Alimentacao'
   AND NOT EXISTS (SELECT 1 FROM categoria o WHERE o.usuario_id = c.usuario_id AND o.nome = 'Alimentação');

UPDATE categoria c SET nome = 'Saúde'
 WHERE c.nome = 'Saude'
   AND NOT EXISTS (SELECT 1 FROM categoria o WHERE o.usuario_id = c.usuario_id AND o.nome = 'Saúde');

UPDATE categoria c SET nome = 'Educação'
 WHERE c.nome = 'Educacao'
   AND NOT EXISTS (SELECT 1 FROM categoria o WHERE o.usuario_id = c.usuario_id AND o.nome = 'Educação');

UPDATE categoria c SET nome = 'Salário'
 WHERE c.nome = 'Salario'
   AND NOT EXISTS (SELECT 1 FROM categoria o WHERE o.usuario_id = c.usuario_id AND o.nome = 'Salário');
