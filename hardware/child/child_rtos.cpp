// Codigo desenvolvido por Julian Carreiro.

// Boas praticas neste codigo.

#include <Arduino.h>
#include <ETH.h>
#include <WebSocketsClient.h>
#include <PN532_I2C.h>
#include <PN532.h>
#include <Wire.h>
#include <ArduinoJson.h>
#include "esp_system.h"

// Definição dos pinos
const int RELE1_PIN = 2; 
const int I2C_SCL = 32;  
const int I2C_SDA = 33;  
const int PN532_RESET_PIN = 15;  

// Ethernet Config
#define ETH_CLK_MODE    ETH_CLOCK_GPIO0_IN
#define ETH_POWER_PIN  16 
#define ETH_TYPE        ETH_PHY_LAN8720 
#define ETH_ADDR        1 
#define ETH_MDC_PIN     23 
#define ETH_MDIO_PIN    18 

WebSocketsClient webSocket;

// Estados
bool global_ethernet_conexao = false;
bool global_rele_ativo = false;
bool global_rsto_ativo = false;

int DURACAO_RELE = 28000;

String global_uid_anterior;
String global_uid_atual;

// Protótipos de funções
void iniciarEthernet();
void gerenciar_erros_callback();
void iniciarPn532_callback();
void reiniciar_pn532_callback();
void ativarRele();
void desativar_rele_callback();
String lerRfid();
void enviarInsertedJson(const String& UID_INSERTED);
void WiFiEvent(WiFiEvent_t event);
void webSocketEvent(WStype_t type, uint8_t* payload, size_t length);

// Instanciar sensor
PN532_I2C pn532_i2c(Wire);
PN532 nfc(pn532_i2c);

// Variáveis do FreeRTOS
TaskHandle_t taskEthernetHandle = NULL;
TaskHandle_t taskPN532Handle = NULL;
TaskHandle_t taskWebSocketHandle = NULL;
TaskHandle_t taskReleHandle = NULL;

// Função que trata eventos de Ethernet
void WiFiEvent(WiFiEvent_t event) {
  switch (event) {
    case SYSTEM_EVENT_ETH_START:
      ETH.setHostname("wt32-child");
      break;
    case SYSTEM_EVENT_ETH_GOT_IP:
      global_ethernet_conexao = true;
      break;
    case SYSTEM_EVENT_ETH_DISCONNECTED:
      global_ethernet_conexao = false;
      break;
    default:
      break;
  }
}

// Tarefa de gerenciamento Ethernet
void taskEthernet(void* pvParameters) {
  for (;;) {
    if (!global_ethernet_conexao) {
      ETH.begin(ETH_ADDR, ETH_POWER_PIN, ETH_MDC_PIN, ETH_MDIO_PIN, ETH_TYPE, ETH_CLK_MODE);
    }
    vTaskDelay(pdMS_TO_TICKS(2000));  // Checa a cada 2 segundos
  }
}

// Função que trata eventos do WebSocket
void webSocketEvent(WStype_t type, uint8_t* payload, size_t length) {
  if (type == WStype_TEXT) {
    if (payload[0] == '{') {
      StaticJsonDocument<256> json_received_message;
      DeserializationError error = deserializeJson(json_received_message, payload, length);

      if (!error) {
        // Lógica de mensagens JSON...
      } else {
        webSocket.sendTXT("Erro: JSON inesperado recebido.");
      }
    }
  }
}

// Tarefa de gerenciamento do WebSocket
void taskWebSocket(void* pvParameters) {
  for (;;) {
    webSocket.loop();  // Loop para manter a conexão WebSocket ativa
    vTaskDelay(pdMS_TO_TICKS(100));  // Checa a cada 100 ms
  }
}

// Tarefa de leitura do RFID
void taskPN532(void* pvParameters) {
  for (;;) {
    String uid = lerRfid();
    if (uid != global_uid_anterior && !uid.isEmpty()) {
      global_uid_anterior = uid;
      enviarInsertedJson(uid);
    } else if (global_uid_anterior != "" && uid.isEmpty()) {
      global_uid_anterior = "";
      // Logica para quando o cartão for removido...
    }
    vTaskDelay(pdMS_TO_TICKS(6000));  // Tempo de espera entre leituras de RFID
  }
}

// Função para ativar o relé
void ativarRele() {
  digitalWrite(RELE1_PIN, LOW);
  global_rele_ativo = true;
  vTaskDelay(pdMS_TO_TICKS(DURACAO_RELE));  // Aguarda o tempo de ativação do relé
  desativar_rele_callback();
}

// Função para desativar o relé
void desativar_rele_callback() {
  digitalWrite(RELE1_PIN, HIGH);
  global_rele_ativo = false;
}

// Inicializa a Ethernet
void iniciarEthernet() {
  WiFi.onEvent(WiFiEvent);
  ETH.begin(ETH_ADDR, ETH_POWER_PIN, ETH_MDC_PIN, ETH_MDIO_PIN, ETH_TYPE, ETH_CLK_MODE);
}

// Inicializa o PN532
void iniciarPn532_callback() {
  digitalWrite(PN532_RESET_PIN, HIGH);
  Wire.begin(I2C_SDA, I2C_SCL);
  nfc.begin();
  uint32_t versiondata = nfc.getFirmwareVersion();
  if (versiondata) {
    nfc.SAMConfig();  // Configura o PN532 para leitura de cartões RFID
  }
}

// Função para ler o RFID
String lerRfid() {
  uint8_t uid[7] = {0};  // Buffer para armazenar o UID lido
  uint8_t uidLength;
  
  if (nfc.readPassiveTargetID(PN532_MIFARE_ISO14443A, uid, &uidLength)) {
    String uid_string = "";
    for (uint8_t i = 0; i < uidLength; i++) {
      if (uid[i] < 0x10) {
        uid_string += "0";
      }
      uid_string += String(uid[i], HEX);
    }
    return uid_string;
  }
  return "";
}

// Função para enviar JSON ao WebSocket
void enviarInsertedJson(const String& UID_INSERTED) {
  StaticJsonDocument<256> json_doc;
  json_doc["inserted"]["rfid"] = UID_INSERTED;
  json_doc["inserted"]["station_mac"] = ETH.macAddress();
  json_doc["message_id"] = String(random(10000000, 99999999));
  String message;
  serializeJson(json_doc, message);
  webSocket.sendTXT(message);
}

// Setup principal
void setup() {
  Serial.begin(115200);
  pinMode(RELE1_PIN, OUTPUT);
  digitalWrite(RELE1_PIN, HIGH);
  iniciarEthernet();
  iniciarPn532_callback();

  // Criação de tasks FreeRTOS
  xTaskCreatePinnedToCore(taskEthernet, "TaskEthernet", 4096, NULL, 1, &taskEthernetHandle, 0);
  xTaskCreatePinnedToCore(taskPN532, "TaskPN532", 4096, NULL, 1, &taskPN532Handle, 1);
  xTaskCreatePinnedToCore(taskWebSocket, "TaskWebSocket", 4096, NULL, 1, &taskWebSocketHandle, 1);
}

void loop() {
  // O loop não é necessário com o uso do RTOS
}
