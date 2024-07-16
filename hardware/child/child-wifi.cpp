#include <Arduino.h>
#include <WiFi.h>
#include <PN532_I2C.h>
#include <PN532.h>
#include <WebSocketsServer.h>

// Reles e RFID
#define RELE_PIN 2
#define RELE_PIN2 4

// I2C
#define I2C_SCL 22
#define I2C_SDA 21

// WiFi
const char* ssid = "2.4ghz";
const char* password = "55FC5C7A51";

unsigned long previousMillis = 0;
const long interval = 5000;
const long releDuration = 20000; // Tempo de ativação do relé em milissegundos

PN532_I2C pn532_i2c(Wire);
PN532 nfc(pn532_i2c);

WebSocketsServer webSocket = WebSocketsServer(81);

bool wifi_connected = false;

String lastUID = "";
String currentUID = "";

bool releAtivo = false;
unsigned long tempoInicioAtivacao = 0;

void WiFiEvent(WiFiEvent_t event) {
  switch (event) {
    case SYSTEM_EVENT_STA_START:
      Serial.println("WiFi Started");
      WiFi.setHostname("esp32-wifi"); // Set WiFi hostname
      break;
    case SYSTEM_EVENT_STA_CONNECTED:
      Serial.println("WiFi Connected");
      break;
    case SYSTEM_EVENT_STA_GOT_IP:
      Serial.print("WiFi MAC: ");
      Serial.print(WiFi.macAddress());
      Serial.print(", IPv4: ");
      Serial.print(WiFi.localIP());
      wifi_connected = true;
      break;
    case SYSTEM_EVENT_STA_DISCONNECTED:
      Serial.println("WiFi Disconnected");
      wifi_connected = false;
      WiFi.begin(ssid, password); // Reconnect WiFi
      break;
    default:
      break;
  }
}

void webSocketEvent(uint8_t num, WStype_t type, uint8_t * payload, size_t length) {
  switch(type) {
    case WStype_TEXT:
      Serial.printf("[%u] Text: %s\n", num, payload);
      webSocket.sendTXT(num, payload);
      if (strcmp((char*)payload, "ativar 1") == 0) {
        acionarRele(RELE_PIN);
      } 
      else if (strcmp((char*)payload, "ativar 2") == 0) {
        acionarRele(RELE_PIN2);
      } 
      break;
    default:
      break;
  }
}

void readRFID() {
  // Verifica se há um cartão RFID
  uint8_t success;
  uint8_t uid[] = { 0, 0, 0, 0, 0, 0, 0 };  // Buffer para armazenar o UID lido
  uint8_t uidLength;                        // Comprimento do UID (4 ou 7 bytes dependendo do tipo de cartão)
  success = nfc.readPassiveTargetID(PN532_MIFARE_ISO14443A, uid, &uidLength, 100);
  
  if (success) {
    // Se um cartão foi encontrado, converte o UID para uma string
    currentUID = "";
    for (uint8_t i=0; i < uidLength; i++) {
      currentUID += String(uid[i], HEX);
    }

    // Verifica se o UID atual é diferente do UID anterior ou se é a primeira leitura
    if (currentUID != lastUID || lastUID == "") {
      // Envia o UID para todos os clientes conectados via WebSocket
      webSocket.broadcastTXT("inserido:" + currentUID);

      // Imprime o UID no Serial Monitor
      Serial.print("inserido:");
      Serial.println(currentUID);

      // Atualiza o último UID lido
      lastUID = currentUID;
    }
  } else {
    // Se não foi encontrado um cartão RFID, verifica se o último UID lido foi diferente de vazio
    if (lastUID != "") {
      // Envia uma mensagem indicando que o cartão foi removido para todos os clientes conectados via WebSocket
      webSocket.broadcastTXT("removido:" + lastUID);

      // Imprime no Serial Monitor que o cartão foi removido
      //Serial.println("removido:" + lastUID);

      // Limpa o último UID lido
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

  digitalWrite(RELE_PIN, LOW);
  digitalWrite(RELE_PIN2, LOW);
  delay(2000);
  digitalWrite(RELE_PIN, HIGH);
  digitalWrite(RELE_PIN2, HIGH);

  Serial.begin(115200);
  
  Wire.begin(I2C_SDA, I2C_SCL);
  nfc.begin();

  uint32_t versiondata = nfc.getFirmwareVersion();
  if (!versiondata) {
    Serial.println("Não foi possível encontrar o PN53x. Certifique-se de que está conectado corretamente.");
  } else {
    nfc.SAMConfig();
    Serial.println("ESP32 PN532 Iniciado!");
    Serial.println(versiondata);
  }
  btStop();
  
  WiFi.onEvent(WiFiEvent);
  WiFi.begin(ssid, password);
  webSocket.begin();
  webSocket.onEvent(webSocketEvent);
}

void loop() {
  // Executa o loop do WebSocketsServer
  webSocket.loop();
  
  unsigned long currentMillis = millis();
  if (currentMillis - previousMillis >= interval) {
    // Salva o último tempo de leitura
    previousMillis = currentMillis;
    // Chama a função para ler o cartão RFID
    readRFID();
  }

  if (releAtivo && (currentMillis - tempoInicioAtivacao >= releDuration)) {
    // Se o relé está ativo e o tempo de ativação expirou, desativa o relé
    digitalWrite(RELE_PIN, HIGH);
    digitalWrite(RELE_PIN2, HIGH);
    releAtivo = false;
  }
}
