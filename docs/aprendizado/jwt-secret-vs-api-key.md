# `jwt_secret` vs `admin_api_key` — por que dois secrets diferentes

## Contexto da dúvida

A BE-10 introduziu `app.admin.api-key` (chave do operador pra gerar convites). A BE-11 introduziu `app.jwt.secret` (chave de assinatura de JWTs). Os dois são gerados com `openssl rand`. A pergunta foi: pra que serve cada uma e por que precisam ser diferentes.

## Resumo destilado

São **dois secrets com propósitos diferentes**, cada um com seu próprio ciclo de vida.

### `admin_api_key` — "senha" do operador

| Característica | Valor |
|---|---|
| Tipo conceitual | API key (senha estática) |
| Quem conhece | Operador (você) + servidor |
| Trafega na rede? | Sim, em todo request admin (`X-Admin-Key`) |
| Uso pelo servidor | Compara string com o header da request |
| Mínimo de entropia | "Hard to guess" — 32 bytes random é suficiente |
| Custo de rotação | Zero pro usuário final. Só você precisa do novo valor. |
| Endpoint protegido | Só o de gerar convite (`/admin/api/v1/...`) |

### `jwt_secret` — chave criptográfica de assinatura

| Característica | Valor |
|---|---|
| Tipo conceitual | Signing key (chave HMAC) |
| Quem conhece | Só o servidor (nunca sai) |
| Trafega na rede? | Não, nunca |
| Uso pelo servidor | Assina JWTs ao gerar; valida assinatura ao receber |
| Mínimo de entropia | HS256 exige ≥256 bits (32 bytes) por padrão criptográfico |
| Custo de rotação | Alto: todos os JWTs em circulação viram inválidos. Usuários precisam de novo link mágico |
| Endpoint protegido | Indiretamente: todos os `/api/v1/**` que usam JWT como auth (dezenas) |

## Por que TÊM que ser diferentes

### 1. Blast radius (raio de explosão) diferente

- Se `admin_api_key` vaza: atacante gera convites, mas ainda precisa exchange + cada token é single-use + dura 7 dias. Limitado.
- Se `jwt_secret` vaza: atacante forja JWT pra qualquer `requisitante_id`, pula tudo, vira instantaneamente "qualquer usuário". Catastrófico.

Misturar = blast radius sempre o pior dos dois.

### 2. Lifecycle diferente

- `admin_api_key` pode ficar a mesma por anos. Rotação barata.
- `jwt_secret` raramente é rotacionada (pelo custo). Mas se um dia precisar, o custo é proporcional ao número de usuários ativos.

Forçar rotação simultânea (porque é o mesmo valor) gera dor desproporcional.

### 3. Separation of duties

Se você um dia precisar passar uma chave pra uma máquina de monitoramento ou CI, você não quer que essa máquina ganhe poder de forjar JWTs implicitamente. Separando, cada chave dá acesso só à sua função.

## Pontos-chave

- **API key** = "senha de admin", trafega na rede, comparada por igualdade
- **Signing key** = "chave criptográfica", nunca sai do servidor, validada por algoritmo
- **Nunca reutilize a mesma string pra os dois**, mesmo que pareça redundante
- **Cada secret = um valor independente**, gerado independente, rotacionado independente
- **Padrão da indústria** segue isso: AWS (IAM key vs KMS), Stripe (API key vs webhook signing secret), OAuth (client_secret vs JWT signing key) — todos separam

## Analogia

Empresa que opera caixa eletrônico:
- **`admin_api_key`** = chave física do compartimento traseiro do caixa, que o técnico de manutenção usa pra abrir e fazer operações administrativas
- **`jwt_secret`** = chave master da fábrica que produz os cartões de débito dos clientes

Não usa a mesma chave pra os dois porque:
- A chave de manutenção circula entre vários técnicos e várias máquinas
- A chave master fica trancada na fábrica
- Roubar uma não compromete a outra

## Pra aprofundar

- HMAC-SHA256 — algoritmo de assinatura simétrica usado em JWT
- Chaves assimétricas (RS256, ES256) — alternativa onde servidor assina com chave privada e validação pode acontecer fora (em terceiros). Custo maior, ganho de separação de roles.
- Padrão "envelope encryption" do KMS — chave master criptografa data keys
- Por que `openssl rand -base64 N` é uma boa fonte (entropia do `/dev/urandom`)
