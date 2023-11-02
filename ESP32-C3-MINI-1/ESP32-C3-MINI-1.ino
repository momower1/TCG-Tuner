#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLE2902.h>
#include <SPI.h>
#include <MFRC522.h>

// https://www.uuidgenerator.net/
// Choose unique identifiers for the BLE service and characteristic
const char* uuidService = "5f804f25-4bd9-457a-ac2d-ba39563d9b66";
const char* uuidCharacteristic = "bbe3aeba-fe89-464f-9a3b-b845b758b239";

const int rfidCount = 2;
const byte rfidPinsSDA[rfidCount] = { 4, 5 };
const byte rfidPinRST = 9;

const byte spiPinSCK = 6;
const byte spiPinMISO = 2;
const byte spiPinMOSI = 7;

BLEServer* bleServer = nullptr;
BLECharacteristic* bleCharacteristic = nullptr;

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

MFRC522 rfids[rfidCount];

void setup()
{
  Serial.begin(115200);

  // RFID-RC522
  SPI.begin(spiPinSCK, spiPinMISO, spiPinMOSI, -1);

  for (int rfidIndex = 0; rfidIndex < rfidCount; rfidIndex++)
  {
    rfids[rfidIndex].PCD_Init(rfidPinsSDA[rfidIndex], rfidPinRST);
  }

  pinMode(rfidPinRST, OUTPUT);
  digitalWrite(rfidPinRST, LOW);

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
  for (int rfidIndex = 0; rfidIndex < rfidCount; rfidIndex++)
  {
    digitalWrite(rfidPinRST, HIGH);
    rfids[rfidIndex].PCD_Init();

    // Waiting a bit is necessary to reliably detect Mifare Ultralight tags
    delay(1);

    if (rfids[rfidIndex].PICC_IsNewCardPresent())
    {
      Serial.println("New card present");

      if (rfids[rfidIndex].PICC_ReadCardSerial())
      {
        MFRC522::PICC_Type piccType = rfids[rfidIndex].PICC_GetType(rfids[rfidIndex].uid.sak);
        Serial.print("RFID/NFC Tag Type: ");
        Serial.println(rfids[rfidIndex].PICC_GetTypeName(piccType));

        // print UID in Serial Monitor in the hex format
        Serial.print("UID:");
        
        for (int i = 0; i < rfids[rfidIndex].uid.size; i++)
        {
          Serial.print(rfids[rfidIndex].uid.uidByte[i] <= 0xF ? " 0" : " ");
          Serial.print(rfids[rfidIndex].uid.uidByte[i], HEX);
        }

        Serial.println();

        // BLE Notify
        if (bleServer->getConnectedCount() > 0)
        {
          bleCharacteristic->setValue((uint8_t*)rfids[rfidIndex].uid.uidByte, rfids[rfidIndex].uid.size);
          bleCharacteristic->notify();
          Serial.println("BLE Notify");
        }

        rfids[rfidIndex].PICC_HaltA();
        rfids[rfidIndex].PCD_StopCrypto1();
      }
    }

    digitalWrite(rfidPinRST, LOW);
  }
}