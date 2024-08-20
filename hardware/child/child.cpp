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
#include <WebSocketsClient.h>
#include <PN532_I2C.h>
#include <PN532.h>
#include "TickTwo.h"
#include <Wire.h>
#include <ArduinoJson.h> // Inclua a biblioteca ArduinoJson
#include "esp_system.h"


// Definição dos pinos
const int RELE1_PIN = 2; // Pino digital conectado ao primeiro relé
const int RELE2_PIN = 4; // Pino digital conectado ao segundo relé
const int I2C_SCL = 32;  // Pino i2c clock RFID
const int I2C_SDA = 33;  // Pino i2c data RFID
const int PN532_RESET_PIN = 15;   // Reset pin RFID

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

WebSocketsClient webSocket; // WebSocket client

//Estados
bool ethernet_conexao = false;
bool rele1Ativo = false;
bool rele2Ativo = false;
bool rsto_ativo = false;

int DURACAO_RELE = 30000;// Duração do rele ativado em milissegundos

// Variaveis globais
String uidAnterior = "";
String uidAtual = "";

// Protótipos de funções
void iniciarEthernet();
void gerenciar_erros_callback();
void ler_rfid_callback();
void desativar_rele1_callback();
void desativar_rele2_callback();
void reiniciar_pn532_callback();
void iniciarPn532_callback(int I2C_SDA, int I2C_SCL);// Iniciar o rsto, instancia o PN532 e inicia a comunicacao em modo leitura
void LogResponse();

// Instanciar sensor
PN532_I2C pn532_i2c(Wire);
PN532 nfc(pn532_i2c);

