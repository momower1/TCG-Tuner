# TCG-Tuner
TCG-Tuner is the combination of an electronic playmat with an android app that detects individual cards and plays corresponding sound effects in trading card games. Each card needs to be double sleeved and an RFID tag sticker with a unique ID is placed outside of the inner sleeve. The TCG-Tuner electronic playmat has 8 RFID scanners, one for each card zone, and detects when a card is played and notifies the TCG-Tuner android app about its card ID over bluetooth. The app then plays a corresponding sound from the app storage directory. The BLE connection requires the device address, service UUID and characteristic UUID to be configured correctly in the app preferences menu. Then, when a tag ID is received, the app will look for the \"RFID.txt\" file in the app storage directory. This file contains mappings from tag ID to sound filename separated by \"->\" in each line. TCG-Tuner will then play the sound file that is referenced by the ID. All audio files needd to be placed into the storage directory. The storage directory path can be obtained from the app preferences menu.

Example RFID.txt content:
<br>``D368B20D -> OP02-01.wav``
<br>``8320DC0F -> OP03-17.wav``

![TCG-Tuner](TCG-Tuner.jpg?raw=true)

# Hardware Setup
- Android Smartphone
- USB-Micro Power Supply
- Electronics
  - 1x ESP32-C3-MINI-1 (Development Board) https://de.aliexpress.com/item/1005004998207821.html
  - 8x RFID-RC522 Reader https://de.aliexpress.com/item/32801850680.html
  - 50x NFC Tag Sticker (NTAG215) https://www.amazon.de/dp/B0C623J1C7
  - 7x Colored Cables 24 AWG https://www.amazon.de/dp/B0B15DCC17
  - 3x Pin Header 40 Pins 2,54mm 90 Degrees https://www.ebay.de/itm/334326227743
  - 3x Pin Socket 40 Pins 2,54mm https://www.ebay.de/itm/153869572374
  - 6x Rubber Band
- 3D Printing
  - Print Settings
    - Material: PLA
    - Nozzle Size: 0.2
    - Print Temperature: 210°C
    - Bed Temperature: 60°C
    - Print Speed: 60mm/s
    - Adhesion Type: Skirt
    - Support: Off
    - Infill: 25%
  - Parts
    - 1x Bottom 1-9
    - 1x Top 1-9
    - 16x Connector
- Playmat Construction
  - 3D Print all parts
  - Bottom parts are split into 3 foldable segments consisting of 3 parts
  - The segments are connected with rubber bands, making them foldable
  - For each segment, glue the parts together using the connectors
  - Place the RFID sensors into their slots
  - Place the ESP32-C3-MINI-1 into its slot
  - Connect the pins with wires
    - For all RFID sensors
      - Solder SCK together and connect it to GPIO6 (FSPICLK)
      - Solder MOSI together and connect it to GPIO7 (FSPID)
      - Solder MISO together and connect it to GPIO2 (FSPIQ)
      - Solder RST together and connect it to GPIO10 (FSPICSO)
      - Solder GND together and connect it to GND
      - Solder 3.3V together and connect it to 3.3V
    - For each individual RFID sensor
      - Connect its SDA pin to a GPIO pin
      - E.g. One Piece TCG
        - SDA Character1 connected to GPIO0
        - SDA Character2 connected to GPIO1
        - SDA Character3 connected to GPIO3
        - SDA Character4 connected to GPIO4
        - SDA Character5 connected to GPIO5
        - SDA Event connected to GPIO9
        - SDA Leader connected to GPIO18
        - SDA Stage connected to GPIO19
  - Top parts are clipped onto the bottom parts
    - Connect the bottom segments with 3 rubber bands each and layout wires beforehand

# Software Setup
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

## Android App
- Download Android Studio https://developer.android.com/studio
- Open the project
