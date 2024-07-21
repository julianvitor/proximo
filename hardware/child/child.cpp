// Codigo desenvolvido por Julian Carreiro.

// Boas praticas neste codigo.
// Funções callback são nomeadas seguindo o padrão: função_faz_algo_callback().
// Funções normais são nomeadas seguindo o padrão: funcaoFazAlgo().
// Variaveis são nomeadas seguindo o padrão: armazena_alguma_coisa.
// Constantes são nomeadas em caixa alta, seguindo o mesmo padrão das variaveis: ARMAZENA_ALGUMA_COISA.
// Comentarios para bloco e semânticos são definidos acima do codigo.
// Comentarios especificos são definidos ao lado da linha especifica.
// Evite Comentarios sobre sintaxe, apenas em caso onde escritas avançadas são usadas.
// A posição e ordem de estruturas, funções e outros devem ser mantida de acordo com suas funções, em resumo: próximo o que é semelhante.
#include <Arduino.h>
#include <ETH.h>
#include <WebSocketsServer.h>
#include <PN532_I2C.h>
#include <PN532.h>
#include "TickTwo.h"
#include <Wire.h>

// Definição dos pinos
#define RELE1_PIN 2 // Pino digital conectado ao primeiro relé
#define RELE2_PIN 5 // Pino digital conectado ao segundo relé
#define I2C_SCL 32  // Pino i2c clock RFID
#define I2C_SDA 33  // Pino i2c data RFID

// Ethernet
/* 
   * ETH_CLOCK_GPIO0_IN   - default: external clock from crystal oscillator
   * ETH_CLOCK_GPIO0_OUT  - 50MHz clock from internal APLL output on GPIO0 - possibly an inverter is needed for LAN8720
   * ETH_CLOCK_GPIO16_OUT - 50MHz clock from internal APLL output on GPIO16 - possibly an inverter is needed for LAN8720
   * ETH_CLOCK_GPIO17_OUT - 50MHz clock from internal APLL inverted output on GPIO17 - tested with LAN8720
*/
#define ETH_CLK_MODE    ETH_CLOCK_GPIO0_IN
#define ETH_POWER_PIN  16 // Pin# of the enable signal for the external crystal oscillator (-1 to disable for internal APLL source)
#define ETH_TYPE        ETH_PHY_LAN8720 // Type of the Ethernet PHY (LAN8720 or TLK110)
#define ETH_ADDR        1 // I²C-address of Ethernet PHY (0 or 1 for LAN8720, 31 for TLK110)
#define ETH_MDC_PIN     23 // Pin# of the I²C clock signal for the Ethernet PHY
#define ETH_MDIO_PIN    18 // Pin# of the I²C IO signal for the Ethernet PHY

WebSocketsServer webSocket = WebSocketsServer(81); // WebSocket server on port 81

//Estados
bool ethernet_conexao = false;
bool rele1Ativo = false;
bool rele2Ativo = false;

int DURACAO_RELE = 20000;// Duração do rele ativado em milissegundos

// Variaveis globais
String uidAnterior = "";
String uidAtual = "";

// Protótipos de funções
void configurarPn532();
void iniciarEthernet();
// Prototipos de callbacks.
void gerenciar_erros_callback();
void ler_rfid_callback();
void desativar_rele1_callback();
void desativar_rele2_callback();

// Instanciar sensor
PN532_I2C pn532_i2c(Wire);
PN532 nfc(pn532_i2c);

// Instanciar os timers(tasks)
TickTwo timerLerRfid(ler_rfid_callback, 6000, 0, MILLIS);
TickTwo timerGerenciarErros(gerenciar_erros_callback, 2000, 0, MILLIS);
TickTwo timerDesativarRele1(desativar_rele1_callback, DURACAO_RELE, 1, MILLIS);
TickTwo timerDesativarRele2(desativar_rele2_callback, DURACAO_RELE, 1, MILLIS);

void WiFiEvent(WiFiEvent_t event) {
  switch (event) {
    case SYSTEM_EVENT_ETH_START:
      ETH.setHostname("wt32-child"); // Definir ETH hostname
      break;
    case SYSTEM_EVENT_ETH_CONNECTED:
      break;
    case SYSTEM_EVENT_ETH_GOT_IP:
      if (ETH.fullDuplex()) {
      }
      ethernet_conexao = true;
      break;
    case SYSTEM_EVENT_ETH_DISCONNECTED:
      ethernet_conexao = false;
      break;
    case SYSTEM_EVENT_ETH_STOP:
      ethernet_conexao = false;
      break;
    default:
      break;
  }
}

