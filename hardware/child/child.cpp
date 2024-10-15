// Codigo desenvolvido por Julian Carreiro.

// Boas praticas neste codigo.
// Funções callback são nomeadas seguindo o padrão: função_faz_algo_callback().
// Funções normais são nomeadas seguindo o padrão: funcaoFazAlgo().
// Variaveis são nomeadas em lowercase && snakecase.
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
#include <ArduinoJson.h>
#include "esp_system.h"


// Definição dos pinos
const int RELE1_PIN = 2; // Pino digital conectado ao primeiro relé
//const int RELE2_PIN = 4; // Pino digital conectado ao segundo relé
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
bool global_ethernet_conexao = false;
bool global_rele_ativo = false;
//bool rele2Ativo = false;
bool global_rsto_ativo = false;

int DURACAO_RELE = 28000;// Duração do rele ativado em milissegundos

// Variaveis globais
String global_uid_anterior;
String global_uid_atual;

// Protótipos de funções
void iniciarEthernet();
void gerenciar_erros_callback();
void ler_rfid_callback();
void desativar_rele_callback();
void reiniciar_pn532_callback();
void iniciarPn532_callback(int I2C_SDA, int I2C_SCL);// Iniciar o rsto, instanciar o PN532 e inicia a comunicacao em modo leitura
void logResponse();
void releaseMachineIfAvaliable(const JsonObject& JSON_RECEIVED_MESSAGE);
void accioMachineResponse(const JsonObject& JSON_OBJ);
void ativarRele();
void enviarInsertedJson(const String& UID_INSERTED);
String lerRfid();
// Instanciar sensor
PN532_I2C pn532_i2c(Wire);
PN532 nfc(pn532_i2c);

// Instanciar os timers(tasks)
TickTwo timerLerRfid(ler_rfid_callback, 4000, 0, MILLIS);
TickTwo timerReiniciarPn532(reiniciar_pn532_callback, 2000, 0, MILLIS);// Reinicia o PN532 a cada 180 segundos
TickTwo timerAtivarRsto([]() { iniciarPn532_callback(I2C_SDA, I2C_SCL); }, 100, 1, MILLIS);// Necessario uso de função lambda para passar como parametro
TickTwo timerGerenciarErros(gerenciar_erros_callback, 1000, 0, MILLIS);
TickTwo timerDesativarRele1(desativar_rele_callback, DURACAO_RELE, 1, MILLIS);
//TickTwo timerDesativarRele2(desativar_rele2_callback, DURACAO_RELE, 1, MILLIS);


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
      global_ethernet_conexao = true;
      break;
    case SYSTEM_EVENT_ETH_DISCONNECTED:
      global_ethernet_conexao = false;
      break;
    case SYSTEM_EVENT_ETH_STOP:
      global_ethernet_conexao = false;
      break;
    default:
      break;
  }
}
// Função que trata os eventos do WebSocket
void webSocketEvent(WStype_t type, uint8_t* payload, size_t length) {
  switch (type) {
      case WStype_TEXT:
        // Handle JSON
        if (payload[0] == '{') {
            String mensagemRecebida = String((char*)payload).substring(0, length);
            StaticJsonDocument<256> json_received_message;
            DeserializationError error = deserializeJson(json_received_message, mensagemRecebida);

            if (!error) { // Verifica se a desserialização foi bem-sucedida
                // accio_machine response
                if (json_received_message.containsKey("accio_machine") && !json_received_message.containsKey("accio_machine_response")) {
                  accioMachineResponse(json_received_message.as<JsonObject>());
                }
                // Activate relay if machine avaliable
                else if (json_received_message.containsKey("command") && json_received_message["command"].as<String>() == "activate") {
                  releaseMachineIfAvaliable(json_received_message.as<JsonObject>());
                }                
                // Activate all relay
                else if (json_received_message.containsKey("command") && json_received_message["command"].as<String>() == "activate_all") {
                    ativarRele();
                }
                //log response
                else if (json_received_message.containsKey("accio_log")){
                  logResponse();
                }
            } 
            else {
                webSocket.sendTXT("Erro: JSON inesperado recebido.");
            }
        } 
        else {}
        break;
  default:
    break;
  }
}

