### Formato do JSON

A requisição do pai deve ser no seguinte formato:

```json
{
    "accio_machine": {},
    "requestId": "12345678"  // ID para confirmação
}

```

A resposta do filho deve ser no seguinte formato:

```json
{
    "accio_machine_response":{
        "CA:FE:CA:FE:CA:FE":{ // Endereço MAC do filho
            "machineId": "UID_RFID_1234567890",  // tag rfid da maquina
            "childId": "CA:FE:CA:FE:CA:FE",  // Endereço MAC do filho
        }
    },
    "requestId": "12345678"  // ID para confirmação
}

```


### Request

- **accio_machine** (objeto):  Objeto principal para a solicitação. Este campo deve ser vazio.
- **requestId** (string): Identificador único para a confirmação da solicitação.

#### Response 
- **accio_machine_response** (objeto): Objeto principal da resposta.
- **machineId** (string): UID de RFID do dispositivo.
- **childId** (string): Identificador único associado ao filho.
- **requestId** (string): Identificador único para a confirmação da solicitação.

### Erros Comuns e Tratamento

- **Campo faltando**: Se um campo obrigatório estiver ausente, o aplicativo não conseguirá processar o log corretamente e retornará um erro genérico no Logcat.
- **Formato incorreto**: Se um campo não estiver no formato correto (por exemplo, uma string no lugar de um número), o aplicativo pode falhar ao tentar exibir o log.
- **Tratamento**: Em caso de erro, o aplicativo exibirá uma mensagem de erro genérica na interface e ignorará o log malformado.

### FAQ

**P:** O que acontece se eu enviar um campo adicional não documentado?
**R:** Campos adicionais serão ignorados pelo aplicativo, mas evite para melhor performance e entendimento do codigo.

**P:** Posso omitir campos opcionais?
**R:** Sim, campos opcionais podem ser omitidos, mas os campos requestId, childId e machineId devem estar presentes.