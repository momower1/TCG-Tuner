#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLE2902.h>

// https://www.uuidgenerator.net/
// Choose unique identifiers for the BLE service and characteristic
const char* uuidService = "5f804f25-4bd9-457a-ac2d-ba39563d9b66";
const char* uuidCharacteristic = "bbe3aeba-fe89-464f-9a3b-b845b758b239";

BLEServer* bleServer = nullptr;
BLECharacteristic* bleCharacteristic = nullptr;
uint32_t value = 0;

class ServerCallbacks: public BLEServerCallbacks
{
    void onConnect(BLEServer* bleServer)
    {
      Serial.println("Connected");
      bleServer->startAdvertising();
      Serial.println("Restarted advertising...");
    };

    void onDisconnect(BLEServer* bleServer)
    {
      Serial.println("Disconnected");
      bleServer->startAdvertising();
      Serial.println("Restarted advertising...");
    }
};

BLE2902 ble2902;
ServerCallbacks serverCallbacks;

void setup()
{
  Serial.begin(115200);

  BLEDevice::init("ESP32-C3-MINI-1");

  // Create the BLE Server
  bleServer = BLEDevice::createServer();
  bleServer->setCallbacks(&serverCallbacks);

  // Create the BLE Service
  BLEService *bleService = bleServer->createService(uuidService);

  // Create a BLE Characteristic
  bleCharacteristic = bleService->createCharacteristic(uuidCharacteristic, BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
  bleCharacteristic->addDescriptor(&ble2902);

  // Start the service
  bleService->start();

  // Start advertising (automatically stops once a client connects)
  BLEAdvertising *bleAdvertising = BLEDevice::getAdvertising();
  bleAdvertising->addServiceUUID(uuidService);
  bleAdvertising->setScanResponse(false);
  bleAdvertising->setMinPreferred(0);
  BLEDevice::startAdvertising();

  Serial.println("Started advertising...");
}

void loop()
{
  if (bleServer->getConnectedCount() > 0)
  {
    bleCharacteristic->setValue((uint8_t*)&value, sizeof(value));
    bleCharacteristic->notify();
    Serial.println(value);
    value++;
    delay(100);
  }
}