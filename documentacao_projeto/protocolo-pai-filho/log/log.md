### Formato do JSON

O JSON deve ser enviado no seguinte formato:

```json
{
    "log": {
        "timestamp": "2024-08-10T14:32:00Z",  // ISO 8601 format
        "deviceInfo": {
            "macAddress": "00:1A:2B:3C:4D:5E", // String no formato XX:XX:XX:XX:XX:XX
            "ipAddress": "192.168.1.10"        // Endereço IP em formato IPv4
        },
        "systemStatus": {
            "coreTemperature": 65.3,           // Temperatura do núcleo em graus Celsius (float)
            "uptime": "3 days 14 hours 32 minutes" // Tempo de atividade (string)
        },
        "pn532Firmware": {
            "version": "1.6.8",                // Versão do firmware (string)
            "status": "OK"                     // Status do firmware (string)
        }
    }
}

```
### Descrição dos Campos

- **log** (objeto): O objeto principal contendo os dados do log.
    - **timestamp** (string): A data e hora em que o log foi criado, no formato ISO 8601.
    - **deviceInfo** (objeto): Informações sobre o dispositivo que enviou o log.
        - **macAddress** (string): Endereço MAC do dispositivo, no formato XX:XX:XX:XX:XX:XX.
        - **ipAddress** (string): Endereço IP do dispositivo em formato IPv4.
    - **systemStatus** (objeto): Status atual do sistema.
        - **coreTemperature** (float): Temperatura do núcleo em graus Celsius.
        - **uptime** (string): Tempo de atividade do sistema.
    - **pn532Firmware** (objeto): Informações sobre o firmware PN532.
        - **version** (string): Versão do firmware.
        - **status** (string): Status atual do firmware (ex: "OK", "Error").

### Erros Comuns e Tratamento

- **Campo faltando**: Se um campo obrigatório estiver ausente, o aplicativo não conseguirá processar o log corretamente e retornará um erro genérico no Logcat.
- **Formato incorreto**: Se um campo não estiver no formato correto (por exemplo, uma string no lugar de um número), o aplicativo pode falhar ao tentar exibir o log.
- **Tratamento**: Em caso de erro, o aplicativo exibirá uma mensagem de erro genérica na interface e ignorará o log malformado.

### FAQ

**P:** O que acontece se eu enviar um campo adicional não documentado?
**R:** Campos adicionais serão ignorados pelo aplicativo, mas recomendamos seguir o formato especificado para evitar confusões.

**P:** Posso omitir campos opcionais?
**R:** Sim, campos opcionais podem ser omitidos sem problemas, mas campos obrigatórios devem estar presentes.
