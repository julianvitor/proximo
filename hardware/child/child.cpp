#include <Arduino.h>
#include <ETH.h>
#include <WiFi.h>
#include <Wire.h>
#include <Adafruit_PN532.h>
#include <WebSocketsServer.h>

// Pinos dos Relés e RFID
#define RELE_PIN 2
#define RELE_PIN2 4

// I2C
#define I2C_SCL 32
#define I2C_SDA 33

// Ethernet
#define ETH_ADDR        1
#define ETH_POWER_PIN_ALTERNATIVE 16
#define ETH_MDC_PIN    23
#define ETH_MDIO_PIN   18
#define ETH_TYPE       ETH_PHY_LAN8720
#define ETH_CLK_MODE   ETH_CLOCK_GPIO17_OUT

unsigned long previousMillis = 0;
const long interval = 5000;
const long releDuration = 20000; // Tempo de ativação do relé em milissegundos

Adafruit_PN532 nfc(I2C_SDA, I2C_SCL);

WebSocketsServer webSocket = WebSocketsServer(81);

static bool eth_connected = false;

String lastUID = "";
String currentUID = "";

bool releAtivo = false;
unsigned long tempoInicioAtivacao = 0;

void WiFiEvent(WiFiEvent_t event) {
  switch (event) {
    case SYSTEM_EVENT_ETH_START:
      ETH.setHostname("esp32-ethernet"); // Define o hostname da ETH
      break;
    case SYSTEM_EVENT_ETH_CONNECTED:
      break;
    case SYSTEM_EVENT_ETH_GOT_IP:
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
      webSocket.sendTXT(num, payload);
      if (strcmp((char*)payload, "ativar 1") == 0) {
        acionarRele(RELE_PIN);
      } else if (strcmp((char*)payload, "ativar 2") == 0) {
        acionarRele(RELE_PIN2);
      }
      break;
    default:
      break;
  }
}

void readRFID() {
  uint8_t success;
  uint8_t uid[] = { 0, 0, 0, 0, 0, 0, 0 };  // Buffer para armazenar o UID lido
  uint8_t uidLength;                        // Comprimento do UID (4 ou 7 bytes dependendo do tipo de cartão)
  success = nfc.readPassiveTargetID(PN532_MIFARE_ISO14443A, uid, &uidLength, 100);
  
  if (success) {
    currentUID = "";
    for (uint8_t i = 0; i < uidLength; i++) {
      currentUID += String(uid[i], HEX);
    }

    if (currentUID != lastUID || lastUID == "") {
      webSocket.broadcastTXT("inserido:" + currentUID);
      lastUID = currentUID;
    }
  } else {
    if (lastUID != "") {
      webSocket.broadcastTXT("removido:" + lastUID);
      lastUID = "";
    }
  }
}

void acionarRele(int pin) {
  digitalWrite(pin, LOW);
  releAtivo = true;
  tempoInicioAtivacao = millis(); // Salva o tempo de início da ativação
}

void setup() {
  pinMode(RELE_PIN, OUTPUT);
  pinMode(RELE_PIN2, OUTPUT);
  pinMode(ETH_POWER_PIN_ALTERNATIVE, OUTPUT);

  digitalWrite(RELE_PIN, LOW);
  digitalWrite(RELE_PIN2, LOW);
  delay(2000);
  digitalWrite(RELE_PIN, HIGH);
  digitalWrite(RELE_PIN2, HIGH);

  digitalWrite(ETH_POWER_PIN_ALTERNATIVE, HIGH);

  Wire.begin(I2C_SDA, I2C_SCL);
  nfc.begin();
  
  uint32_t versiondata = nfc.getFirmwareVersion();
  if (!versiondata) {
    //Serial.println("Não foi possível encontrar o PN53x. Certifique-se de que está conectado corretamente.");
  } else {
    nfc.SAMConfig();
    //Serial.println("ESP32 PN532 Iniciado!");
    //Serial.println(versiondata);
  }
  
  btStop();
  
  WiFi.onEvent(WiFiEvent);
  ETH.begin(ETH_ADDR, ETH_POWER_PIN_ALTERNATIVE, ETH_MDC_PIN, ETH_MDIO_PIN, ETH_TYPE, ETH_CLK_MODE);
  webSocket.begin();
  webSocket.onEvent(webSocketEvent);
}

void loop() {
  if (!eth_connected) {
    ETH.begin(ETH_ADDR, ETH_POWER_PIN_ALTERNATIVE, ETH_MDC_PIN, ETH_MDIO_PIN, ETH_TYPE, ETH_CLK_MODE);
  }
  
  webSocket.loop();
  
  unsigned long currentMillis = millis();
  if (currentMillis - previousMillis >= interval) {
    previousMillis = currentMillis;
    readRFID();
  }

  if (releAtivo && (currentMillis - tempoInicioAtivacao >= releDuration)) {
    digitalWrite(RELE_PIN, HIGH);
    digitalWrite(RELE_PIN2, HIGH);
    releAtivo = false;
  }
}
