Como a atividade trata os dados para realizar a liberação das maquinas.

maquianasCatalog.json API
```json
[
  {
    "id": "3ca61935-2b3b-45aa-970a-4bd390194bfa",
    "name": "30 AM",
    "rfid": "10987654321",
    "status": "TRANSPORTING",
    "stationId": "a2b7fab1-b8f0-4c85-b5a4-c629bbf12936"
  },
  {
    "id": "42942fb8-a40b-4753-a8d0-427b7a5a21dc",
    "name": "Ecolift 70",
    "rfid": "12345678910",
    "status": "ACTIVE",
    "stationId": null
  }
]

```


maquianasPresentes.json hardware
```json

{
    "accio_machine_response": {
        "rfid": "12345678910",
        "childId": "CA:FE:CA:FE:CA:FE"
    },
    "requestId": "12345678"
}

```
maquianasDisponiveis.json local
```json
[
  {
    "rfid": "12345678910",
    "modelo": "Ecolift 70"
  }
]

```

