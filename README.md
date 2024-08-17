# Proximo

Proximo é um projeto composto por dois softwares: ALi para dispositivos Android e o firmware para hardware baseado no WT32-ETH01, projetados para controle de acesso com gatilho de trava.

## Descrição

O Proximo oferece uma solução de controle de acesso simples e fácil de usar, permitindo que os usuários interajam com dispositivos de gatilho de trava de forma conveniente e segura. O software ALi é projetado para funcionar em dispositivos Android, proporcionando uma interface intuitiva para os usuários finais. O firmware para hardware baseado no WT32-ETH01.

## Funcionalidades

- Controle de acesso via dispositivos Android (ALi) e hardware baseado no WT32(ESP32 ETHERNET).
- Integração com dispositivos de gatilho (hardware)
- Interface de usuário ALi para sistema Android embarcado.
- Comunicação entre o ALi e o hardware em tempo real por WebSocket.

## Instalação

1. Para instalar o software ALi no dispositivo Android, baixe o APK mais recente disponível na seção de lançamentos do repositório. Para instalar o firmware no hardware baseado no WT32, grave na WT32-ETH01 o firmware disponivel em `hardware`.

## Uso

1. Abra o aplicativo ALi no dispositivo Android.
2. Faça Cadastro/login com suas credenciais.
3. Selecione a opção desejada para interagir com o gatilho de trava.
4. Faça a devolução do RFID no hardware.
5. O painel Administrador padrão é acessado com **Usuário: admin**, **Senha: admin**.

## Documentação de desenvolvimento 
[Documentação geral](https://github.com/julianvitor/proximo/tree/dev/documentacao_projeto)
[Documentação comunicação local](https://github.com/julianvitor/proximo/tree/main/documentacao_projeto/protocolo_pai_filho)

## Autores

O Proximo é mantido por [Julian Vitor Ambrozio Carreiro].

## Licença

Este projeto está licenciado sob a [Licença MIT](LICENSE).
