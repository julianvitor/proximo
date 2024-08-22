# Documentação para Envio de Informações sobre RFID

### removido

A resposta do filho deve ser no seguinte formato:

```json
{
  "removed": {
    "rfid": "041a2b3c4d5e6f",
    "station_mac": "AA:BB:CC:DD:EE:FF"
  },
  "message_id": "12345678"
}

```


### Descrição dos Campos

- **removed** (objeto): Contém dados específicos da operação realizada
- **rfid**(string): Identificador RFID da máquina sempre em lowercase e sem dois pontos.
- **station_mac**(string):  Endereço MAC da estação(filho).
- **message_id** (string): Identificador único para a confirmação da solicitação.


## Erros Comuns e Tratamento

- **Campo faltando**: se qualquer campo com exceção de message_id que é usado para confirmação estiver faltando a operação será ignorada.
- **Formato incorreto**: Se o UID não estiver no formato recomendado ou se o prefixo estiver incorreto, o aplicativo pode falhar ao tentar processar a mensagem.
- **Tratamento**: Em caso de erro, o aplicativo ignorará a mensagem malformada e, se necessário, exibirá uma mensagem de erro no Logcat

## FAQ

- **P: O que acontece se eu enviar um UID com caracteres não hexadecimais?**
  - **R**: Embora o uso de caracteres não hexadecimais seja permitido, é recomendado usar formato hexadecimal para padronização. Caracteres não hexadecimais podem fazer com que a mensagem seja ignorada ou cause falha no processamento.

- **P: É possível enviar uma mensagem com um prefixo diferente de "inserido:" ?**
  - **R**: Não. O prefixo deve ser exatamente "inserido:" ou "removido:" para que a mensagem seja reconhecida corretamente pelo aplicativo.

- **P: Posso incluir informações adicionais na mensagem?**
  - **R**: Sim, mas não é recomendado, chaves adicionais serão ignoradas, mas isso prejudica a performance.
