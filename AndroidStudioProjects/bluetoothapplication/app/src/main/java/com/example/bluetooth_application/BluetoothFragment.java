package com.example.bluetooth_application;

import static android.content.ContentValues.TAG;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import dagger.hilt.android.AndroidEntryPoint;
import java.io.IOException;
import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.io.InputStream;
import java.io.OutputStream;


@AndroidEntryPoint
public class BluetoothFragment extends Fragment {
    private static final String TAG = "BluetoothFragment";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    private ActivityResultLauncher<String[]> multiplePermissionsLauncher;
    private ActivityResultLauncher<String> singlePermissionLauncher;
    private ActivityResultLauncher<Intent> enableBluetoothLauncher;
    // Bluetooth components
    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> deviceListAdapter;
    private final Map<String, BluetoothDevice> deviceMap = new HashMap<>();
    private ConnectedThread connectedThread;
    private BroadcastReceiver receiver;
    private String pendingOperation = null;
    // UI components
    private ListView pairedList;
    private ListView availableDevicesList;

    private MaterialButton enableBluetoothBtn;
    private MaterialButton connectDeviceBtn;
    private TextView statusText;
    private LottieAnimationView lottieButton;
    private static final int REQUEST_BLUETOOTH_CONNECT = 1001;
    // Define the receivers as class fields
    private BroadcastReceiver discoveryReceiver;
    private BroadcastReceiver pairingReceiver;
    private boolean permissionsRequested = false;
    private static final int REQUEST_BLUETOOTH_CONNECT_PAIRING = 1002; // Define at class level
    private BluetoothDevice pendingPairingDevice = null;
    private Runnable pendingOnPairedCallback = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Fragment created");

