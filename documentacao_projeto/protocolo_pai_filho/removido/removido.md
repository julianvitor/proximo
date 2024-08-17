# Documentação para Envio de Informações sobre RFID

### removido

A resposta do filho deve ser no seguinte formato:

```json
{
    "report": {
        "removido": "04:1A:2B:3C:4D:5E:6F",  // tag rfid da maquina
    },
    "requestId": "12345678"// ID para confirmação
}

```


### Descrição dos Campos

- **report** (objeto): O objeto principal do evento.
- **removido** (string): Identificador único, indica que um novo cartão RFID foi detectado e removido.
- **requestId** (string): Identificador único para a confirmação da solicitação.


## Erros Comuns e Tratamento

- **Campo faltando**: Se `removido:` estiver ausente, a mensagem não será processada corretamente e será considerada inválida.
- **Formato incorreto**: Se o UID não estiver no formato recomendado ou se o prefixo estiver incorreto, o aplicativo pode falhar ao tentar processar a mensagem.
- **Tratamento**: Em caso de erro, o aplicativo ignorará a mensagem malformada e, se necessário, exibirá uma mensagem de erro no Logcat

## FAQ

- **P: O que acontece se eu enviar um UID com caracteres não hexadecimais?**
  - **R**: Embora o uso de caracteres não hexadecimais seja permitido, é recomendado usar formato hexadecimal para padronização. Caracteres não hexadecimais podem fazer com que a mensagem seja ignorada ou cause falha no processamento.

- **P: É possível enviar uma mensagem com um prefixo diferente de "inserido:" ?**
  - **R**: Não. O prefixo deve ser exatamente "inserido:" ou "removido:" para que a mensagem seja reconhecida corretamente pelo aplicativo.

- **P: Posso incluir informações adicionais na mensagem?**
  - **R**: Sim, mas não é recomendado, chaves adicionais serão ignoradas, mas isso prejudica a performance.
