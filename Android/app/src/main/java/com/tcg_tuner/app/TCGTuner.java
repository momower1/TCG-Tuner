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
import android.widget.Toast;

import java.io.File;
import java.util.List;
import java.util.UUID;

public class TCGTuner extends Activity {

    private String esp32Address = "EC:DA:3B:AB:08:BE";
    private String esp32Service = "5f804f25-4bd9-457a-ac2d-ba39563d9b66";
    private String esp32Characteristic = "bbe3aeba-fe89-464f-9a3b-b845b758b239";

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
            runOnUiThread(() -> { Toast.makeText(getApplicationContext(), "onConnectionStateChange", Toast.LENGTH_LONG).show(); });

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            runOnUiThread(() -> { Toast.makeText(getApplicationContext(), "onServicesDiscovered", Toast.LENGTH_LONG).show(); });

            bluetoothGattService = gatt.getService(UUID.fromString(esp32Service));

            if (bluetoothGattService == null) {
                runOnUiThread(() -> { ShowMessage("ERROR", "Failed to get BluetoothGattService!"); });
                return;
            }

            bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(esp32Characteristic));

            if (bluetoothGattCharacteristic == null) {
                runOnUiThread(() -> { ShowMessage("ERROR", "Failed to get BluetoothGattCharacteristic!"); });
                return;
            }

            bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, true);

            List<BluetoothGattDescriptor> bluetoothGattDescriptors = bluetoothGattCharacteristic.getDescriptors();

            for (BluetoothGattDescriptor bluetoothGattDescriptor : bluetoothGattDescriptors)
            {
                bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                bluetoothGatt.writeDescriptor(bluetoothGattDescriptor);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            runOnUiThread(() -> { Toast.makeText(getApplicationContext(), "onCharacteristicRead", Toast.LENGTH_LONG).show(); });
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            final byte[] data = characteristic.getValue();
            final StringBuilder stringBuilder = new StringBuilder(data.length);

            for (byte byteChar : data) {
                stringBuilder.append(String.format("%02X", byteChar));
            }

            String filepathSound = getExternalFilesDir(null).getAbsolutePath();
            filepathSound += "/" + stringBuilder.toString() + ".wav";

            MediaPlayer mediaPlayer;

            // Either play tag specific sound if it exists or fallback to default sound
            if (new File(filepathSound).exists()) {
                mediaPlayer = MediaPlayer.create(getApplicationContext(), Uri.parse(filepathSound));
            } else {
                filepathSound += " (MISSING)";
                mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.play);
            }

            mediaPlayer.setOnCompletionListener((MediaPlayer m) -> {m.release(); });
            mediaPlayer.start();

            final String message = stringBuilder.toString() + "\n" + filepathSound;
            runOnUiThread(() -> { Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show(); });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load saved application preferences
        sharedPreferences = getSharedPreferences("TCG-Tuner", MODE_PRIVATE);

        esp32Address = sharedPreferences.getString("esp32Address", esp32Address);
        esp32Service = sharedPreferences.getString("esp32Service", esp32Service);
        esp32Characteristic = sharedPreferences.getString("esp32Characteristic", esp32Characteristic);

        // TODO: Add UI elements to edit these strings and update preferences like here
        sharedPreferences.edit().putString("esp32Address", esp32Address).commit();
        sharedPreferences.edit().putString("esp32Service", esp32Service).commit();
        sharedPreferences.edit().putString("esp32Characteristic", esp32Characteristic).commit();

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

        bluetoothDevice = bluetoothAdapter.getRemoteDevice(esp32Address);

        if (bluetoothDevice == null) {
            ShowMessage("ERROR", "Failed to get remote BluetoothDevice!");
            return;
        }

        bluetoothGatt = bluetoothDevice.connectGatt(this, false, gattCallback);

        if (bluetoothGatt == null) {
            ShowMessage("ERROR", "Failed to connect BluetoothGatt!");
            return;
        }
    }
}
