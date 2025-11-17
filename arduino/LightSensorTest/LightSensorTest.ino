#include "Adafruit_TinyUSB.h"

int light_sensor = A0; 

void setup() {
  Serial.begin(9600);
}

// the loop routine runs over and over again forever:
void loop() {
  int raw_light = analogRead(light_sensor);
  int light = map(raw_light, 0, 1023, 0, 100);
  Serial.print("Light level: "); 
  Serial.println(light);
  delay(100);
}

