package com.tcg_tuner.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class TCGTuner extends Activity {

    private final String[] permissionRequests = { "android.permission.BLUETOOTH_CONNECT" };
    private String esp32Address = "EC:DA:3B:AB:08:BE";
    private String esp32Service = "5f804f25-4bd9-457a-ac2d-ba39563d9b66";
    private String esp32Characteristic = "bbe3aeba-fe89-464f-9a3b-b845b758b239";

    private HashMap<String, String> rfidMap = new HashMap<String, String>();

    SharedPreferences sharedPreferences;

    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice bluetoothDevice;
    BluetoothGatt bluetoothGatt;
    BluetoothGattService bluetoothGattService;
    BluetoothGattCharacteristic bluetoothGattCharacteristic;

    private void ShowMessage(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setNeutralButton("OK", null);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    protected final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTING: {
                    runOnUiThread(() -> { Toast.makeText(getApplicationContext(), "Connecting", Toast.LENGTH_SHORT).show(); });
                    break;
                }
                case BluetoothProfile.STATE_CONNECTED: {
                    runOnUiThread(() -> { Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show(); });
                    gatt.discoverServices();
                    break;
                }
                case BluetoothProfile.STATE_DISCONNECTING: {
                    runOnUiThread(() -> { Toast.makeText(getApplicationContext(), "Disconnecting", Toast.LENGTH_SHORT).show(); });
                    break;
                }
                case BluetoothProfile.STATE_DISCONNECTED: {
                    runOnUiThread(() -> { Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show(); });
                    runOnUiThread(() -> { Toast.makeText(getApplicationContext(), "Reconnecting", Toast.LENGTH_SHORT).show(); });
                    gatt.connect();
                    break;
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            runOnUiThread(() -> { Toast.makeText(getApplicationContext(), "Service Discovered", Toast.LENGTH_SHORT).show(); });

            bluetoothGattService = gatt.getService(UUID.fromString(esp32Service));

            if (bluetoothGattService == null) {
                runOnUiThread(() -> { ShowMessage("ERROR", "Failed to get BluetoothGattService!\n\nUUID:\n" + esp32Service); });
                return;
            }

            bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(esp32Characteristic));

            if (bluetoothGattCharacteristic == null) {
                runOnUiThread(() -> { ShowMessage("ERROR", "Failed to get BluetoothGattCharacteristic!\n\nUUID:\n" + esp32Characteristic); });
                return;
            }

            gatt.setCharacteristicNotification(bluetoothGattCharacteristic, true);

            List<BluetoothGattDescriptor> bluetoothGattDescriptors = bluetoothGattCharacteristic.getDescriptors();

            for (BluetoothGattDescriptor bluetoothGattDescriptor : bluetoothGattDescriptors)
            {
                bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(bluetoothGattDescriptor);
            }

            runOnUiThread(() -> { Toast.makeText(getApplicationContext(), "Enabled Notifications", Toast.LENGTH_SHORT).show(); });
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            runOnUiThread(() -> { Toast.makeText(getApplicationContext(), "Read", Toast.LENGTH_SHORT).show(); });
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            final byte[] data = characteristic.getValue();
            final StringBuilder stringBuilder = new StringBuilder(data.length);

            for (byte byteChar : data) {
                stringBuilder.append(String.format("%02X", byteChar));
            }

            MediaPlayer mediaPlayer;
            String tagID = stringBuilder.toString().replaceAll("\\s", "");
            String displayText = tagID;

            // Load the sound that is referenced by RFID.txt if possible, otherwise load a default resource sound
            if (rfidMap.containsKey(tagID)) {
                String filepathSound = getExternalFilesDir(null).getAbsolutePath() + "/" + rfidMap.get(tagID);
                displayText += "\n↓\n" + rfidMap.get(tagID);

                if (new File(filepathSound).exists()) {
                    mediaPlayer = MediaPlayer.create(getApplicationContext(), Uri.parse(filepathSound));
                } else {
                    displayText += "\n(MISSING)";
                    mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.play);
                }
            } else {
                displayText += "\n(MISSING)";
                mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.play);
            }

            ((TextView)findViewById(R.id.tagID)).setText(displayText);

            // Play the sound and release the player when finished
            mediaPlayer.setOnCompletionListener((MediaPlayer m) -> {m.release(); });
            mediaPlayer.start();

            runOnUiThread(() -> { Toast.makeText(getApplicationContext(), tagID, Toast.LENGTH_SHORT).show(); });
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Request all the app permissions at startup
        // Functions that require permissions throw exceptions when denied
        requestPermissions(permissionRequests, 0);
    }

    private void Start() {
        // Inform the user in case a permission is required
        for (String permission : permissionRequests) {
            if (checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED) {
                ShowMessage("PERMISSION DENIED", permission.substring(19) + " needs to be permitted for this app to work correctly!\n\nPlease go to the app settings and grant the permission.");
                return;
            }
        }

        // Load saved application preferences
        sharedPreferences = getSharedPreferences("TCG-Tuner", MODE_PRIVATE);
        esp32Address = sharedPreferences.getString("esp32Address", esp32Address);
        esp32Service = sharedPreferences.getString("esp32Service", esp32Service);
        esp32Characteristic = sharedPreferences.getString("esp32Characteristic", esp32Characteristic);

        // Update UI preferences elements
        ((TextView)findViewById(R.id.esp32AddressEdit)).setText(esp32Address);
        ((TextView)findViewById(R.id.esp32ServiceEdit)).setText(esp32Service);
        ((TextView)findViewById(R.id.esp32CharacteristicEdit)).setText(esp32Characteristic);
        ((TextView)findViewById(R.id.storageDirectoryText)).setText(getExternalFilesDir(null).getAbsolutePath() + "/");

        // Save preferences on UI button click
        findViewById(R.id.buttonSave).setOnClickListener(view -> {
            sharedPreferences.edit().putString("esp32Address", String.valueOf(((TextView)findViewById(R.id.esp32AddressEdit)).getText())).commit();
            sharedPreferences.edit().putString("esp32Service", String.valueOf(((TextView)findViewById(R.id.esp32ServiceEdit)).getText())).commit();
            sharedPreferences.edit().putString("esp32Characteristic", String.valueOf(((TextView)findViewById(R.id.esp32CharacteristicEdit)).getText())).commit();
        });

        // Set all the UI callbacks
        findViewById(R.id.buttonPreferences).setOnClickListener(view -> {
            View linearLayoutInner = findViewById(R.id.linearLayoutInner);

            if (linearLayoutInner.getVisibility() == View.VISIBLE) {
                linearLayoutInner.setVisibility(View.GONE);
                findViewById(R.id.tagID).setVisibility(View.VISIBLE);
            } else {
                linearLayoutInner.setVisibility(View.VISIBLE);
                findViewById(R.id.tagID).setVisibility(View.GONE);
            }
        });

        // Parse the RFID.txt file from the storage directory into a map
        // Each line in the file maps an ID to a sound filename e.g.
        // D368B20D -> OP02-01.wav
        try {
            FileReader rfidFileReader = new FileReader(getExternalFilesDir(null).getAbsolutePath() + "/RFID.txt");
            BufferedReader rfidBufferedReader = new BufferedReader(rfidFileReader);
            String rfidLine;

            while((rfidLine = rfidBufferedReader.readLine()) != null) {
                String[] splits = rfidLine.split("->", 2);
                String tagID = splits[0].replaceAll("\\s", "");
                String filenameSound = splits[1].trim();
                rfidMap.put(tagID, filenameSound);
            }
        } catch (Exception e) {
            ShowMessage("ERROR", "Failed parsing \"" + getExternalFilesDir(null).getAbsolutePath() + "/RFID.txt\"\n\nMake sure to place an RFID.txt file into the storage directory that contains mappings from tag ID to sound filename separated by \"->\" in each line. TCG-Tuner will then play the sound file that is referenced by the ID. Make sure to also place all audio files in the storage directory.\n\nExample RFID.txt content:\nD368B20D -> OP02-01.wav\n8320DC0F -> OP03-17.wav");
        }

        // Determine whether BLE is supported on the device
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            ShowMessage("ERROR", "BLE not supported by this device!");
            return;
        }

        // Initialize Bluetooth
        bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);

        if (bluetoothManager == null) {
            ShowMessage("ERROR", "Failed to get BluetoothManager!");
            return;
        }

        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            ShowMessage("ERROR", "Failed to get BluetoothAdapter!");
            return;
        }

        try {
            bluetoothDevice = bluetoothAdapter.getRemoteDevice(esp32Address);
        } catch (Exception e) {
            ShowMessage("ERROR", e.getMessage());
        }

        if (bluetoothDevice == null) {
            ShowMessage("ERROR", "Failed to get remote BluetoothDevice!");
            return;
        }

        bluetoothGatt = bluetoothDevice.connectGatt(this, false, gattCallback);

        if (bluetoothGatt == null) {
            ShowMessage("ERROR", "Failed to connect BluetoothGatt!");
            return;
        }

        Toast.makeText(getApplicationContext(), "Starting", Toast.LENGTH_SHORT).show();
    }
}