void webSocketEvent(uint8_t num, WStype_t type, uint8_t * payload, size_t length) {
    switch(type) {
      case WStype_TEXT:
        if (strcmp((char*)payload, "ativar 1") == 0){
          ativarRele();
        } 
        else if (strcmp((char*)payload, "ativar 2") == 0){
          ativarRele2();
        }
        else if (strcmp((char*)payload, "firmware") == 0){
          uint32_t versiondata = nfc.getFirmwareVersion();// Verificar se o pn532 foi detectado
          if (versiondata){
            String firmware = "PN532 Firmware: ";
            firmware += String(versiondata);
            webSocket.sendTXT(num, firmware);// Enviar versão do firmware para o cliente
          } 
          else{
            webSocket.sendTXT(num, "Erro: PN532 não encontrado");// PN532 não econtrado, enviar mensagme de erro
          }
        }
        // Eco das mensagens recebidas
        webSocket.sendTXT(num, payload, length); 
        break;
      default:
        break;
    }
  }

void gerenciar_erros_callback(){
  if (!ethernet_conexao) {
    // Se não estiver conectado, tenta inicializar a conexão Ethernet novamente
    ETH.begin(ETH_ADDR, ETH_POWER_PIN, ETH_MDC_PIN, ETH_MDIO_PIN, ETH_TYPE, ETH_CLK_MODE);
  }
}

void ler_rfid_callback() {
  // Verifica se há um cartão RFID
  uint8_t success;
  uint8_t uid[7] = { 0, 0, 0, 0, 0, 0, 0 };  // Buffer para armazenar o UID lido
  uint8_t uidLength;                        // Comprimento do UID (4 ou 7 bytes dependendo do tipo de cartão)
  
  // Tenta ler um cartão RFID
  success = nfc.readPassiveTargetID(PN532_MIFARE_ISO14443A, uid, &uidLength, 100);
  
  // Se um cartão foi encontrado, converte o UID para uma string
  if (success) {
    uidAtual = "";
    for (uint8_t i = 0; i < uidLength; i++) {
      if (uid[i] < 0x10) uidAtual += "0"; // Adiciona um zero à esquerda se necessário
      uidAtual += String(uid[i], HEX);
    }
    //verificar se o UID mudou ou se é a primeira vez que é lido
    if (uidAtual!= uidAnterior || uidAtual == "") {
      uidAnterior = uidAtual;
      webSocket.broadcastTXT("inserido:" + uidAtual);    // Envia o UID para todos os clientes conectados via WebSocket
    }
  }
  // Se não foi encontrado um cartão RFID, verifica se o ultimo UID lido foi diferente de vazio
  else {
    if (uidAnterior!= "") {
      webSocket.broadcastTXT("removido:" + uidAnterior); // Envia o UID para todos os clientes conectados via WebSocket
      uidAnterior = "";
    }
  }
}

void desativar_rele1_callback() {
    digitalWrite(RELE1_PIN, HIGH);
    rele1Ativo = false;
}

void desativar_rele2_callback() {
    digitalWrite(RELE2_PIN, HIGH);
    rele2Ativo = false;
}

void ativarRele() {
    digitalWrite(RELE1_PIN, LOW);
    rele1Ativo = true;
    timerDesativarRele1.start();

}

void ativarRele2() {
    digitalWrite(RELE2_PIN, LOW);
    rele2Ativo = true;
    timerDesativarRele2.start();

}

void iniciarEthernet(){
  WiFi.onEvent(WiFiEvent);
  ETH.begin(ETH_ADDR,ETH_POWER_PIN, ETH_MDC_PIN, ETH_MDIO_PIN, ETH_TYPE, ETH_CLK_MODE);

}

void configurarPn532() {
  uint32_t versiondata = nfc.getFirmwareVersion();
  if (!versiondata) {} 
  else {
    nfc.SAMConfig(); // PN532 configurar para operar no modo de leitura.
  }
}

void inicializarReles() {
  pinMode(RELE1_PIN, OUTPUT);
  pinMode(RELE2_PIN, OUTPUT); 
}

void testarReles() {
  digitalWrite(RELE1_PIN, LOW);
  delay(3000);
  digitalWrite(RELE1_PIN, HIGH);
  delay(1000);
  digitalWrite(RELE2_PIN, LOW);
  delay(3000);
  digitalWrite(RELE2_PIN, HIGH);
}

void setup() {
  btStop();// desativar o Bluetooth
  inicializarReles();
  testarReles();

  Wire.begin(I2C_SDA, I2C_SCL);// Inicializa a interface I2C com os pinos SDA e SCL.
  nfc.begin();// Inicializa o módulo PN532
  configurarPn532(); // Configura o PN532 para operar no modo de leitura.


  iniciarEthernet();
  webSocket.begin();
  webSocket.onEvent(webSocketEvent);

  // Iniciar a tasks
  timerLerRfid.start();
  timerGerenciarErros.start();
}

void loop() {
  timerGerenciarErros.update();
  timerLerRfid.update();
  timerDesativarRele1.update();
  timerDesativarRele2.update();

  // Executa o loop do WebSocketsServer
  webSocket.loop();
}