void gerenciar_erros_callback(){
  if (!global_ethernet_conexao) {
    // Se não estiver conectado, tenta inicializar a conexão Ethernet novamente
    ETH.begin(ETH_ADDR, ETH_POWER_PIN, ETH_MDC_PIN, ETH_MDIO_PIN, ETH_TYPE, ETH_CLK_MODE);
  }
}

void reiniciar_pn532_callback(){
  // Reinicia o PN532
  digitalWrite(PN532_RESET_PIN, LOW);// pn532 entra em modo suspensão
  global_rsto_ativo = false;
  timerAtivarRsto.start();// Inicia e configura o pn532 no tempo determinado
  iniciarPn532_callback(I2C_SDA, I2C_SCL);

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
    global_uid_atual = "";
    for (uint8_t i = 0; i < uidLength; i++) {
      if (uid[i] < 0x10) {  // quando um número é menor que 16 hexadecimal
        global_uid_atual += "0"; // adiciona um zero à esquerda  garantir que todos tenham dois dígitos. ex: A -> 0A hexadecimal
      }
      global_uid_atual += String(uid[i], HEX);
    }
    //verificar se o UID mudou ou se é a primeira vez que é lido
    if (global_uid_atual!= global_uid_anterior || global_uid_atual == "") {
      global_uid_anterior = global_uid_atual;
      enviarInsertedJson(global_uid_atual); 
    }
  }
  // Se não foi encontrado um cartão RFID, verifica se o ultimo UID lido foi diferente de vazio
  else {
    if (global_uid_anterior!= "") {
      enviarRemovedJson(global_uid_anterior); 
      global_uid_anterior = "";
      
    }
  }
}
void desativar_rele_callback() {
    digitalWrite(RELE1_PIN, LOW);
    global_rele_ativo = false;
}

void ativarRele() {
    digitalWrite(RELE1_PIN, HIGH);
    global_rele_ativo = true;
    timerDesativarRele1.start();

}

void iniciarEthernet(){
  WiFi.onEvent(WiFiEvent);
  ETH.begin(ETH_ADDR,ETH_POWER_PIN, ETH_MDC_PIN, ETH_MDIO_PIN, ETH_TYPE, ETH_CLK_MODE);

}

void iniciarPn532_callback(int I2C_SDA, int I2C_SCL) {
  digitalWrite(PN532_RESET_PIN, HIGH);
  global_rsto_ativo = true;
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
  //pinMode(RELE2_PIN, OUTPUT); 
  pinMode(PN532_RESET_PIN, OUTPUT);
}

void testarReles() {
  digitalWrite(RELE1_PIN, HIGH);
  delay(2000);
  digitalWrite(RELE1_PIN, LOW);
  delay(2000);
  digitalWrite(RELE1_PIN, HIGH);
  delay(2000);
  digitalWrite(RELE1_PIN, LOW);

}

String lerRfid(){
  //obter rfid conectado
  // Verifica se há um cartão RFID
  String uid_string;
  uint8_t success;
  uint8_t uid[7] = { 0, 0, 0, 0, 0, 0, 0 };  // Buffer para armazenar o UID lido
  uint8_t uidLength;                        // Comprimento do UID (4 ou 7 bytes dependendo do tipo de cartão)
  
  // Tenta ler um cartão RFID
  success = nfc.readPassiveTargetID(PN532_MIFARE_ISO14443A, uid, &uidLength, 100);
  
  // Se um cartão foi encontrado, converte o UID para uma string
  if (success) {
    uid_string = "";
    for (uint8_t i = 0; i < uidLength; i++) {
      if (uid[i] < 0x10) {  // quando um número é menor que 16 hexadecimal
        uid_string += "0"; // adiciona um zero à esquerda  garantir que todos tenham dois dígitos. ex: A -> 0A hexadecimal
      }
      uid_string += String(uid[i], HEX);
    }
  }
  return uid_string;
}

