package com.droiduino.droiduinorccontrol;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private Button btnPID,btnDTCNumber,btnGetDTC,testDB,btnClearDTC;

    private TextView text;
    private TextView Dtext;

    private String deviceName = null;
    private String deviceAddress;
    public static Handler handler;
    public static BluetoothSocket mmSocket;
    public static ConnectedThread connectedThread;
    public static CreateConnectThread createConnectThread;

    public static final String[] ReturnValue = {null};

    public FirebaseFirestore db;

    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI Initialization
        final Button buttonConnect = findViewById(R.id.buttonConnect);
        final Toolbar toolbar = findViewById(R.id.toolbar);

        final ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        text = findViewById(R.id.textView);
        btnPID = findViewById(R.id.buttonPID);
        btnDTCNumber = findViewById(R.id.buttonDtcNumber);
        btnGetDTC = findViewById(R.id.buttonGetDTC);
        testDB = findViewById(R.id.TestDatabase);
        Dtext = findViewById(R.id.DescriptionText);
        btnClearDTC = findViewById(R.id.buttonClearDTC);

        testDB.setEnabled(true);
        //Disable buttons until BT connection is established
        btnPID.setEnabled(false);
        btnDTCNumber.setEnabled(false);
        btnGetDTC.setEnabled(false);
        Dtext.setEnabled(false);
        btnClearDTC.setEnabled(false);


        // If a bluetooth device has been selected from SelectDeviceActivity
        deviceName = getIntent().getStringExtra("deviceName");
        if (deviceName != null){
            // Get the device address to make BT Connection
            deviceAddress = getIntent().getStringExtra("deviceAddress");
            // Show progress and connection status
            toolbar.setSubtitle("Connecting to " + deviceName + "...");
            progressBar.setVisibility(View.VISIBLE);
            buttonConnect.setEnabled(false);

            /*
            This is the most important piece of code. When "deviceName" is found
            the code will call a new thread to create a bluetooth connection to the
            selected device (see the thread code below)
             */
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            createConnectThread = new CreateConnectThread(bluetoothAdapter,deviceAddress);
            createConnectThread.start();
        }

        /*
        Second most important piece of Code
         */
        handler = new Handler(Looper.getMainLooper()) {
            @SuppressLint("ResourceAsColor")
            @Override
            public void handleMessage(@NonNull Message msg){
                switch (msg.what){
                    case CONNECTING_STATUS:
                        switch(msg.arg1){
                            case 1:
                                toolbar.setSubtitle("Connected to " + deviceName);
                                progressBar.setVisibility(View.GONE);
                                buttonConnect.setEnabled(true);
                                btnPID.setEnabled(true);
                                btnDTCNumber.setEnabled(true);
                                btnGetDTC.setEnabled(true);
                                btnClearDTC.setEnabled(true);
                                break;
                            case -1:
                                toolbar.setSubtitle("Device fails to connect");
                                progressBar.setVisibility(View.GONE);
                                buttonConnect.setEnabled(true);
                                break;
                        }
                        break;

                    case MESSAGE_READ:
                        //clearing the textview
                        text.setText("");

                        String DTCCodes = null;

                        // Read message from Arduino
                        String arduinoMsg = msg.obj.toString();
                        //Log.e("Arduino Message",arduinoMsg);
                        if(arduinoMsg.contains("Positive Response")) {
                            text.append("Positive Response\n");

                            //String that contains only DTC codes one after another
                            DTCCodes = arduinoMsg.substring("Positive Response".length());
                            //Log.d("DTCCODES", DTCCodes);

                            //Until theres enough characters to represent a DTC code process it
                            while (DTCCodes.length() > 5) {
                                String code = DTCCodes.substring(0, 5);
                                //Log.e("Error", code);

                                //Searching for the description of the DTC in the database
                                ReadData(code);

                                DTCCodes = DTCCodes.replace(code, "");
                                //Log.d("DTCCODES", DTCCodes);
                            }
                        }else
                            text.append("Negative Response");

                        break;
                }
            }
        };

        // Select Bluetooth Device
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SelectDeviceActivity.class);
                startActivity(intent);
            }
        });

        //Button for getting suppported PID's
        btnPID.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Sending the command
                String cmdText = "1";
                connectedThread.write(cmdText);
            }
        });

        //Button for getting the DTC's number
        btnDTCNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Sending the command
                String cmdText = "2";
                connectedThread.write(cmdText);
            }
        });

        //Button for getting the DTC codes
        btnGetDTC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Sending the command
                String cmdText = "3";
                connectedThread.write(cmdText);
            }
        });

        //Button for testing the DB
        testDB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ReadData("P0309");
            }
        });

        //Button for getting the DTC codes
        btnClearDTC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO implement warning message for clearing DTC's
                //Sending the command
                String cmdText = "4";
                connectedThread.write(cmdText);
            }
        });


    }
    /**
     * Function to read from the database
     * Firebase has been use as service
     */
    @SuppressLint("SetTextI18n")
    public void ReadData(final String DTCID){

        db= FirebaseFirestore.getInstance();
        //text.setText("The error description will appear here...");

        db.collection("CODES")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                //Get the data that corresponds to the DTCID from DB
                                if(document.getData().containsValue(DTCID)) {
                                    //Store it a one element array and print it in the configured textview
                                    ReturnValue[0] = (String) document.getData().get("DESCRIPTION");
                                    //log for debug
                                    Log.d("yes",ReturnValue[0]);
                                    if(text.getText().equals("The error description will appear here..."))
                                        text.setText(ReturnValue[0]);
                                    else{
                                        text.append("\n");
                                        text.append(ReturnValue[0]);
                                    }
                                }
                            }
                        } else {
                            Log.w(TAG, "Error getting Description.", task.getException());
                            text.setText("Error getting Description.");
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Error","Can't connect to collection");
                    }
                });
    }

    /* ============================ Thread to Create Connection =================================== */
    public static class CreateConnectThread extends Thread {

        public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);

                //TODO : Check why the methods below doesnt work, is it because I use Samsung ?
//                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
//                tmp = device.createRfcommSocketToServiceRecord(uuid);

            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.cancelDiscovery();
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                Log.e("Status", "Device connected");
                handler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    Log.e("Status", "Cannot connect to device");
                    handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connectedThread = new ConnectedThread(mmSocket);
            connectedThread.run();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    /* =============================== Thread for Data Transfer =========================================== */
    public static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;


        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException ignored) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes = 0; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    /*
                    Read from the InputStream from Arduino until termination character is reached.
                    Then send the whole String message to GUI Handler.
                     */
                    buffer[bytes] = (byte) mmInStream.read();
                    String readMessage;
                    if (buffer[bytes] == '\n'){
                        readMessage = new String(buffer,0,bytes);
                        Log.e("Arduino Message",readMessage);
                        handler.obtainMessage(MESSAGE_READ,readMessage).sendToTarget();
                        bytes = 0;
                    } else {
                        bytes++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes(); //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e("Send Error","Unable to send message",e);
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    /* ============================ Terminate Connection at BackPress ====================== */
    @Override
    public void onBackPressed() {
        // Terminate Bluetooth Connection and close app
        if (createConnectThread != null){
            createConnectThread.cancel();
        }
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }
}
