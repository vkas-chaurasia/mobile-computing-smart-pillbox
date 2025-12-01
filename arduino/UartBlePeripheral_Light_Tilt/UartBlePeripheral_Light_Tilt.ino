#include "Adafruit_TinyUSB.h"
#include <bluefruit.h>

// Sensor Pins
int lightSensor = A0;
int tiltSensor = 9;

bool lightSubscribed = false;
bool tiltSubscribed = false;

// Standard BLE Services
BLEDis deviceInfoService;
BLEBas batteryService;

// Custom GATT Service API
// Pillbox Service UUID: 0000AB00-B5A3-F393-E0A9-E50E24DCCA9E 
// Light Sensor Characteristic UUID: 0000AB01-B5A3-F393-E0A9-E50E24DCCA9E
 // Tilt Sensor Characteristic UUID: 0000AB02-B5A3-F393-E0A9-E50E24DCCA9E

// Custom GATT Service UUIDs
uint8_t const pillboxServiceUuid[]     = { 0x9E, 0xCA, 0xDC, 0x24, 0x0E, 0xE5, 0xA9, 0xE0, 0x93, 0xF3, 0xA3, 0xB5, 0x00, 0xAB, 0x00, 0x00 };
uint8_t const lightCharacteristicUuid[] = { 0x9E, 0xCA, 0xDC, 0x24, 0x0E, 0xE5, 0xA9, 0xE0, 0x93, 0xF3, 0xA3, 0xB5, 0x01, 0xAB, 0x00, 0x00 };
uint8_t const tiltCharacteristicUuid[]  = { 0x9E, 0xCA, 0xDC, 0x24, 0x0E, 0xE5, 0xA9, 0xE0, 0x93, 0xF3, 0xA3, 0xB5, 0x02, 0xAB, 0x00, 0x00 };

// Service and Characteristics
BLEService pillboxService = BLEService(pillboxServiceUuid);
BLECharacteristic lightCharacteristic = BLECharacteristic(lightCharacteristicUuid);
BLECharacteristic tiltCharacteristic  = BLECharacteristic(tiltCharacteristicUuid);

// Connection callbacks
void connectedCallback(uint16_t connHandle) {
  Serial.print(connHandle);
  Serial.println(" connected");
}

void disconnectedCallback(uint16_t connHandle, uint8_t reason) {
  Serial.print(connHandle);
  Serial.print(" disconnected, reason=");
  Serial.println(reason);

  lightSubscribed = false;
  tiltSubscribed = false;
}

// CCCD callback
void myCccdCallback(uint16_t connHandle, BLECharacteristic* chr, uint16_t cccdValue) {
  bool enabled = cccdValue & BLE_GATT_HVX_NOTIFICATION;

  if (chr->uuid == lightCharacteristic.uuid) {
    lightSubscribed = enabled;
    Serial.print("Light Sensor: ");
    Serial.println(enabled ? "Subscribed" : "Unsubscribed");
  }

  else if (chr->uuid == tiltCharacteristic.uuid) {
    tiltSubscribed = enabled;
    Serial.print("Tilt Sensor: ");
    Serial.println(enabled ? "Subscribed" : "Unsubscribed");
  }
}

// Setup pillbox service
void setupPillboxService() {
  pillboxService.begin();

  // Light characteristic
  lightCharacteristic.setProperties(CHR_PROPS_READ | CHR_PROPS_NOTIFY);
  lightCharacteristic.setPermission(SECMODE_OPEN, SECMODE_NO_ACCESS);
  lightCharacteristic.setFixedLen(1);
  lightCharacteristic.setCccdWriteCallback(myCccdCallback);
  lightCharacteristic.begin();
  lightCharacteristic.write8(0);

  // Tilt characteristic
  tiltCharacteristic.setProperties(CHR_PROPS_READ | CHR_PROPS_NOTIFY);
  tiltCharacteristic.setPermission(SECMODE_OPEN, SECMODE_NO_ACCESS);
  tiltCharacteristic.setFixedLen(1);
  tiltCharacteristic.setCccdWriteCallback(myCccdCallback);
  tiltCharacteristic.begin();
  tiltCharacteristic.write8(0);
}

void startAdvertising() {
  Bluefruit.Advertising.addFlags(BLE_GAP_ADV_FLAGS_LE_ONLY_GENERAL_DISC_MODE);
  Bluefruit.Advertising.addName();
  Bluefruit.Advertising.addService(pillboxService);

  Bluefruit.Advertising.restartOnDisconnect(true);
  Bluefruit.Advertising.setInterval(32, 244);
  Bluefruit.Advertising.setFastTimeout(30);
  Bluefruit.Advertising.start(0);

  Serial.println("Advertising...");
}

void setup() {
  Serial.begin(115200);
  while (!Serial) delay(10);

  Serial.println("Starting Pillbox BLE Service");

  Bluefruit.begin();
  Bluefruit.setName("pillbox");
  Bluefruit.Periph.setConnectCallback(connectedCallback);
  Bluefruit.Periph.setDisconnectCallback(disconnectedCallback);

  deviceInfoService.setManufacturer("Team A");
  deviceInfoService.setModel("Pillbox v1.0");
  deviceInfoService.begin();

  batteryService.begin();
  batteryService.write(100);

  setupPillboxService();
  startAdvertising();

  pinMode(tiltSensor, INPUT);
}

void loop() {

  if (Bluefruit.connected()) {

    uint8_t lightPercent = 0;
    uint8_t tiltState = 0;

    // Notify only if subscribed
    if (lightSubscribed) {
      int rawLight = analogRead(lightSensor);
      lightPercent = map(rawLight, 0, 1023, 0, 100);
      lightCharacteristic.notify8(lightPercent);
    }

    if (tiltSubscribed) {
      tiltState = digitalRead(tiltSensor);
      tiltCharacteristic.notify8(tiltState);
    }

    Serial.print("Light: ");
    Serial.print(lightPercent);
    Serial.print("% | Tilt: ");
    Serial.println(tiltState);
  }

  delay(1000);
}
