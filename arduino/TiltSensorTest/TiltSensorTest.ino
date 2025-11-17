#include "Adafruit_TinyUSB.h"

int sensorPin = 9;

void setup() {
  Serial.begin(9600);
  pinMode(sensorPin, INPUT);
}

void loop() {
  int sensorState = digitalRead(sensorPin);
  Serial.println(sensorState);
  delay(1);
}
