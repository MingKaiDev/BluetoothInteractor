package com.example.bluetoothchat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.example.bluetoothchat.R; // Ensure this import exists
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BluetoothApp";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 100;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice selectedDevice;
    private ArrayAdapter<String> deviceListAdapter;

    private static final String APP_NAME = "BluetoothChat";
    private static final UUID deviceUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private Button scanButton, connectButton, startServerButton, sendButton;
    private ListView deviceListView;
    private EditText messageInput;

    private AcceptThread acceptThread;
    private ConnectedThread connectedThread;

    private Handler handler = new Handler(Looper.getMainLooper());
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private MainPagerAdapter pagerAdapter;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        pagerAdapter = new MainPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Main");
                            break;
                        case 1:
                            tab.setText("Empty 1");
                            break;
                        case 2:
                            tab.setText("Empty 2");
                            break;
                    }
                }).attach();

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_main) {
                viewPager.setCurrentItem(0);
                return true;
            } else if (itemId == R.id.navigation_empty1) {
                viewPager.setCurrentItem(1);
                return true;
            } else if (itemId == R.id.navigation_empty2) {
                viewPager.setCurrentItem(2);
                return true;
            }
            return false;
        });

        scanButton = findViewById(R.id.scanButton);
        connectButton = findViewById(R.id.connectButton);
        startServerButton = findViewById(R.id.startServerButton);
        sendButton = findViewById(R.id.sendButton);
        deviceListView = findViewById(R.id.deviceListView);
        messageInput = findViewById(R.id.messageInput);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        deviceListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        deviceListView.setAdapter(deviceListAdapter);

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show();
            finish();
        }

        scanButton.setOnClickListener(v -> listPairedDevices());
        connectButton.setOnClickListener(v -> connectToDevice());
        startServerButton.setOnClickListener(v -> startBluetoothServer());
        sendButton.setOnClickListener(v -> sendMessage());

        deviceListView.setOnItemClickListener((parent, view, position, id) -> {
            String deviceInfo = (String) parent.getItemAtPosition(position);
            String deviceAddress = deviceInfo.split("\n")[1];
            selectedDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);
            Toast.makeText(this, "Selected: " + selectedDevice.getName(), Toast.LENGTH_SHORT).show();
        });
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice() {
        if (selectedDevice == null) {
            Toast.makeText(this, "No device selected!", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Connecting...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            try {
                bluetoothSocket = selectedDevice.createRfcommSocketToServiceRecord(deviceUUID);
                bluetoothSocket.connect();
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Connected to " + selectedDevice.getName(), Toast.LENGTH_SHORT).show();
                });

                connectedThread = new ConnectedThread(bluetoothSocket);
                connectedThread.start();

            } catch (IOException e) {
                Log.e(TAG, "Connection failed", e);
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void startBluetoothServer() {
        acceptThread = new AcceptThread();
        acceptThread.start();
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket serverSocket;

        public AcceptThread() {
            BluetoothServerSocket temp = null;
            try {
                temp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, deviceUUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket listen() failed", e);
            }
            serverSocket = temp;
        }

        public void run() {
            BluetoothSocket socket;
            while (true) {
                try {
                    Log.d(TAG, "Waiting for connection...");
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket accept() failed", e);
                    break;
                }

                if (socket != null) {
                    Log.d(TAG, "Client connected!");
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Client Connected!", Toast.LENGTH_SHORT).show());

                    connectedThread = new ConnectedThread(socket);
                    connectedThread.start();

                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Could not close the server socket", e);
                    }
                    break;
                }
            }
        }
    }
    @SuppressLint("MissingPermission")
    private void listPairedDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+ Permission Handling
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSIONS);
                return;
            }
        }

        if (bluetoothAdapter.isEnabled()) {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            deviceListAdapter.clear();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    deviceListAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else {
                Toast.makeText(this, "No paired devices found", Toast.LENGTH_SHORT).show();
            }
        } else {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private class ConnectedThread extends Thread {
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tempIn = null;
            OutputStream tempOut = null;

            try {
                tempIn = socket.getInputStream();
                tempOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error getting streams", e);
            }

            inputStream = tempIn;
            outputStream = tempOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                    String receivedMessage = new String(buffer, 0, bytes);

                    handler.post(() -> Toast.makeText(MainActivity.this, "Received: " + receivedMessage, Toast.LENGTH_SHORT).show());
                } catch (IOException e) {
                    Log.e(TAG, "Connection lost", e);
                    break;
                }
            }
        }

        public void write(String message) {
            try {
                outputStream.write(message.getBytes());
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Sent: " + message, Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                Log.e(TAG, "Error sending data", e);
            }
        }
    }

    private void sendMessage() {
        String message = messageInput.getText().toString();
        if (!message.isEmpty() && connectedThread != null) {
            connectedThread.write(message);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (bluetoothSocket != null) bluetoothSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing socket", e);
        }

        if (acceptThread != null) {
            acceptThread.interrupt();
        }

        if (connectedThread != null) {
            connectedThread.interrupt();
        }
    }
}
