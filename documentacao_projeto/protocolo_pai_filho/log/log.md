### Formato do JSON

A quequisição do pai deve ser no seguinte formato:

```json
{
    "accio_log": {},
    "message_id": "12345678"  
}

{"accio_log":{},"message_id": "12345678"}

```

A resposta do filho deve ser no seguinte formato:

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
            "version": "1.6.8",
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
