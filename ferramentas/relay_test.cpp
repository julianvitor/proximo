// Definição do pino do relé
const int relePin = 2;

void setup() {
  // Configura o pino do relé como saída
  pinMode(relePin, OUTPUT);
}

void loop() {
  // Ativa o relé (HIGH)
  digitalWrite(relePin, HIGH);
  delay(2000); // Mantém o relé ativado por 1 segundo (1000 ms)

  // Desativa o relé (LOW)
  digitalWrite(relePin, LOW);
  delay(2000);
  }