        // Initialize Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "onCreate: Bluetooth not supported on this device");
            showToast("Bluetooth not supported");
            requireActivity().onBackPressed();
            return;
        }
        Log.d(TAG, "onCreate: Bluetooth adapter initialized");
        setupPermissionLaunchers();
        // Initialize receivers
        createDiscoveryReceiver();
        createPairingReceiver();

        // Request permissions immediately when fragment is created
        requestInitialPermissions();
    }

    private void requestInitialPermissions() {
        if (permissionsRequested) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            String[] permissions = {
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };

            if (shouldShowRationaleForPermissions(permissions)) {
                showPermissionRationaleDialog(
                        "This app needs Bluetooth and Location permissions to discover and connect to devices",
                        () -> {
                            multiplePermissionsLauncher.launch(permissions);
                            permissionsRequested = true;
                        }
                );
            } else {
                multiplePermissionsLauncher.launch(permissions);
                permissionsRequested = true;
            }
        } else {
            String permission = Manifest.permission.ACCESS_FINE_LOCATION;

            if (shouldShowRequestPermissionRationale(permission)) {
                showPermissionRationaleDialog(
                        "This app needs Location permission to discover Bluetooth devices",
                        () -> {
                            singlePermissionLauncher.launch(permission);
                            permissionsRequested = true;
                        }
                );
            } else {
                singlePermissionLauncher.launch(permission);
                permissionsRequested = true;
            }
        }
    }

    private void retryPendingOperation() {
        if (pendingOperation == null) {
            Log.d(TAG, "retryPendingOperation: No pending operation");
            return;
        }

        Log.d(TAG, "retryPendingOperation: Executing pending operation: " + pendingOperation);
        String operation = pendingOperation;
        pendingOperation = null; // Clear it first to avoid loops

        switch (operation) {
            case "SCAN":
                Log.d(TAG, "retryPendingOperation: Retrying scan operation");
                handleScanClick();
                break;
            case "CONNECT":
                Log.d(TAG, "retryPendingOperation: Retrying connect operation");
                handleConnectClick();
                break;
            case "TOGGLE_BLUETOOTH":
                Log.d(TAG, "retryPendingOperation: Retrying bluetooth toggle");
                toggleBluetooth();
                break;
            default:
                Log.w(TAG, "retryPendingOperation: Unknown operation: " + operation);
        }
    }

    private void setupPermissionLaunchers() {
        Log.d(TAG, "setupPermissionLaunchers: Setting up permission launchers");

        multiplePermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                results -> {
                    boolean allGranted = true;
                    for (Boolean isGranted : results.values()) {
                        if (!isGranted) {
                            allGranted = false;
                            break;
                        }
                    }
                    Log.d(TAG, "MultiplePermissions result - allGranted: " + allGranted);

                    if (allGranted) {
                        showToast("Permissions granted");
                        if (pendingOperation != null) {
                            Log.d(TAG, "Retrying pending operation: " + pendingOperation);
                            retryPendingOperation();
                        }
                    } else {
                        Log.w(TAG, "Required permissions were denied");
                        showToast("Required permissions denied");
                        pendingOperation = null;
                    }
                }
        );

        singlePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    Log.d(TAG, "SinglePermission result - isGranted: " + isGranted);
                    if (isGranted) {
                        showToast("Permission granted");
                        if (pendingOperation != null) {
                            Log.d(TAG, "Retrying pending operation: " + pendingOperation);
                            retryPendingOperation();
                        }
                    } else {
                        Log.w(TAG, "Required permission was denied");
                        showToast("Required permission denied");
                        pendingOperation = null;
                    }
                }
        );

        enableBluetoothLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "Bluetooth enable result: " + result.getResultCode());
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Log.d(TAG, "Bluetooth enabled successfully");
                        showToast("Bluetooth enabled");
                        updateButtonStates();
                        if (pendingOperation != null) {
                            Log.d(TAG, "Retrying pending operation: " + pendingOperation);
                            retryPendingOperation();
                        }
                    } else {
                        Log.w(TAG, "Bluetooth enable request denied");
                        showToast("Bluetooth enable request denied");
                        updateButtonStates();
                    }
                }
        );
    }

    private boolean shouldShowRationaleForPermissions(String[] permissions) {
        Log.d(TAG, "shouldShowRationaleForPermissions: Checking for " + permissions.length + " permissions");

        for (String permission : permissions) {
            boolean shouldShow = shouldShowRequestPermissionRationale(permission);
            Log.d(TAG, "shouldShowRationaleForPermissions: " + permission + " = " + shouldShow);

            if (shouldShow) {
                Log.d(TAG, "shouldShowRationaleForPermissions: Rationale needed for " + permission);
                return true;
            }
        }

        Log.d(TAG, "shouldShowRationaleForPermissions: No rationale needed for any permissions");
        return false;
    }

    private void showPermissionRationaleDialog(String message, Runnable onAccept) {
        Log.d(TAG, "showPermissionRationaleDialog: Displaying dialog with message: " + message);

        new AlertDialog.Builder(requireContext())
                .setTitle("Permission Required")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    Log.d(TAG, "showPermissionRationaleDialog: User accepted rationale");
                    onAccept.run();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Log.d(TAG, "showPermissionRationaleDialog: User denied rationale");
                    pendingOperation = null;
                })
                .setOnCancelListener(dialog -> {
                    Log.d(TAG, "showPermissionRationaleDialog: Dialog cancelled");
                    pendingOperation = null;
                })
                .show();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bluetooth, container, false);
        initializeViews(view);
        setupAdapter();
        setupListeners();
        return view;
    }

    private void initializeViews(View view) {
        pairedList = view.findViewById(R.id.paired_devices_list);
        availableDevicesList = view.findViewById(R.id.available_devices_list);
        enableBluetoothBtn = view.findViewById(R.id.enableBluetooth);
        connectDeviceBtn = view.findViewById(R.id.connectDevice);
        statusText = view.findViewById(R.id.sample_text);
        lottieButton = view.findViewById(R.id.lottieButton);
    }

    private ArrayAdapter<String> pairedDeviceListAdapter;

    private void setupAdapter() {
        Log.d(TAG, "setupAdapter: Initializing device list adapters");

        // Adapter for available devices
        deviceListAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.list_item, R.id.tv_list_item, new ArrayList<>());
        availableDevicesList.setAdapter(deviceListAdapter);
        availableDevicesList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        availableDevicesList.setOnItemClickListener((parent, view, position, id) -> {
            Log.d(TAG, "onItemClick: Available device selected: " + deviceListAdapter.getItem(position));
            availableDevicesList.setItemChecked(position, true);
            updateButtonStates();
        });

        // Adapter for paired devices
        pairedDeviceListAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.list_item, R.id.tv_list_item, new ArrayList<>());
        pairedList.setAdapter(pairedDeviceListAdapter);
        pairedList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        pairedList.setOnItemClickListener((parent, view, position, id) -> {
            Log.d(TAG, "onItemClick: Paired device selected: " + pairedDeviceListAdapter.getItem(position));
            pairedList.setItemChecked(position, true);
            updateButtonStates();
        });

        // Populate paired devices
        populatePairedDevices();
    }

    private void populatePairedDevices() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_CONNECT);
            return;
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        pairedDeviceListAdapter.clear();
        if (pairedDevices != null && !pairedDevices.isEmpty()) {
            for (BluetoothDevice device : pairedDevices) {
                String deviceInfo = device.getName() + " - " + device.getAddress();
                pairedDeviceListAdapter.add(deviceInfo);
            }
        } else {
            pairedDeviceListAdapter.add("No paired devices");
        }
        pairedDeviceListAdapter.notifyDataSetChanged();
    }

    private void setupListeners() {
        Log.d(TAG, "setupListeners: Setting up button click listeners");
        enableBluetoothBtn.setOnClickListener(v -> {
            Log.d(TAG, "enableBluetoothBtn: Clicked");
            toggleBluetooth();
        });
        connectDeviceBtn.setOnClickListener(v -> {
            Log.d(TAG, "connectDeviceBtn: Clicked");
            handleConnectClick();
        });

        lottieButton.setOnClickListener(v -> {
            Log.d(TAG, "scanDevicesBtn: Clicked");
            handleScanClick();
        });
    }
    private void toggleBluetooth() {
        Log.d(TAG, "toggleBluetooth: Attempting to toggle Bluetooth. Current state: " +
                (bluetoothAdapter != null ? bluetoothAdapter.isEnabled() : "Adapter NULL"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            Log.w(TAG, "toggleBluetooth: BLUETOOTH_CONNECT permission missing");
            requestBluetoothPermissions("TOGGLE_BLUETOOTH");
            return;
        }

        try {
            if (bluetoothAdapter.isEnabled()) {
                Log.d(TAG, "toggleBluetooth: Disabling Bluetooth");
                if (ActivityCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "toggleBluetooth: BLUETOOTH_CONNECT permission check failed");
                    return;
                }
                boolean disableResult = bluetoothAdapter.disable();
                Log.d(TAG, "toggleBluetooth: disable() returned " + disableResult);
                showToast("Bluetooth disabled");
                updateButtonStates();
            } else {
                Log.d(TAG, "toggleBluetooth: Requesting to enable Bluetooth");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                Log.d(TAG, "toggleBluetooth: Launching enable intent");
                enableBluetoothLauncher.launch(enableBtIntent);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "toggleBluetooth: SecurityException", e);
        }
    }

    /**
     * Bluetooth state is clearly and properly checked here before scanning starts such as:
     * 1) If the Bluetooth adapter is null or disabled
     * 2) If a scan is already in progress
     * 3) If required permissions are granted
     *
     * Also starts or stops discovery depending on state
     */
    private void handleScanClick() {
        Log.d(TAG, "handleScanClick: Scan button clicked. BT State: " +
                (bluetoothAdapter != null ? bluetoothAdapter.isEnabled() : "Adapter NULL"));

        if (!bluetoothAdapter.isEnabled()) {
            lottieButton.pauseAnimation(); // Ensure animation is stopped
            showToast("Please enable Bluetooth first");
            pendingOperation = "SCAN";
            toggleBluetooth();
            return;
        }

        if (isScanning()) {
            Log.d(TAG, "handleScanClick: Discovery already in progress. Stopping...");
            lottieButton.pauseAnimation(); // Stop animation when stopping scan
            stopDeviceDiscovery();
            return;
        }

        if (!checkScanPermissions()) {
            lottieButton.pauseAnimation(); // Ensure animation is stopped
            pendingOperation = "SCAN";
            requestScanPermissions();
            return;
        }

        Log.d(TAG, "handleScanClick: All checks passed. Starting discovery...");
        lottieButton.playAnimation(); // Start animation when scanning starts
        startDeviceDiscovery();
    }

    private boolean checkScanPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean hasScan = hasPermission(Manifest.permission.BLUETOOTH_SCAN);
            boolean hasConnect = hasPermission(Manifest.permission.BLUETOOTH_CONNECT);
            Log.d(TAG, "checkScanPermissions: BLUETOOTH_SCAN=" + hasScan +
                    ", BLUETOOTH_CONNECT=" + hasConnect);
            return hasScan && hasConnect;
        } else {
            boolean hasLocation = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            Log.d(TAG, "checkScanPermissions: ACCESS_FINE_LOCATION=" + hasLocation);
            return hasLocation;
        }
    }

    private void requestScanPermissions() {
        Log.d(TAG, "requestScanPermissions: SDK=" + Build.VERSION.SDK_INT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            String[] permissions = {
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            };
            Log.d(TAG, "requestScanPermissions: Requesting Android 12+ permissions");

            if (shouldShowRationaleForPermissions(permissions)) {
                Log.d(TAG, "requestScanPermissions: Showing rationale dialog");
                showPermissionRationaleDialog(
                        "Bluetooth permissions needed",
                        () -> {
                            Log.d(TAG, "User accepted rationale. Launching permission request");
                            multiplePermissionsLauncher.launch(permissions);
                        }
                );
            } else {
                Log.d(TAG, "requestScanPermissions: Launching permission request without rationale");
                multiplePermissionsLauncher.launch(permissions);
            }
        } else {
            String permission = Manifest.permission.ACCESS_FINE_LOCATION;
            Log.d(TAG, "requestScanPermissions: Requesting location permission for Android <12");

            if (shouldShowRequestPermissionRationale(permission)) {
                Log.d(TAG, "requestScanPermissions: Showing location rationale");
                showPermissionRationaleDialog(
                        "Location permission needed",
                        () -> {
                            Log.d(TAG, "User accepted location rationale. Launching request");
                            singlePermissionLauncher.launch(permission);
                        }
                );
            } else {
                Log.d(TAG, "requestScanPermissions: Requesting location permission directly");
                singlePermissionLauncher.launch(permission);
            }
        }
    }

    private void disconnectDevice() {
        if (connectedThread != null) {
            // Cancel the connection thread
            connectedThread.cancel();
            connectedThread = null;

            // Update UI
            showToast("Disconnected");
            statusText.setText("Status: Disconnected");
        }
        updateButtonStates();
    }

    private void requestBluetoothPermissions(String operation) {
        Log.d(TAG, "requestBluetoothPermissions: Operation=" + operation);
        pendingOperation = operation;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            String[] permissions = {
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            };
            Log.d(TAG, "requestBluetoothPermissions: Android 12+ - requesting: " + Arrays.toString(permissions));

            if (shouldShowRationaleForPermissions(permissions)) {
                Log.d(TAG, "requestBluetoothPermissions: Showing rationale dialog");
                showPermissionRationaleDialog(
                        "Bluetooth permissions are required for this operation.",
                        () -> {
                            Log.d(TAG, "requestBluetoothPermissions: User accepted rationale - launching permission request");
                            multiplePermissionsLauncher.launch(permissions);
                        }
                );
            } else {
                Log.d(TAG, "requestBluetoothPermissions: Launching permission request without rationale");
                multiplePermissionsLauncher.launch(permissions);
            }
        } else {
            Log.w(TAG, "requestBluetoothPermissions: Unexpected call for pre-Android 12 device");
        }
    }

    // Would check if scanning is already in progress (a state check)
    private boolean isScanning() {
        try {
            if (bluetoothAdapter == null) {
                Log.d(TAG, "isScanning: BluetoothAdapter is null");
                return false;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                boolean hasPermission = hasPermission(Manifest.permission.BLUETOOTH_SCAN);
                Log.d(TAG, "isScanning: BLUETOOTH_SCAN permission: " + hasPermission);
                if (!hasPermission) {
                    return false;
                }
            }

            boolean isDiscovering = bluetoothAdapter.isDiscovering();
            Log.d(TAG, "isScanning: isDiscovering: " + isDiscovering);
            return isDiscovering;
        } catch (SecurityException e) {
            Log.e(TAG, "isScanning: SecurityException", e);
            return false;
        }
    }
    // Create the discovery receiver (called once in onCreate)
    private void createDiscoveryReceiver() {
        discoveryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    try {
                        if (device != null && device.getName() != null) {
                            String address = device.getAddress();
                            if (!deviceMap.containsKey(address)) {
                                deviceMap.put(address, device);
                                deviceListAdapter.add(device.getName() + " - " + address);
                            }
                        }
                    } catch (SecurityException e) {
                        Log.e(TAG, "SecurityException handling found device", e);
                    }
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    updateButtonStates();
                }
            }
        };
    }

    // Initiates the scanning process (an action)
    private void startDeviceDiscovery() {
        Log.d(TAG, "startDeviceDiscovery: Starting...");

        if (bluetoothAdapter == null) {
            Log.e(TAG, "startDeviceDiscovery: BluetoothAdapter is null");
            return;
        }

        // Clear existing devices
        Log.d(TAG, "startDeviceDiscovery: Clearing " + deviceMap.size() + " devices from map");
        deviceMap.clear();
        Log.d(TAG, "startDeviceDiscovery: Clearing " + deviceListAdapter.getCount() + " items from adapter");
        deviceListAdapter.clear();

        // Stop any existing discovery
        Log.d(TAG, "startDeviceDiscovery: Stopping any existing discovery");
        stopDeviceDiscovery();

        // Register for device discovery events
        Log.d(TAG, "startDeviceDiscovery: Registering receiver");
        registerReceiver();

        // Start discovery
        try {
            Log.d(TAG, "startDeviceDiscovery: Attempting to start discovery");
            boolean discoveryStarted = bluetoothAdapter.startDiscovery();
            Log.d(TAG, "startDeviceDiscovery: startDiscovery() returned " + discoveryStarted);

            if (discoveryStarted) {
                showToast("Scanning for devices...");
            } else {
                Log.e(TAG, "startDeviceDiscovery: Failed to start discovery");
                showToast("Scan failed to start");
            }
            updateButtonStates();
        } catch (SecurityException e) {
            Log.e(TAG, "startDeviceDiscovery: SecurityException", e);
            showToast("Permission error during scanning");
        }
    }

    private void registerDiscoveryReceiver() {
        if (discoveryReceiver == null) {
            createDiscoveryReceiver();
        }

        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            requireContext().registerReceiver(discoveryReceiver, filter);
        } catch (Exception e) {
            Log.e(TAG, "Error registering discovery receiver", e);
        }
    }

    private void stopDeviceDiscovery() {
        try {
            if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException stopping discovery", e);
        }
        updateButtonStates();
    }

    private void unregisterDiscoveryReceiver() {
        if (discoveryReceiver != null) {
            try {
                requireContext().unregisterReceiver(discoveryReceiver);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Discovery receiver not registered", e);
            }
        }
    }

    // Create the pairing receiver (called once in onCreate)
    private void createPairingReceiver() {
        pairingReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                    BluetoothDevice receivedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // This receiver will be used with specific devices, so we'll handle the device check when registering
                    int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    int previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                    if (bondState == BluetoothDevice.BOND_BONDED) {
                        // Successfully paired
                        showToast("Sucessfully Paired ");
                        populatePairedDevices();
                        updateAvailableDeviceList(receivedDevice);
                        //unregisterPairingReceiver();

                        // The callback will be provided when registering for a specific device
                    } else if (bondState == BluetoothDevice.BOND_NONE && previousBondState == BluetoothDevice.BOND_BONDING) {
                        // Pairing failed
                        unregisterPairingReceiver();
                        showToast("Pairing failed");
                    }
                }
            }
        };
    }

    private void updateAvailableDeviceList(BluetoothDevice bondedDevice) {
        if (deviceListAdapter != null) {
            // Remove bonded device from available devices list
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                return;
            }
            String deviceInfo = bondedDevice.getName() + " - " + bondedDevice.getAddress();
            deviceListAdapter.remove(deviceInfo);
            deviceListAdapter.notifyDataSetChanged();
        }
    }


    private void registerPairingReceiverForDevice(BluetoothDevice device, Runnable onPaired) {
        if (pairingReceiver == null) {
            Log.d(TAG, "PairingReceiver is null. Creating a new one.");
            createPairingReceiver();
        } else {
            Log.d(TAG, "PairingReceiver already exists.");
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // Store pending device and callback to retry after permission is granted
            pendingPairingDevice = device;
            pendingOnPairedCallback = onPaired;

            Log.w(TAG, "BLUETOOTH_CONNECT permission not granted. Requesting permission...");
            requestPermissions(
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    REQUEST_BLUETOOTH_CONNECT_PAIRING
            );
            return;
        }

        try {
            Log.d(TAG, "Registering pairing BroadcastReceiver for device: " + device.getName() + " - " + device.getAddress());

            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            requireActivity().registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    BluetoothDevice receivedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    Log.d(TAG, "BroadcastReceiver received intent: " + action);
                    if (receivedDevice != null) {
                        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        Log.d(TAG, "Received device: " + receivedDevice.getName() + " - " + receivedDevice.getAddress());

                        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            Log.w(TAG, "BLUETOOTH_CONNECT permission still not granted inside receiver.");
                            return;
                        }

                        if (receivedDevice.getAddress().equals(device.getAddress())) {
                            Log.d(TAG, "Device address matches the target device.");
                            pairingReceiver.onReceive(context, intent);

                            int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                            Log.d(TAG, "Current bond state: " + bondState);

                            if (bondState == BluetoothDevice.BOND_BONDED) {
                                Log.d(TAG, "Device successfully paired. Running onPaired callback.");
                                onPaired.run();
                            }
                        } else {
                            Log.d(TAG, "Received device does not match the target device.");
                        }
                    }
                }
            }, filter);

            Log.d(TAG, "Receiver registered successfully.");

        } catch (Exception e) {
            Log.e(TAG, "Error registering pairing receiver", e);
        }
    }

    private void unregisterPairingReceiver() {
        if (pairingReceiver != null) {
            try {
                requireActivity().unregisterReceiver(pairingReceiver);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Pairing receiver not registered", e);
            }
        }
    }

    // Modified ensureDevicePaired method to use the new receiver
    private void ensureDevicePaired(BluetoothDevice device, Runnable onPaired) {
        Log.d(TAG, "entering EnsureDevicePaired function");
        try {
            Log.d(TAG, "Checking Bluetooth CONNECT permission...");
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        REQUEST_BLUETOOTH_CONNECT
                );
                return; // Wait for the user response
            }

            Log.d(TAG, "Permission granted. Checking if device is already bonded...");
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                // Already paired, proceed with connection
                onPaired.run();
                return;
            }

            Log.d(TAG, "Device not bonded. Registering receiver for pairing...");
            // Register receiver for this specific device
            registerPairingReceiverForDevice(device, onPaired);

            Log.d(TAG, "Attempting to start bonding (pairing)...");
            // Start pairing process
            if (ActivityCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                boolean pairingStarted = device.createBond();
                if (!pairingStarted) {
                    unregisterPairingReceiver();
                    Log.d(TAG, "Pairing failed");
                    showToast("Failed to start pairing");
                } else {
                    Log.d(TAG, "Pairing started successfully.");
                    showToast("Pairing with device...");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Pairing error", e);
            showToast("Pairing error");
        }
    }

    private void registerReceiver() {
        registerDiscoveryReceiver();
    };

    private void unregisterReceiver() {
        unregisterDiscoveryReceiver();
    }

    private void handleConnectClick() {
        Log.d(TAG, "handleConnectClick: Connect button clicked");

        // 1. Check Bluetooth state
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.w(TAG, "handleConnectClick: Bluetooth disabled - cannot connect");
            showToast("Please enable Bluetooth first");
            pendingOperation = "CONNECT";
            toggleBluetooth();
            return;
        }

        // 2. Check if already connected
        if (connectedThread != null) {
            disconnectDevice();
            return;
        }

        // 3. Verify device selection
        int position = pairedList.getCheckedItemPosition();
        if (position == ListView.INVALID_POSITION) {
            showToast("Select a device first");
            return;
        }

        // 4. Handle permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            pendingOperation = "CONNECT";
            requestBluetoothPermissions("CONNECT");
            return;
        }

        // 5. Get selected device
        String selectedItem = deviceListAdapter.getItem(position);
        if (selectedItem != null) {
            String address = selectedItem.substring(selectedItem.lastIndexOf(" - ") + 3);
            Log.d(TAG, "String Address: " + address);
            BluetoothDevice device = deviceMap.get(address);

            if (device != null) {
                // New: Ensure device is paired before connecting
                ensureDevicePaired(device, () -> {
                    // This runs after pairing is complete or if already paired
                    connectToDevice(device);
                });
            }
        }
    }

    private void connectToDevice(BluetoothDevice device) {
        stopDeviceDiscovery();

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        new Thread(() -> {
            BluetoothSocket socket = null;
            boolean connectionSuccess = false;

            try {
                // Try standard SPP UUID first
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // Store the device to retry after permission is granted
                    //pendingDeviceToConnect = device;

                    // Request the permission
                    requestPermissions(
                            new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                            REQUEST_BLUETOOTH_CONNECT
                    );
                    return;
                }
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                bluetoothAdapter.cancelDiscovery();

                // Set connection timeout
                try {
                    socket.connect();
                    connectionSuccess = true;
                } catch (IOException e) {
                    Log.e(TAG, "Standard connection failed, trying fallback", e);
                    try {
                        socket.close();
                    } catch (IOException closeException) {
                        Log.e(TAG, "Error closing socket", closeException);
                    }

                    // Try fallback method
                    try {
                        Method m = device.getClass().getMethod("createInsecureRfcommSocket", int.class);
                        socket = (BluetoothSocket) m.invoke(device, 1);
                        socket.connect();
                        connectionSuccess = true;
                    } catch (Exception fallbackException) {
                        Log.e(TAG, "Fallback connection failed", fallbackException);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Connection error", e);
            }

            // Handle connection result
            final boolean finalSuccess = connectionSuccess;
            final BluetoothSocket finalSocket = socket;
            requireActivity().runOnUiThread(() -> {
                if (finalSuccess && finalSocket != null) {
                    connectedThread = new ConnectedThread(finalSocket);
                    connectedThread.start();
                    showToast("Connected to " + device.getName());
                } else {
                    showToast("Connection failed");
                    if (finalSocket != null) {
                        try {
                            finalSocket.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Error closing socket", e);
                        }
                    }
                }
                updateButtonStates();
            });
        }).start();
    }

    private void updateButtonStates() {
        if (!isAdded()) {
            Log.w(TAG, "updateButtonStates: Fragment not attached to activity");
            return;
        }

        // Get current states
        boolean isEnabled = false;
        boolean isConnected = connectedThread != null;
        boolean deviceSelected = pairedList.getCheckedItemPosition() != ListView.INVALID_POSITION;

        try {
            isEnabled = bluetoothAdapter != null && bluetoothAdapter.isEnabled();
            boolean isScanning = isScanning();

            // Control Lottie animation
            if (isScanning) {
                lottieButton.playAnimation();
            } else {
                lottieButton.pauseAnimation();
            }

            Log.d(TAG, String.format("State - Enabled: %b, Scanning: %b, Connected: %b, DeviceSelected: %b",
                    isEnabled, isScanning, isConnected, deviceSelected));
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException in updateButtonStates", e);
        }

        // Update Bluetooth toggle button
        enableBluetoothBtn.setIcon(ContextCompat.getDrawable(requireContext(),
                isEnabled ? R.drawable.ic_bluetooth : R.drawable.ic_bluetooth_disabled));

        // Update connect/disconnect button
        connectDeviceBtn.setEnabled(isEnabled && (deviceSelected || isConnected));
        connectDeviceBtn.setText(isConnected ? "Disconnect" : "Connect");

        // Update status text
        statusText.setText(isConnected ? "Status: Connected" :
                isEnabled ? "Status: Ready" : "Status: Bluetooth Off");

        Log.d(TAG, String.format("Button states - Connect enabled: %b", connectDeviceBtn.isEnabled()));
    }

    @Override
    public void onStart() {
        super.onStart();
        updateButtonStates();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_BLUETOOTH_CONNECT_PAIRING) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "BLUETOOTH_CONNECT permission granted (for pairing).");

                if (pendingPairingDevice != null && pendingOnPairedCallback != null) {
                    registerPairingReceiverForDevice(pendingPairingDevice, pendingOnPairedCallback);
                    pendingPairingDevice = null;
                    pendingOnPairedCallback = null;
                }
            } else {
                Log.w(TAG, "BLUETOOTH_CONNECT permission denied by user (for pairing).");
                showToast("Bluetooth permission is required to pair with devices.");
            }
        }

        boolean allGranted = true;
        if (grantResults.length > 0) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
        } else {
            allGranted = false;
        }


        if (allGranted && pendingOperation != null) {
            retryPendingOperation();
        } else {
            showToast("Permissions denied");
            pendingOperation = null;
        }
    }
    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(requireContext(), permission) ==
                PackageManager.PERMISSION_GRANTED;
    }
    private void showToast(String message) {
        if (isAdded()) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private boolean running = true;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread: Creating new connection thread");
            this.socket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                Log.d(TAG, "ConnectedThread: Getting socket streams");
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
                Log.d(TAG, "ConnectedThread: Successfully got streams");
            } catch (IOException e) {
                Log.e(TAG, "ConnectedThread: Error getting streams", e);
            }

            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        public void run() {
            Log.d(TAG, "ConnectedThread: Thread started");
            byte[] buffer = new byte[1024];

            while (running) {
                try {
                    Log.d(TAG, "ConnectedThread: Waiting for incoming data");
                    int bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        String received = new String(buffer, 0, bytes);
                        Log.d(TAG, "ConnectedThread: Received " + bytes + " bytes: " + received);

                        // Update UI with received data
                        final String finalReceived = received;
                        if (isAdded() && getActivity() != null) {
                            requireActivity().runOnUiThread(() -> {
                                statusText.setText("Received: " + finalReceived);
                            });
                        }
                    }
                } catch (IOException e) {
                    if (running) {
                        Log.e(TAG, "ConnectedThread: Connection lost", e);
                    } else {
                        Log.d(TAG, "ConnectedThread: Socket closed");
                    }
                    break;
                }
            }

            Log.d(TAG, "ConnectedThread: Thread ending");
            if (isAdded() && getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    showToast("Disconnected");
                    connectedThread = null;
                    statusText.setText("Status: Disconnected");
                    updateButtonStates();
                });
            }
        }
        public void write(byte[] bytes) {
            try {
                Log.d(TAG, "ConnectedThread write: Sending " + bytes.length + " bytes");
                outputStream.write(bytes);
                Log.d(TAG, "ConnectedThread write: Data sent successfully");
            } catch (IOException e) {
                Log.e(TAG, "ConnectedThread write: Error sending data", e);
            }
        }
        public void cancel() {
            Log.d(TAG, "ConnectedThread cancel: Closing connection");
            running = false;
            try {
                socket.close();
                Log.d(TAG, "ConnectedThread cancel: Socket closed successfully");
            } catch (IOException e) {
                Log.e(TAG, "ConnectedThread cancel: Error closing socket", e);
            }
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Fragment resumed");
        // Don't unregister receiver here as in the original code
        registerReceiver(); // Register receiver when coming back to the fragment
        updateButtonStates();
    }
    @Override
    public void onPause() {
        Log.d(TAG, "onPause: Fragment paused");
        super.onPause();
        stopDeviceDiscovery();
    }
    @Override
    public void onStop() {
        Log.d(TAG, "onStop: Fragment stopped");
        super.onStop();
        unregisterReceiver();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnectDevice();
        unregisterPairingReceiver();
        Log.d(TAG, "onDestroy: unregisterPairingReceiver() called");
        unregisterDiscoveryReceiver();
        Log.d(TAG, "onDestroy: unregisterDiscoveryReceiver() called");
    }
}