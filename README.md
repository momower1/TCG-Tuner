# Developer Setup

## ESP32-C3-MINI-1
- Follow instructions from https://docs.espressif.com/projects/arduino-esp32/en/latest/installing.html
  - Install Arduino IDE from https://www.arduino.cc/en/Main/Software
  - Open _Preferences_ and set _Additional Board Manager URLs_ to `https://espressif.github.io/arduino-esp32/package_esp32_index.json`
  - Open _Tools->Board->Board Manager_ and install _esp32_ platform by Espressif Systems
  - Connect ESP32-C3-MINI-1 with USB and select _ESP32C3 Dev Module_ for the COM port
- For the RFID-RC522 board
  - In Arduino IDE open _Tools->Manage Libraries_ and install _MFRC522_ by GithubCommunity
- For Dictionary data type
  - In Arduino IDE open _Tools->Manage Libraries_ and install _Dictionary_ by Anatoli Arkhipenko