String obterEnderecoMAC() {
  String macString = ETH.macAddress();
  return macString;
}
void enviarInsertedJson(const String& UID_INSERTED) {
  // Criar o objeto JSON
  StaticJsonDocument<256> to_send_json_doc;

  String message_id = String(random(10000000, 99999999));
  String macAddress = obterEnderecoMAC();

  to_send_json_doc["inserted"]["rfid"] = UID_INSERTED;
  to_send_json_doc["inserted"]["station_mac"] = macAddress;
  to_send_json_doc["message_id"] = message_id;

  String to_send_json_string;
  serializeJson(to_send_json_doc, to_send_json_string);
  webSocket.sendTXT(to_send_json_string);
}

void enviarRemovedJson(const String& UID_REMOVED) {
  // Criar o objeto JSON
  StaticJsonDocument<256> jsonDoc;

  // Gerar um message_id aleatório
  String message_id = String(random(10000000, 99999999));

  String macAddress = obterEnderecoMAC();

  // Adicionar dados ao JSON
  jsonDoc["removed"]["rfid"] = UID_REMOVED;
  jsonDoc["removed"]["station_mac"] = macAddress;

  jsonDoc["message_id"] = message_id;
  // Converter JSON para String
  String jsonString;
  serializeJson(jsonDoc, jsonString);

  // Enviar JSON para todos os clientes conectados
  webSocket.sendTXT(jsonString);
}

void accioMachineResponse(const JsonObject& JSON_OBJ) {
  StaticJsonDocument<256> responseJson;
  String responseString;
  String message_id = String(random(10000000, 99999999));
  String uid_conectado = lerRfid();

  responseJson["accio_machine_response"]["rfid"] = uid_conectado; 
  responseJson["accio_machine_response"]["childId"] = obterEnderecoMAC(); 
  responseJson["message_id"] = message_id;

  serializeJson(responseJson, responseString);
  webSocket.sendTXT(responseString);
}

void logResponse() {
  // Criar o objeto JSON
  StaticJsonDocument<1024> to_send_json;
  String uid_conectado = "";
  String message_id = String(random(10000000, 99999999));
  String macAddress = obterEnderecoMAC();
  String ipAddress = ETH.localIP().toString();
  String response_firmware_version_data;
  unsigned long uptime = millis() / 1000;
  float temperature = 40.0;
  uint32_t firmware_version_data = nfc.getFirmwareVersion();
  if (firmware_version_data) {
    response_firmware_version_data = String(firmware_version_data);
  } 

  uid_conectado = lerRfid();

  to_send_json["response_log"]["device_info"]["mac_address"] = macAddress;
  to_send_json["response_log"]["device_info"]["ip_address"] = ipAddress;

  to_send_json["response_log"]["system_status"]["core_temperature"] = temperature;
  to_send_json["response_log"]["system_status"]["uptime"] = String(uptime / 86400) + " days " + String((uptime % 86400) / 3600) + " hours " + String((uptime % 3600) / 60) + " minutes";

  to_send_json["response_log"]["pn532"]["version"] = response_firmware_version_data;
  to_send_json["response_log"]["pn532"]["status"] = (firmware_version_data > 0) ? "OK" : "Erro";
  to_send_json["response_log"]["pn532"]["rfid"] = uid_conectado;

  to_send_json["message_id"] = message_id;
  // Converter JSON para String
  String jsonString;
  serializeJson(to_send_json, jsonString);

  // Enviar JSON para todos os clientes conectados
  webSocket.sendTXT(jsonString);
}

void releaseMachineIfAvaliable(const JsonObject& JSON_RECEIVED_MESSAGE){
  // se o rfid presente for o mesmo da mensagem. libera a maquina.
  if (JSON_RECEIVED_MESSAGE.containsKey("rfid")) {
    String rfid_requested = JSON_RECEIVED_MESSAGE["rfid"].as<String>(); // Obtém o valor associado à chave "rfid"
    if(!rfid_requested.isEmpty()){ 
      String uid_atual;
      uid_atual = lerRfid();     
      if (rfid_requested == uid_atual) {
        ativarRele(); 
        //playSound()
        //ENVIAR RESPOSTA de acionamento da maquina
      }
    }
  }
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
  webSocket.setReconnectInterval(50);
}

void loop() {
  // Tasks do TickTwo 
  timerGerenciarErros.update();
  timerLerRfid.update();
  timerDesativarRele1.update();
  timerReiniciarPn532.update();
  timerAtivarRsto.update();

  // Executa o loop do WebSocketsServer
  webSocket.loop();
}