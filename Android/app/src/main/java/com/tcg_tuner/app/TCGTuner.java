package com.tcg_tuner.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

public class TCGTuner extends Activity {

    private final String esp32AddressDefault = "EC:DA:3B:AB:08:BE";
    private final String esp32ServiceDefault = "5f804f25-4bd9-457a-ac2d-ba39563d9b66";
    private final String esp32CharacteristicDefault = "bbe3aeba-fe89-464f-9a3b-b845b758b239";

    SharedPreferences sharedPreferences;

    BluetoothAdapter bluetoothAdapter;

    private void ShowMessage(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setNeutralButton("OK", null);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load saved application preferences
        sharedPreferences = getSharedPreferences("TCG-Tuner", MODE_PRIVATE);

        String esp32Address = sharedPreferences.getString("esp32Address", esp32AddressDefault);
        String esp32Service = sharedPreferences.getString("esp32Service", esp32ServiceDefault);
        String esp32Characteristic = sharedPreferences.getString("esp32Characteristic", esp32CharacteristicDefault);

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
        final BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            ShowMessage("ERROR", "Failed to get BluetoothAdapter!");
            return;
        }

        Toast.makeText(this, "onCreate", Toast.LENGTH_LONG).show();
    }
}