// Instanciar os timers(tasks)
TickTwo timerLerRfid(ler_rfid_callback, 6000, 0, MILLIS);
TickTwo timerReiniciarPn532(reiniciar_pn532_callback, 120000, 0, MILLIS);// Reinicia o PN532 a cada 120 segundos
TickTwo timerAtivarRsto([]() { iniciarPn532_callback(I2C_SDA, I2C_SCL); }, 100, 1, MILLIS);//necessario uso de função lambda para passar os parametros
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
// Função que trata os eventos do WebSocket
void webSocketEvent(WStype_t type, uint8_t * payload, size_t length) {
    switch(type) {
      case WStype_TEXT:
        // Verifica se o payload começa com '{', indicando que é um JSON
        if (payload[0] == '{') {
          String mensagemRecebida = String((char*)payload).substring(0, length);
          StaticJsonDocument<512> jsonDoc;
          DeserializationError error = deserializeJson(jsonDoc, mensagemRecebida);
          if (!error) {
            // Verificar se o campo "accio_machine" existe exatamente no JSON recebido
            if (jsonDoc.containsKey("accio_machine") && !jsonDoc.containsKey("accio_machine_response")) {
              
            }
          } else {
            webSocket.sendTXT("Erro: JSON inválido recebido.");
          }
        } else {

          if (strcmp((char*)payload, "activate all") == 0) {
            ativarRele();
            ativarRele2();
          } 
          else if (strcmp((char*)payload, "firmware") == 0) {
            uint32_t versiondata = nfc.getFirmwareVersion(); 
            if (versiondata) {
              String firmware = "PN532 Firmware: ";
              firmware += String(versiondata);
              webSocket.sendTXT(firmware); 
            } 
            else {
              webSocket.sendTXT("Erro: PN532 não encontrado"); 
            }
          }
          else if (strcmp((char*)payload, "log") == 0) {
            LogResponse();
          }
        }
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

void reiniciar_pn532_callback(){
  // Reinicia o PN532
  digitalWrite(PN532_RESET_PIN, LOW);// pn532 entra em modo suspensão
  rsto_ativo = false;
  timerAtivarRsto.start();// Inicia e configura o pn532 no tempo determinado

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
      enviarInsertedJson(uidAtual); 
    }
  }
  // Se não foi encontrado um cartão RFID, verifica se o ultimo UID lido foi diferente de vazio
  else {
    if (uidAnterior!= "") {
      enviarRemovedJson(uidAnterior); 
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

void iniciarPn532_callback(int I2C_SDA, int I2C_SCL) {
  digitalWrite(PN532_RESET_PIN, HIGH);
  rsto_ativo = true;
  Wire.begin(I2C_SDA, I2C_SCL);// Inicializa a interface I2C com os pinos SDA e SCL.
  nfc.begin();// Inicializa o módulo PN532
  uint32_t versiondata = nfc.getFirmwareVersion();
  if (!versiondata) {} 
  else {
    nfc.SAMConfig(); // PN532 configurar para operar no modo de leitura.
  }
}

void inicializarGPIOS() {
  pinMode(RELE1_PIN, OUTPUT);
  pinMode(RELE2_PIN, OUTPUT); 
  pinMode(PN532_RESET_PIN, OUTPUT);
}

void testarReles() {
  digitalWrite(RELE1_PIN, LOW);
  digitalWrite(RELE2_PIN, LOW);
  delay(3000);
  digitalWrite(RELE2_PIN, HIGH);
  digitalWrite(RELE1_PIN, HIGH);
}

String obterEnderecoMAC() {
  String macString = ETH.macAddress();
  return macString;
}
void enviarInsertedJson(const String& UID_INSERTED) {
  // Criar o objeto JSON
  StaticJsonDocument<256> jsonDoc;

  // Gerar um requestId aleatório
  String requestId = String(random(10000000, 99999999));

  // Obter o endereço MAC
  String macAddress = obterEnderecoMAC();

  // Adicionar dados ao JSON
  jsonDoc["report"]["inserted"]["rfid"] = UID_INSERTED;
  jsonDoc["report"]["inserted"]["station_mac"] = macAddress;
  jsonDoc["requestId"] = requestId;

  // Converter JSON para String
  String jsonString;
  serializeJson(jsonDoc, jsonString);

  // Enviar JSON para todos os clientes conectados
  webSocket.sendTXT(jsonString);
}

void enviarRemovedJson(const String& UID_REMOVED) {
  // Criar o objeto JSON
  StaticJsonDocument<256> jsonDoc;

  // Gerar um requestId aleatório
  String requestId = String(random(10000000, 99999999));

  // Adicionar dados ao JSON
  jsonDoc["report"]["removed"]["rfid"] = UID_REMOVED;
  jsonDoc["requestId"] = requestId;

  // Converter JSON para String
  String jsonString;
  serializeJson(jsonDoc, jsonString);

  // Enviar JSON para todos os clientes conectados
  webSocket.sendTXT(jsonString);
}

void accioMachineResponse(const String& UID_ATUAL){
StaticJsonDocument<256> responseJson;

String requestId = String(random(10000000, 99999999));

responseJson["accio_machine_response"]["rfid"] = UID_ATUAL; 
responseJson["accio_machine_response"]["childId"] = obterEnderecoMAC(); 
responseJson["requestId"] = requestId;

String respostaString;

serializeJson(responseJson, respostaString);

webSocket.sendTXT(respostaString);
}
void LogResponse() {
  // Criar o objeto JSON
  StaticJsonDocument<1024> jsonDoc;

  String macAddress = obterEnderecoMAC();

  // Adicionar dados ao JSON
  jsonDoc["log"]["timestamp"] = "2024-08-10T14:32:00Z";
  
  // Obter dados de rede
  String ipAddress = WiFi.localIP().toString();
  jsonDoc["log"]["deviceInfo"]["macAddress"] = macAddress;
  jsonDoc["log"]["deviceInfo"]["ipAddress"] = ipAddress;

  // Adicionar o RFID ao log
  jsonDoc["log"]["deviceInfo"]["rfid"] = uidAtual; // uidAtual contém o RFID atual

  // Obter a temperatura do núcleo
  float temperature = 40.0; // obter temperatura do núcleo Xtensa
  jsonDoc["log"]["systemStatus"]["coreTemperature"] = temperature;

  // Obter o tempo de atividade
  unsigned long uptime = millis() / 1000;
  jsonDoc["log"]["systemStatus"]["uptime"] = String(uptime / 86400) + " days " + String((uptime % 86400) / 3600) + " hours " + String((uptime % 3600) / 60) + " minutes";

  // Obter a versão do firmware do PN532
  uint32_t versiondata = nfc.getFirmwareVersion();
  jsonDoc["log"]["pn532Firmware"]["version"] = String(versiondata);
  jsonDoc["log"]["pn532Firmware"]["status"] = (versiondata > 0) ? "OK" : "Erro";



  // Converter JSON para String
  String jsonString;
  serializeJson(jsonDoc, jsonString);

  // Enviar JSON para todos os clientes conectados
  webSocket.sendTXT(jsonString);
}

void setup() {
  btStop();// desativar o Bluetooth
  inicializarGPIOS();
  testarReles();
  iniciarPn532_callback(I2C_SDA, I2C_SCL); // Configura o PN532 para operar no modo de leitura.
  iniciarEthernet();

  webSocket.begin("192.168.1.150", 8080,"/");
  webSocket.onEvent(webSocketEvent);

  // Iniciar a tasks
  timerLerRfid.start();
  timerGerenciarErros.start();
  timerReiniciarPn532.start();
  webSocket.setReconnectInterval(1000);
}

void loop() {
  // Tasks do TickTwo 
  timerGerenciarErros.update();
  timerLerRfid.update();
  timerDesativarRele1.update();
  timerDesativarRele2.update();
  timerReiniciarPn532.update();
  timerAtivarRsto.update();

  // Executa o loop do WebSocketsServer
  webSocket.loop();
}