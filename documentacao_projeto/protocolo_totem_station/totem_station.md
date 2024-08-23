# Comm protocol for totem <--> station

## Activate all relays from all stations
```json
{"command": "activate_all"}
```
## Activate relay based on present rfid
```json
{
    "command": "activate",
    "rfid": "041a2b3c4d5e6f",  
    "message_id": "12345678"
}
```
***

## Insertion

Station response should contains the following:

```json
{
  "inserted": {
    "rfid": "041a2b3c4d5e6f",
    "station_mac": "AA:BB:CC:DD:EE:FF"
  },
  "message_id": "12345678"
}

```

### Descrição dos Campos

- **inserted** (objeto): Contém dados específicos da operação realizada
- **rfid**(string): Identificador RFID da máquina, deve ser sempre lowercase e sem dois pontos mesmo sendo hexadecimal.
- **station_mac**(string):  Endereço MAC da estação(filho) sempre em uppercase e com dois pontos.
- **message_id** (string): Identificador único para a confirmação da solicitação.


## Erros Comuns e Tratamento

- **Campo faltando**: se qualquer campo com exceção de message_id que é usado para confirmação estiver faltando a operação será ignorada.
- **Formato incorreto**: Se o UID não estiver no formato recomendado ou se o prefixo estiver incorreto, o aplicativo pode falhar ao tentar processar a mensagem.
- **Tratamento**: Em caso de erro, o aplicativo ignorará a mensagem malformada e, se necessário, exibirá uma mensagem de erro no Logcat

## FAQ

- **P: É possível enviar uma mensagem com um prefixo diferente de "inserted:" ?**
  - **R**: Não. O prefixo deve ser exatamente "inserted:" ou "removed:" para que a mensagem seja reconhecida corretamente pelo aplicativo.

- **P: Posso incluir informações adicionais na mensagem?**
  - **R**: Sim, mas não é recomendado, chaves adicionais serão ignoradas, mas isso prejudica a performance.

***


### Log request and response

Totem request should contains the following:

```	json
{"accio_log":{},"message_id": "12345678"}

```

Station response should contains the following:

```json
{
    "response_log": {
        "device_info": {
            "station_mac": "00:1A:2B:3C:4D:5E",
            "ip_address": "192.168.1.10"
        },
        "system_status": {
            "core_temperature": 65.3,
            "uptime": "0 days 0 hours 4 minutes"
        },
        "pn532": {
            "version": "83254",
            "status": "OK",
            "rfid":"041a2b3c4d5e6f"
        }
    },
    "message_id": "12345678"
}
```


### Descrição dos Campos

- **log** (objeto): O objeto principal contendo os dados do log.
    - **times_tamp** (string): A data e hora em que o log foi criado, no formato ISO 8601.
    - **device_info** (objeto): Informações sobre o dispositivo que enviou o log.
        - **station_mac** (string): Endereço MAC do dispositivo em uppercase e com dois pontos.
        - **ip_address** (string): Endereço IP do dispositivo em formato IPv4.
    - **system_status** (objeto): Status atual do sistema.
        - **core_temperature** (float): Temperatura do núcleo em graus Celsius.
        - **uptime** (string): Tempo de atividade do sistema.
    - **pn532_firmware** (objeto): Informações sobre o firmware PN532.
        - **version** (string): Versão do firmware.
        - **status** (string): Status atual do firmware (ex: "OK", "Error").
    - **message_id** (string): Identificador único para a confirmação da solicitação.

### Erros Comuns e Tratamento

- **Campo faltando**: Se um campo obrigatório estiver ausente, o aplicativo não conseguirá processar o log corretamente e retornará um erro genérico no Logcat.
- **Formato incorreto**: Se um campo não estiver no formato correto (por exemplo, uma string no lugar de um número), o aplicativo pode falhar ao tentar exibir o log.
- **Tratamento**: Em caso de erro, o aplicativo exibirá uma mensagem de erro genérica na interface e ignorará o log malformado.

### FAQ

**P:** O que acontece se eu enviar um campo adicional não documentado?
**R:** Campos adicionais serão ignorados pelo aplicativo, mas recomendamos seguir o formato especificado para evitar confusões.

**P:** Posso omitir campos opcionais?
**R:** Sim, campos opcionais podem ser omitidos sem problemas, mas campos obrigatórios devem estar presentes.

*** 

### Accio_machine

Totem request should contains the following:

```json	
{"accio_machine":{},"message_id":"12345678"}
```

Station response should contains the following:

```json
{
    "accio_machine_response": {
        "rfid": "041a2b3c4d5e6f",
        "station_mac": "cafecafecafe"
    },
    "message_id": "12345678"
}
```
minify version of **station response**

```json
{"accio_machine_response": {"rfid": "041a2b3c4d5e6f","station_mac": "cafecafecafe"},"message_id": "12345678"}
```

### Request

- **accio_machine** (objeto):  Objeto principal para a solicitação. Este campo deve ser vazio.
- **message_id** (string): Identificador único para a confirmação da solicitação.

#### Response 
- **accio_machine_response** (objeto): Objeto principal da resposta.
- **rfid** (string): UID de RFID do dispositivo sempre em lowercase e sem dois pontos.
- **station_mac** (string): Identificador único associado ao filho.
- **message_id** (string): Identificador único para a confirmação da solicitação.

### Erros Comuns e Tratamento

- **Campo faltando**: Se um campo obrigatório estiver ausente, o aplicativo não conseguirá processar o log corretamente e retornará um erro genérico no Logcat.
- **Formato incorreto**: Se um campo não estiver no formato correto (por exemplo, uma string no lugar de um número), o aplicativo pode falhar ao tentar exibir o log.
- **Tratamento**: Em caso de erro, o aplicativo exibirá uma mensagem de erro genérica na interface e ignorará o log malformado.

### FAQ

**P:** O que acontece se eu enviar um campo adicional não documentado?
**R:** Campos adicionais serão ignorados pelo aplicativo, mas evite para melhor performance e entendimento do codigo.

**P:** Posso omitir campos opcionais?
**R:** Sim, campos opcionais podem ser omitidos, mas os campos message_id, station_mac e rfid devem estar presentes.

***
## Message confirmation
```json
{
    "confimation": {
        "message_id": "12345678"  
    } 
}
```
***
## Removed

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
