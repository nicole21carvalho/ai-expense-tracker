<div align="center">

# 💰 Cofre

**Controle financeiro pessoal com categorização automática de lançamentos por IA.**

Você lança o gasto, o Cofre descobre a categoria.

[![CI](https://github.com/nicole21carvalho/cofre/actions/workflows/ci.yml/badge.svg)](https://github.com/nicole21carvalho/cofre/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?logo=springboot&logoColor=white)
![Angular](https://img.shields.io/badge/Angular-22-DD0031?logo=angular&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)

[O que resolve](#-o-que-resolve) •
[Funcionalidades](#-funcionalidades) •
[Arquitetura](#-arquitetura) •
[Rodando](#-rodando) •
[Endpoints](#-endpoints) •
[Decisões](#-decisões-que-valem-explicar) •
[Privacidade](#-privacidade)

<br>

<img src="docs/img/painel.png" alt="Painel do Cofre com saldo do mês e extrato categorizado" width="820">

</div>

---

## 🎯 O que resolve

Anotar gasto é fácil; classificar gasto é o que ninguém faz. Sem categoria, o extrato vira uma lista sem significado e o usuário abandona o app na segunda semana. O Cofre tenta resolver isso lançando primeiro e perguntando depois: ao salvar uma transação sem categoria, a API sugere uma a partir da descrição.

A sugestão tem duas implementações por trás da mesma interface `Categorizador`. Sem chave de API configurada, roda um classificador por palavra-chave que resolve os casos óbvios e custa zero. Com chave, roda um modelo de linguagem que lida com descrições que regra não pega — `PG *MERC BOM PRECO 04` não contém a palavra "mercado".

Isso é decisão de projeto, não indecisão: a aplicação precisa subir e funcionar na máquina de quem clonou o repositório, sem credencial nenhuma. E a versão por regras serve de linha de base — sem ela não dá para afirmar que o modelo melhora alguma coisa.

A resposta do modelo nunca é gravada direto. Ela é validada contra as categorias que o usuário já tem, e qualquer falha de rede devolve "sem sugestão" em vez de erro: categorização é extra, o lançamento precisa salvar de qualquer jeito.

## ✨ Funcionalidades

- 🏷️ **Categorização automática** — por regras locais ou por modelo de linguagem, com a marca *sugerido* no extrato
- ✏️ **Correção manual** — trocar a categoria desmarca a sugestão da IA
- 📊 **Resumo do mês** — receitas, despesas, saldo e gasto por categoria
- 🔐 **Autenticação JWT** com limite de tentativas no login
- 🛡️ **Consentimento LGPD** — nada sai da aplicação sem o usuário permitir
- 📱 **Responsivo** — funciona no celular

<div align="center">
<table>
  <tr>
    <td align="center"><img src="docs/img/login.png" alt="Tela de login" width="520"><br><sub>Login</sub></td>
    <td align="center"><img src="docs/img/painel-mobile.png" alt="Painel no celular" width="200"><br><sub>Celular</sub></td>
  </tr>
</table>
</div>

## 🧱 Stack

| Camada | Tecnologias |
| --- | --- |
| **Front-end** | Angular 22 · TypeScript · RxJS · Signals |
| **Back-end** | Java 21 · Spring Boot 4.1 · Spring Security · JPA |
| **Dados** | PostgreSQL 16 · Flyway |
| **Infra** | Docker · Docker Compose · nginx · GitHub Actions |

## 🏗️ Arquitetura

```mermaid
flowchart LR
    U([Navegador]) -->|:4200| N[nginx<br/>Angular]
    N -->|/api| A[API<br/>Spring Boot]
    A --> D[(PostgreSQL)]
    A --> C{Categorizador}
    C -->|sem chave ou<br/>sem consentimento| R[Regras por<br/>palavra-chave]
    C -->|com chave e<br/>consentimento| L[Modelo de<br/>linguagem]
```

A API não é publicada no host: todo acesso passa pelo nginx, que é quem informa à API o IP real do cliente para o limite de tentativas do login.

## 🚀 Rodando

Com Docker, sobe tudo:

```bash
git clone https://github.com/nicole21carvalho/cofre.git
cd cofre
cp .env.example .env
# Preencha no .env:
#   COFRE_JWT_SECRET, por exemplo com: openssl rand -base64 48
#   DB_PASSWORD,      por exemplo com: openssl rand -hex 24
docker compose up --build
```

A interface fica em `http://localhost:4200`, e a API responde no mesmo endereço, em `http://localhost:4200/api`.

<details>
<summary><b>Só a API, sem Docker (H2 em memória)</b></summary>

```bash
cd backend
export COFRE_JWT_SECRET="$(openssl rand -base64 48)"
mvn spring-boot:run
```

</details>

> [!IMPORTANT]
> A chave do JWT não tem valor padrão de propósito: sem `COFRE_JWT_SECRET` (mínimo de 32 bytes), a API se recusa a subir. Um padrão no código seria uma chave pública, e com ela qualquer pessoa assinaria tokens válidos.

> [!TIP]
> Para ativar a categorização por modelo, preencha `COFRE_IA_API_KEY` no `.env`. Sem isso, o classificador por regras assume.

## 🔌 Endpoints

| Método | Rota | O que faz |
| --- | --- | --- |
| `POST` | `/api/auth/cadastro` | Cria conta e já semeia categorias padrão |
| `POST` | `/api/auth/login` | Devolve o JWT |
| `GET` | `/api/transacoes` | Extrato paginado, filtro opcional por período |
| `POST` | `/api/transacoes` | Lança; sem `categoriaId`, pede sugestão |
| `PUT` | `/api/transacoes/{id}` | Edita; corrigir a categoria desmarca a flag de IA |
| `DELETE` | `/api/transacoes/{id}` | Remove |
| `GET` | `/api/transacoes/resumo` | Receitas, despesas, saldo e gasto por categoria |
| `GET` | `/api/categorias` | Lista as do usuário |
| `GET` | `/api/privacidade` | Diz se há IA configurada e se o usuário consentiu |
| `PUT` | `/api/privacidade/consentimento-ia` | Concede (`{"aceito": true}`) ou revoga o consentimento |

Todas as rotas abaixo de `/api`, exceto cadastro e login, exigem `Authorization: Bearer <token>`.

## 🧠 Decisões que valem explicar

**JWT em vez de sessão.** Sessão exige estado no servidor, e com mais de uma instância atrás de um balanceador cada requisição pode cair numa instância que não a conhece. O custo é não conseguir invalidar um token antes da hora — por isso a expiração é curta.

**O id do dono vem do token, nunca do corpo da requisição.** Se viesse do corpo, trocar um número bastaria para ler o extrato de outra pessoa. Todo método de repositório filtra por `usuario_id`.

**`BigDecimal` para dinheiro.** `double` acumula erro de arredondamento e vira divergência de centavos no fechamento.

**Flyway em vez de `ddl-auto: update`.** Schema versionado em arquivo é schema que dá para revisar em pull request e reproduzir em qualquer ambiente.

**CI com dependências travadas.** As actions são fixadas por SHA do commit, não por tag, e o pipeline roda testes, build e varredura de vulnerabilidades conhecidas (osv-scanner) antes de construir as imagens.

## 🔒 Privacidade

Com a categorização por modelo configurada, a descrição dos lançamentos sem categoria e os nomes das categorias do usuário vão para um serviço externo. Por isso o envio depende de consentimento explícito de cada usuário (LGPD, art. 7º, I):

- Ninguém começa consentindo. Até o usuário clicar em **Permitir** no painel, a sugestão vem das regras locais e nada sai da aplicação.
- O aviso diz o que é enviado, o que não é (valor, data, dados da conta) e para quem (`cofre.ia.provedor`).
- A data do consentimento fica gravada (`usuario.consentimento_ia_em`), porque cabe ao controlador provar que ele foi dado (art. 8º, §2º).
- Desativar fica no mesmo lugar e com o mesmo destaque que permitir (art. 8º, §5º). Depois disso, nenhuma descrição nova é enviada.

Sem chave de IA configurada, o modo por regras roda sozinho, não envia nada para fora e o painel nem mostra o pedido.

Isso cobre o consentimento na aplicação. Em uso real ainda faltariam a política de privacidade e a avaliação do contrato com o provedor, que tratam do que ele faz com os dados recebidos.

## 📚 O que aprendi

**Spring Security foi a parte mais difícil.** Ele funciona como uma cadeia de filtros que roda antes do controller, e quase tudo que dá errado ali aparece como um status HTTP que não explica a causa. Dois casos me ensinaram a ler essa cadeia:

- *Sem token, a API respondia 403.* Esse é o padrão do Spring, mas 403 quer dizer "sei quem você é e você não pode"; o certo é 401, "não sei quem você é". Como o front encerra a sessão ao receber 401, foi preciso um `authenticationEntryPoint` próprio.
- *Todo erro virava logout.* Qualquer falha na API, de um JSON malformado a um erro 500, deslogava o usuário. A causa estava longe do sintoma: quando algo falha, o Tomcat reencaminha a requisição para `/error`, esse reencaminhamento não carrega o usuário do token, e o Spring Security respondia 401. A correção foi liberar só o reencaminhamento interno (`DispatcherType.ERROR`), sem abrir `/error` para quem chama de fora.

Aprendi a seguir a requisição pelo caminho inteiro, filtro por filtro, em vez de mexer onde o problema aparece.

**Migration aplicada não se edita.** As categorias padrão nasceram sem acento ("Alimentacao"). Corrigir o `V1` não adiantava, porque quem já tinha banco não rodaria a migration de novo, então a correção virou um `V2`. E o `UPDATE` óbvio quebrava para quem já tinha criado "Alimentação" à mão, por causa da chave única. Escrevi um teste que monta um banco no estado do `V1`, com esse usuário dentro, antes de confiar na migration.

**`depends_on` não espera o banco ficar pronto.** A API subia antes de o Postgres aceitar conexão e morria no Flyway. `depends_on` só espera o container *iniciar*; foi preciso um `healthcheck` com `pg_isready` e `condition: service_healthy`.

**Porta publicada é porta aberta.** Com a API exposta no host, quem chamasse direto podia forjar o `X-Forwarded-For` e driblar o limite de tentativas do login, que conta tentativas por IP. Tirei a porta da API: agora todo acesso passa pelo nginx, que é quem informa o IP real.

**Nem toda atualização é melhoria.** No primeiro push, o Dependabot abriu sete PRs. Três entraram; os outros trocavam a versão do Java ou do Node só em uma das imagens, ou subiam o TypeScript para uma versão que o Angular ainda não aceita. Atualizar dependência também é decisão de projeto, e o CI é o que permite tomar essa decisão com segurança.

---

<div align="center">

Feito por **Nicole de Carvalho** · [GitHub](https://github.com/nicole21carvalho)

</div>
