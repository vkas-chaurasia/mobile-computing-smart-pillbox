#include "Adafruit_TinyUSB.h" 
#include <bluefruit.h> 

int lightSensor = A0; 
int tiltSensor = 9;

BLEDis deviceInfoService;  // Device information service 
BLEBas batteryService;   // Battery service

//6E400001-B5A3-F393-E0A9-E50E24DCCA9E
//6E400002-B5A3-F393-E0A9-E50E24DCCA9E
//6E400003-B5A3-F393-E0A9-E50E24DCCA9E

// Nordic UART BLE Service UUIDs 
uint8_t const uartServiceUuid[] = { 0x9E, 0xCA, 0xDC, 0x24, 0x0E, 0xE5, 0xA9, 0xE0, 0x93, 0xF3, 0xA3, 0xB5, 0x01, 0x00, 0x40, 0x6E };
uint8_t const rxCharacteristicUuid[] = { 0x9E, 0xCA, 0xDC, 0x24, 0x0E, 0xE5, 0xA9, 0xE0, 0x93, 0xF3, 0xA3, 0xB5, 0x02, 0x00, 0x40, 0x6E };
uint8_t const txCharacteristicUuid[] = { 0x9E, 0xCA, 0xDC, 0x24, 0x0E, 0xE5, 0xA9, 0xE0, 0x93, 0xF3, 0xA3, 0xB5, 0x03, 0x00, 0x40, 0x6E };

uint16_t mtu; // Maximum Transmission Unit
BLEService uartService = BLEService(uartServiceUuid);
BLECharacteristic rxCharacteristic = BLECharacteristic(rxCharacteristicUuid);  
BLECharacteristic txCharacteristic = BLECharacteristic(txCharacteristicUuid);


void connectedCallback(uint16_t connHandle) {
  char centralName[32] = { 0 };
  BLEConnection *connection = Bluefruit.Connection(connHandle);
  connection->getPeerName(centralName, sizeof(centralName));
  Serial.print(connHandle);
  Serial.print(", connected to ");
  Serial.print(centralName);
  Serial.println();
}


void disconnectedCallback(uint16_t connHandle, uint8_t reason) {
  Serial.print(connHandle);
  Serial.print(" disconnected, reason = ");
  Serial.println(reason);
  Serial.println("Advertising ...");
}


void cccdCallback(uint16_t connHandle, BLECharacteristic* characteristic, uint16_t cccdValue) {
  if (characteristic->uuid == txCharacteristic.uuid) {
    Serial.print("UART 'Notify', ");
    if (characteristic->notifyEnabled()) {
      Serial.println("enabled");
    } else {
      Serial.println("disabled");
    }
  }
}

void writeCallback(uint16_t connHandle, BLECharacteristic* characteristic, uint8_t* rxData, uint16_t len) {
  // Reserved for commands from central
}


void setupUartService() {
  uartService.begin();

  txCharacteristic.setProperties(CHR_PROPS_NOTIFY);
  txCharacteristic.setPermission(SECMODE_OPEN, SECMODE_NO_ACCESS);
  txCharacteristic.setMaxLen(mtu);
  txCharacteristic.setCccdWriteCallback(cccdCallback);
  txCharacteristic.begin();

  rxCharacteristic.setProperties(CHR_PROPS_WRITE | CHR_PROPS_WRITE_WO_RESP);
  rxCharacteristic.setPermission(SECMODE_NO_ACCESS, SECMODE_OPEN);
  rxCharacteristic.setMaxLen(mtu);
  rxCharacteristic.setWriteCallback(writeCallback, true);
  rxCharacteristic.begin();
}

void startAdvertising() {
  Bluefruit.Advertising.addFlags(BLE_GAP_ADV_FLAGS_LE_ONLY_GENERAL_DISC_MODE);
  Bluefruit.Advertising.addTxPower();
  Bluefruit.Advertising.addService(uartService);
  Bluefruit.Advertising.addName();
  
  const int fastModeInterval = 32;     
  const int slowModeInterval = 244;
  const int fastModeTimeout = 30;
  Bluefruit.Advertising.restartOnDisconnect(true);
  Bluefruit.Advertising.setInterval(fastModeInterval, slowModeInterval);
  Bluefruit.Advertising.setFastTimeout(fastModeTimeout); 
  Bluefruit.Advertising.start(0);
  Serial.println("Advertising ...");
}

void setup() {
  Serial.begin(115200);
  while (!Serial) { delay(10); }
  Serial.println("Setup");
  
  Bluefruit.begin();
  Bluefruit.setName("pillbox");
  Bluefruit.Periph.setConnectCallback(connectedCallback);
  Bluefruit.Periph.setDisconnectCallback(disconnectedCallback);

  deviceInfoService.setManufacturer("Team A");
  deviceInfoService.setModel("Pillbox v1.0");
  deviceInfoService.begin();

  batteryService.begin();
  batteryService.write(100);

  pinMode(tiltSensor, INPUT);

  mtu = Bluefruit.getMaxMtu(BLE_GAP_ROLE_PERIPH);

  setupUartService();
  startAdvertising();

}

void loop() {
  if (Bluefruit.connected()) {
    int rawLight = analogRead(lightSensor);
    int lightPercent = map(rawLight, 0, 1023, 0, 100);
    int tiltState = digitalRead(tiltSensor);

    String data = "light:" + String(lightPercent) + ";tilt:" + String(tiltState) + "\n";

    uint8_t txData[mtu];
    data.getBytes(txData, mtu);

    if (!txCharacteristic.notify(txData, strlen((char*)txData))) {
      Serial.println("Notify error");
    } else {
      Serial.println("Sent: " + data);
    }
  }

  delay(1000);
}