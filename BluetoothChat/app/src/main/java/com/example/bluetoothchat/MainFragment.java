package com.example.bluetoothchat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.io.InputStream;
import java.io.IOException;

public class MainFragment extends Fragment {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice selectedDevice;
    private ArrayAdapter<String> deviceListAdapter;

    private static final UUID deviceUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private Button scanButton, connectButton, startServerButton, sendButton;
    private ListView deviceListView;
    private EditText messageInput;
    private TextView messageDisplay;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        scanButton = view.findViewById(R.id.scanButton);
        connectButton = view.findViewById(R.id.connectButton);
        startServerButton = view.findViewById(R.id.startServerButton);
        sendButton = view.findViewById(R.id.sendButton);
        deviceListView = view.findViewById(R.id.deviceListView);
        messageInput = view.findViewById(R.id.messageInput);
        messageDisplay = view.findViewById(R.id.messageDisplay); // Initialize TextView

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        deviceListAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1);
        deviceListView.setAdapter(deviceListAdapter);

        if (bluetoothAdapter == null) {
            Toast.makeText(getContext(), "Bluetooth not supported", Toast.LENGTH_LONG).show();
            requireActivity().finish();
        }

        scanButton.setOnClickListener(v -> listPairedDevices());
        connectButton.setOnClickListener(v -> connectToDevice());
        startServerButton.setOnClickListener(v -> startBluetoothServer());
        sendButton.setOnClickListener(v -> sendMessage());

        deviceListView.setOnItemClickListener((parent, view1, position, id) -> {
            String deviceInfo = (String) parent.getItemAtPosition(position);
            String deviceAddress = deviceInfo.split("\n")[1];
            selectedDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);
            Toast.makeText(getContext(), "Selected: " + selectedDevice.getName(), Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    private void updateMessageDisplay(String message) {
        requireActivity().runOnUiThread(() -> {
            String currentText = messageDisplay.getText().toString();
            messageDisplay.setText(currentText + "\n" + message); // Append new message
        });
    }


    @SuppressLint("MissingPermission")
    private void listPairedDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 100);
                return;
            }
        }

        if (bluetoothAdapter.isEnabled()) {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            deviceListAdapter.clear();
            if (!pairedDevices.isEmpty()) {
                for (BluetoothDevice device : pairedDevices) {
                    deviceListAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else {
                Toast.makeText(getContext(), "No paired devices found", Toast.LENGTH_SHORT).show();
            }
        } else {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
        }
    }

    private void listenForMessages() {
        new Thread(() -> {
            try {
                if (bluetoothSocket == null) {
                    updateMessageDisplay("Error: Bluetooth socket is null");
                    return;
                }

                InputStream inputStream = bluetoothSocket.getInputStream();
                byte[] buffer = new byte[1024];
                int bytes;

                while (true) {
                    bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        String receivedMessage = new String(buffer, 0, bytes);
                        updateMessageDisplay("Received: " + receivedMessage);
                    }
                }
            } catch (IOException e) {
                updateMessageDisplay("Connection lost");
            }
        }).start();
    }


    @SuppressLint("MissingPermission")
    private void connectToDevice() {
        if (selectedDevice == null) {
            Toast.makeText(getContext(), "No device selected!", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                bluetoothSocket = selectedDevice.createRfcommSocketToServiceRecord(deviceUUID);
                bluetoothSocket.connect();
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Connected to " + selectedDevice.getName(), Toast.LENGTH_SHORT).show()
                );

                listenForMessages(); // Start listening for messages

            } catch (IOException e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Connection failed", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }



    private void startBluetoothServer() {
        // Implement Bluetooth server logic here
        Toast.makeText(getContext(), "Starting server...", Toast.LENGTH_SHORT).show();
    }

    private void sendMessage() {
        String message = messageInput.getText().toString();
        if (!message.isEmpty() && bluetoothSocket != null) {
            try {
                bluetoothSocket.getOutputStream().write(message.getBytes());
                updateMessageDisplay("Sent: " + message);
            } catch (IOException e) {
                updateMessageDisplay("Error sending message");
            }
        }
    }
}
