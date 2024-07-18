#include <Arduino.h>
#include <ETH.h>
#include <WebSocketsServer.h>
#include <PN532_I2C.h>
#include <PN532.h>
#include "TickTwo.h"

// Reles e RFID
#define RELE_PIN 2
#define RELE_PIN2 4

// I2C
#define I2C_SCL 32
#define I2C_SDA 33

// Ethernet
/* 
   * ETH_CLOCK_GPIO0_IN   - default: external clock from crystal oscillator
   * ETH_CLOCK_GPIO0_OUT  - 50MHz clock from internal APLL output on GPIO0 - possibly an inverter is needed for LAN8720
   * ETH_CLOCK_GPIO16_OUT - 50MHz clock from internal APLL output on GPIO16 - possibly an inverter is needed for LAN8720
   * ETH_CLOCK_GPIO17_OUT - 50MHz clock from internal APLL inverted output on GPIO17 - tested with LAN8720
*/
#define ETH_CLK_MODE    ETH_CLOCK_GPIO0_IN

// Pin# of the enable signal for the external crystal oscillator (-1 to disable for internal APLL source)
#define ETH_POWER_PIN  16

// Type of the Ethernet PHY (LAN8720 or TLK110)
#define ETH_TYPE        ETH_PHY_LAN8720

// I²C-address of Ethernet PHY (0 or 1 for LAN8720, 31 for TLK110)
#define ETH_ADDR        1

// Pin# of the I²C clock signal for the Ethernet PHY
#define ETH_MDC_PIN     23

// Pin# of the I²C IO signal for the Ethernet PHY
#define ETH_MDIO_PIN    18

PN532_I2C pn532_i2c(Wire);
PN532 nfc(pn532_i2c);

WebSocketsServer webSocket = WebSocketsServer(81); // WebSocket server on port 81

static bool eth_connected = false;

String lastUID = "";
String currentUID = "";

bool releAtivo = false;

void WiFiEvent(WiFiEvent_t event) {
  switch (event) {
    case SYSTEM_EVENT_ETH_START:
      ETH.setHostname("esp32-ethernet"); // Set ETH hostname
      break;
    case SYSTEM_EVENT_ETH_CONNECTED:
      break;
    case SYSTEM_EVENT_ETH_GOT_IP:
      if (ETH.fullDuplex()) {
      }
      eth_connected = true;
      break;
    case SYSTEM_EVENT_ETH_DISCONNECTED:
      eth_connected = false;
      break;
    case SYSTEM_EVENT_ETH_STOP:
      eth_connected = false;
      break;
    default:
      break;
  }
}

void webSocketEvent(uint8_t num, WStype_t type, uint8_t * payload, size_t length) {
    switch(type) {
      case WStype_TEXT:
        if (strcmp((char*)payload, "ativar 1") == 0){
          acionarRele(RELE_PIN);
        } 
        else if (strcmp((char*)payload, "ativar 2") == 0){
          acionarRele(RELE_PIN2);
        }
        // verificar se o cliente quer o firmware
        else if (strcmp((char*)payload, "firmware") == 0){
            
          // Verificar se o pn532 foi detectado
          uint32_t versiondata = nfc.getFirmwareVersion();  

          if (versiondata){
            String firmware = "PN532 Firmware version: ";
            firmware += String(versiondata);

            // Enviar versão do firmware para o cliente
            webSocket.sendTXT(num, firmware);
          } 
          else{
            // PN532 não econtrado, enviar mensagme de erro
            webSocket.sendTXT(num, "Erro: PN532 não encontrado");
          }
        }
        // Echo message back to client
        webSocket.sendTXT(num, payload, length); 
        break;
      default:
        break;
    }
  }

void lerRfid() {
  // Verifica se há um cartão RFID
  uint8_t success;
  uint8_t uid[7] = { 0, 0, 0, 0, 0, 0, 0 };  // Buffer para armazenar o UID lido
  uint8_t uidLength;                        // Comprimento do UID (4 ou 7 bytes dependendo do tipo de cartão)
  
  // Tenta ler um cartão RFID
  success = nfc.readPassiveTargetID(PN532_MIFARE_ISO14443A, uid, &uidLength, 100);
  
  if (success) {
    // Se um cartão foi encontrado, converte o UID para uma string
    String uidString = "";
    for (uint8_t i = 0; i < uidLength; i++) {
      if (uid[i] < 0x10) uidString += "0"; // Adiciona um zero à esquerda se necessário
      uidString += String(uid[i], HEX);
    }

    // Envia o UID para todos os clientes conectados via WebSocket
    webSocket.broadcastTXT("UID:" + uidString);
  }
}

void acionarRele(int pin) {
    digitalWrite(pin, LOW);
    releAtivo = true;
}

void desativarRele(int pin){
    digitalWrite(pin, HIGH);
    releAtivo = false;
}

TickTwo timerLerRfid(lerRfid, 5000, 0, MILLIS);

void setup() {
  pinMode(RELE_PIN, OUTPUT);
  pinMode(RELE_PIN2, OUTPUT);

  digitalWrite(RELE_PIN, LOW);
  digitalWrite(RELE_PIN2, LOW);
  delay(3000);
  digitalWrite(RELE_PIN, HIGH);
  digitalWrite(RELE_PIN2, HIGH);
  
  Wire.begin(I2C_SDA, I2C_SCL);
  nfc.begin();

  uint32_t versiondata = nfc.getFirmwareVersion();
  if (!versiondata) {
  } else {
    nfc.SAMConfig();
  }
  btStop();
  
  WiFi.onEvent(WiFiEvent);
  ETH.begin(ETH_ADDR,ETH_POWER_PIN, ETH_MDC_PIN, ETH_MDIO_PIN, ETH_TYPE, ETH_CLK_MODE);
  webSocket.begin();
  webSocket.onEvent(webSocketEvent);

  // Iniciar a task do rfid
  timerLerRfid.start();

}

void loop() {
  timerLerRfid.update();
  
  if (!eth_connected) {
    // Se não estiver conectado, tenta inicializar a conexão Ethernet novamente
    ETH.begin(ETH_ADDR, ETH_POWER_PIN, ETH_MDC_PIN, ETH_MDIO_PIN, ETH_TYPE, ETH_CLK_MODE);
  }

  // Executa o loop do WebSocketsServer
  webSocket.loop();
}
