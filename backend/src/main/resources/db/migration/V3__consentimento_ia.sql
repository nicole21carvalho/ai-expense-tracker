-- Consentimento para enviar a descricao dos lancamentos ao servico de IA
-- (LGPD, art. 7, I e art. 8). Guarda quando o usuario aceitou: o controlador
-- precisa conseguir provar o consentimento (art. 8, par. 2).
-- NULL = nao consentiu ou revogou. Ninguem comeca consentindo: contas que ja
-- existem ficam com NULL e passam a usar so a sugestao local.
ALTER TABLE usuario ADD COLUMN consentimento_ia_em TIMESTAMP NULL;
