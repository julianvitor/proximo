# Documentação para Envio de Informações sobre RFID

## Formato do RFID

### Removido

- **Formato**: `removido:UID`
- **Exemplo**: `removido:1A2B3C4D`

**Características**:

- A string começa com a palavra "removido:".
- Segue o UID do cartão RFID. Recomenda-se o uso de formato hexadecimal para padronização.
- Enviado para todos os clientes conectados via WebSocket quando um cartão RFID é removido.

## Descrição dos Campos

### removido

- **removido:** (prefixo) Indica que um cartão RFID foi removido.
- **UID** (identificador único) é uma string que representa o cartão RFID. Embora não seja obrigatório, é recomendado que o UID esteja no formato hexadecimal para garantir padronização.

## Erros Comuns e Tratamento

- **Campo faltando**: Se o prefixo `inserido:` ou `removido:` estiver ausente, a mensagem não será processada corretamente e será considerada inválida.
- **Formato incorreto**: Se o UID não estiver no formato recomendado ou se o prefixo estiver incorreto, o aplicativo pode falhar ao tentar processar a mensagem.
- **Tratamento**: Em caso de erro, o aplicativo ignorará a mensagem malformada e, se necessário, exibirá uma mensagem de erro na interface do usuário.

## FAQ

- **P: O que acontece se eu enviar um UID com caracteres não hexadecimais?**
  - **R**: Embora o uso de caracteres não hexadecimais seja permitido, é recomendado usar formato hexadecimal para padronização. Caracteres não hexadecimais podem fazer com que a mensagem seja ignorada ou cause falha no processamento.

- **P: É possível enviar uma mensagem com um prefixo diferente de "inserido:" ou "removido:"?**
  - **R**: Não. O prefixo deve ser exatamente "inserido:" ou "removido:" para que a mensagem seja reconhecida corretamente pelo aplicativo.

- **P: Posso incluir informações adicionais na mensagem?**
  - **R**: Não. A mensagem deve seguir estritamente o formato especificado para garantir que o aplicativo possa processar a informação corretamente. Informações adicionais podem causar erros.
