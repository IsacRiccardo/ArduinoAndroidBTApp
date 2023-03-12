package com.droiduino.droiduinorccontrol;

import static android.content.ContentValues.TAG;

import static java.lang.Math.round;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private Button btnPID;
    private Button btnDTCNumber;
    private Button btnGetDTC;
    private Button btnClearDTC;
    private ImageButton SendToDB;

    private Button DetectProtocol;
    private Button BvButton;

    private TextView text, Dtext;

    private String deviceName = null;
    public static Handler handler;
    public static BluetoothSocket mmSocket;
    public static ConnectedThread connectedThread;
    public static CreateConnectThread createConnectThread;

    public int[] SupportedPIDs = new int[33];

    //Variable that will determine how the response will be processed
    public static int CMD;

    public static boolean PID = false;

    public static final String[] ReturnValue = {null};

    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update

    @SuppressLint({"ClickableViewAccessibility", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI Initialization
        final Button buttonConnect = findViewById(R.id.buttonConnect);
        final Toolbar toolbar = findViewById(R.id.toolbar);

        final ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        //text
        text = findViewById(R.id.textView);
        Dtext = findViewById(R.id.DescriptionText);
        //buttons
        btnPID = findViewById(R.id.buttonPID);
        btnDTCNumber = findViewById(R.id.buttonDtcNumber);
        btnGetDTC = findViewById(R.id.buttonGetDTC);
        SendToDB = findViewById(R.id.imageButton);
        BvButton = findViewById(R.id.buttonBV);
        btnClearDTC = findViewById(R.id.buttonClearDTC);
        DetectProtocol = findViewById(R.id.buttonDP);

        //Disable buttons until BT connection is established
        btnPID.setEnabled(false);
        btnDTCNumber.setEnabled(false);
        btnGetDTC.setEnabled(false);
        Dtext.setEnabled(false);
        btnClearDTC.setEnabled(false);
        SendToDB.setEnabled(false);
        BvButton.setEnabled(false);
        DetectProtocol.setEnabled(false);


        // If a bluetooth device has been selected from SelectDeviceActivity
        deviceName = getIntent().getStringExtra("deviceName");
        if (deviceName != null){
            // Get the device address to make BT Connection
            String deviceAddress = getIntent().getStringExtra("deviceAddress");
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
            createConnectThread = new CreateConnectThread(bluetoothAdapter, deviceAddress);
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
                        switch(msg.arg1) {
                            case 1:
                                toolbar.setSubtitle("Connected to " + deviceName);
                                progressBar.setVisibility(View.GONE);
                                buttonConnect.setEnabled(true);
                                btnPID.setEnabled(true);
                                btnDTCNumber.setEnabled(true);
                                btnGetDTC.setEnabled(true);
                                btnClearDTC.setEnabled(true);
                                SendToDB.setEnabled(true);
                                BvButton.setEnabled(true);
                                DetectProtocol.setEnabled(true);
                                text.setEnabled(true);
                                text.setMovementMethod(new ScrollingMovementMethod());
                                break;
                            case -1:
                                toolbar.setSubtitle("Device fails to connect");
                                progressBar.setVisibility(View.GONE);
                                buttonConnect.setEnabled(true);
                                break;
                        }
                        break;

                    case MESSAGE_READ:
                        //clearing the textview if PID is false
                        if(!PID)
                            text.setText("");
                        else
                            text.append("\n");

                        // Read message from Arduino
                        String arduinoMsg = msg.obj.toString();

                        if(arduinoMsg.contains("Positive Response"))
                        {
                            String PS = "<font color=#04ff00>Positive Response</font>";

                            text.append(Html.fromHtml(PS));
                            text.append("\n");

                            switch(CMD)
                            {
                                case 33:
                                    //Supported PID response process
                                    //Extract the data from the response
                                    String RespDataPID = arduinoMsg.substring("Positive Response".length());

                                    //Initialize array
                                    Arrays.fill(SupportedPIDs, 0);

                                    //Split the response based on character "," to process every PID
                                    String [] tokens = RespDataPID.split(",");
                                    Log.d("PID RESPONSE",RespDataPID);

                                    if(text.length()==0)
                                    {
                                        text.setText(">>Service 1 Supported PID's [01-20]:\n");
                                    }else
                                        text.append(">>Service 1 Supported PID's [01-20]:\n");

                                    int i = 0;
                                    text.append(">");

                                    //iterate through the array of tokens and print the supported PID in HEX format
                                    while(i<tokens.length - 1){
                                        //Log.d("INT",Integer.valueOf(tokens[i]).toString());

                                        //insert into string the supported PID's
                                        SupportedPIDs[Integer.parseInt(tokens[i])] = 1;

                                        //convert the value to hex
                                        String HexValue = Integer.toHexString(Integer.parseInt(tokens[i]));
                                        //Log.d("HEX",HexValue);
                                        text.append(HexValue + " ");
                                        i++;
                                    }
                                    Log.e(TAG, Arrays.toString(SupportedPIDs));
                                    //Reset command variable
                                    CMD = 0;
                                    break;

                                case 34:
                                    //DTC number response process
                                    //Extract the data from the response
                                    String RespData = arduinoMsg.substring("Positive Response".length());

                                    if(RespData.startsWith("1"))
                                        text.append(">>Engine check light is ON\n");
                                    else
                                        text.append(">>Engine check light is OFF\n");

                                    text.append(">Number of DTC's: ");
                                    //Extract number of DTC from response
                                    String DTCNB = RespData.substring(1);
                                    text.append(DTCNB);

                                    //Reset command variable
                                    CMD = 0;
                                    break;

                                case 35:
                                    //Read DTC response process
                                    String DTCCodes = null;

                                        //String that contains only DTC codes one after another
                                        DTCCodes = arduinoMsg.substring("Positive Response".length());
                                        //Log.d("DTCCODES", DTCCodes);

                                        //Until theres enough characters to represent a DTC code process it
                                        while (DTCCodes.length() > 5) {
                                            String code = DTCCodes.substring(0, 5);
                                            Log.d("DTC", code);

                                            //Searching for the description of the DTC in the database
                                            ReadData(code);

                                            DTCCodes = DTCCodes.replace(code, "");
                                            Log.d("DTCCODES", DTCCodes);
                                        }
                                    //Reset command variable
                                    CMD = 0;
                                    break;

                                case 36:
                                    //Clear DTC response process
                                    String ClearDTCMSG = null;
                                    ClearDTCMSG = arduinoMsg.substring("Positive Response".length());

                                    text.append(ClearDTCMSG + "\n");
                                    Log.d("Clear DTC MSG", ClearDTCMSG);

                                    //Reset command variable
                                    CMD = 0;
                                    break;

                                case 37:
                                    String BatteryVDesc = arduinoMsg.substring("Positive Response".length());

                                    if(BatteryVDesc.startsWith("V"))
                                    {
                                        String Voltage = BatteryVDesc.substring(1);
                                        Log.d("Battery Voltage", Voltage);
                                        text.append("Battery Voltage: " + Voltage + "\n");
                                    }

                                    //Reset command variable
                                    CMD = 0;
                                    break;

                                case 38:
                                    String ProtocolDesc = arduinoMsg.substring("Positive Response".length());

                                    if(ProtocolDesc.startsWith("P"))
                                    {
                                        String Protocol = ProtocolDesc.substring(1);

                                        Log.d("PROTOCOL", Protocol);
                                        text.append("Protocol: " + Protocol + "\n");
                                    }

                                    //Reset command variable
                                    CMD = 0;
                                    break;

                                // The default case will treat the response for the PID's requests
                                default:
                                    String str = arduinoMsg.substring("Positive Response".length());
                                    Log.d(TAG,str);
                                    if(str.startsWith("4"))
                                    {
                                        //Engine load
                                        int EngLoad;
                                        String hex = str.substring(1);

                                        //interpret hex string to hex and convert to dec
                                        EngLoad = Integer.parseInt(hex.trim(), 16);

                                        //Append the engine load to textview
                                        text.append("Engine Load: " + Math.round((float) EngLoad / 2.55) + "%\n");

                                    }else if(str.startsWith("5"))
                                    {
                                        //Coolant temperature
                                        int CoolantTemp;
                                        String hex = str.substring(1);

                                        //interpret hex string to hex and convert to dec
                                        CoolantTemp = Integer.parseInt(hex.trim(), 16);

                                        //Append the engine load to textview
                                        text.append("Coolant Temperature: " + Integer.toString(CoolantTemp-40) + " Celsius\n");

                                        //Offer a feedback to the user about the temperature
                                        if(CoolantTemp-40 > 200)
                                        {
                                            String Warning ="<font color=#e69b00>Warning, high temperature</font>";
                                            text.append(Html.fromHtml(Warning));
                                        }else{
                                            text.append("Normal temperature");
                                        }

                                    }else if(str.startsWith("A"))
                                    {
                                        //Fuel pressure
                                        int FuelPressure;
                                        String hex = str.substring(1);

                                        //interpret hex string to hex and convert to dec
                                        FuelPressure = Integer.parseInt(hex.trim(), 16);

                                        //Append the engine load to textview
                                        text.append("Fuel pressure: " + Integer.toString(3*FuelPressure) + " kPa\n");

                                        if(3*FuelPressure > 600)
                                        {
                                            String Warning ="<font color=#e69b00>Warning, high fuel pressure</font>";
                                            text.append(Html.fromHtml(Warning));
                                        }else{
                                            text.append("Normal pressure");
                                        }

                                    }else if(str.startsWith("C")) {
                                        //RPM
                                        int RPM_A=0,RPM_B=0;
                                        String hex = str.substring(1);

                                        String Hex_A = hex.substring(0,2);

                                        String Hex_B = hex.substring(2);

                                        //interpret hex string to hex and convert to dec
                                        RPM_A = Integer.parseInt(Hex_A.trim(), 16);
                                        RPM_B = Integer.parseInt(Hex_B.trim(), 16);

                                        Log.d(TAG,Hex_A);
                                        Log.d(TAG,Hex_B);

                                        //Append the engine load to textview
                                        text.append("Engine speed: " + Integer.toString((256*RPM_A+RPM_B)/4) + " RPM\n");

                                    }else if(str.startsWith("D")) {
                                        //Throttle Position
                                        int TP=0;

                                        String hex = str.substring(1);

                                        TP=Integer.parseInt(hex.trim(),16);
                                        Log.d(TAG,Integer.toString(TP));

                                        //Append the engine load to textview
                                        text.append("Throttle Position: " + Integer.toString((100*TP)/255) + "%\n");

                                    }else if(text.length()!=0)
                                            text.append(str + "\n");
                                        break;

                            }

                        }
                        else
                        {
                            Log.e("ArduinoMSG",arduinoMsg);
                            String NS = "<font color=#ff0000>Negative Response</font>";
                            text.append(Html.fromHtml(NS));
                            text.append("\n");
                            text.append(arduinoMsg+"\n");
                        }
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
                CMD = 33;
                connectedThread.write("33");
            }
        });

        btnPID.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                //Set the textview
                text.setText("GENERAL INFO\n");
                text.append("Some PIDs may not be implemented!\n");

                //Variable to inform that the textview shouldn't be cleared anymore
                PID=true;

                //Starting the thread
                MyThread thread = new MyThread();
                thread.start();

                return true;
            }
        });

        //Button for getting the DTC's number
        btnDTCNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Sending the command
                CMD = 34;
                connectedThread.write("34");
            }
        });

        //Button for getting the DTC codes
        btnGetDTC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Sending the command
                CMD = 35;
                connectedThread.write("35");
            }
        });

        //Button for clearing the DTC codes
        btnClearDTC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO implement a warning
                //Sending the command
                CMD = 36;
                connectedThread.write("36");
            }
        });

        //Button used for logging the response
        SendToDB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!text.getText().toString().equals("The error description will appear here..."))
                {
                    if(text.length()>0) {
                        //Log.i("TEXT",text.getText().toString());

                        //If we have relevant information displayed than send it to Firebase DB
                        WriteDataToDB("", text.getText().toString(), "LOG");
                        Toast.makeText(MainActivity.this, "Data logged", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(MainActivity.this, "No relevant data to be logged", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(MainActivity.this, "No relevant data to be logged", Toast.LENGTH_SHORT).show();
                }
            }
        });

        BvButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Sending the command
                CMD = 37;
                connectedThread.write("37");
            }
        });

        DetectProtocol.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Sending the command
                CMD = 38;
                connectedThread.write("38");
            }
        });

    }

    /**
     * Thread to send all the commands supported to get all the information
     * !Command 0x0100 (Get Supported PIDs) must be executed first
     */
    public class MyThread extends Thread {
        public void run(){
            Looper.prepare();//Call looper.prepare()

            //Variable to test if previous Get Supported PIDs command has been executed
            boolean SupportedPIDComm = false;
            int index = 2;
            while(index<33)
            {
                //Verify if Pid (represented by the index) is suppported
                if(SupportedPIDs[index] == 1)
                {
                    //Filter the indexes we send, because not all supported indexes will be implemented in Arduino code
                    //Current implemented indexes: 4, 5, 10, 12, 17
                    if(index == 4 || index == 5 || index == 10 || index == 12 || index == 17) {
                        Log.e(TAG, String.valueOf(SupportedPIDs[index]));
                        SupportedPIDComm = true;
                        if (index >= 10) {
                            Log.e("Index", Integer.toString(index));
                            MainActivity.connectedThread.write(Integer.toString(index));
                        } else {
                            String Comanda = null;
                            Comanda = "0" + index;
                            Log.e("Commmmm", Comanda);
                            MainActivity.connectedThread.write(Comanda);
                        }
                        //Put the thread to sleep because Arduino is not that fast
                        try {
                            Thread.sleep(900);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                index++;
            }
            if(!SupportedPIDComm)
            {
                //Let the user know that he must execute the "Get Supported PIDs" command first
                Toast.makeText(MainActivity.this,"Please execute Get Supported PIDs first",Toast.LENGTH_LONG).show();
            }
            //All the commands have been transmitted, reset the PID variable
            //The textview can be cleared when a new request is sent
            PID=false;

            Log.d("Array", Arrays.toString(SupportedPIDs));
            Looper.loop();//Call looper.prepare()
        }
    }
    /**
     * @brief Function to read from the .csv file that contains all the DTCID's
     * @param DTCID
     */
    @SuppressLint("SetTextI18n")
    public void ReadData(final String DTCID){
        boolean DTCIDFOUND = false;
        Log.d("Read Data Function","IN FUNCTION");

        try {
            InputStream in = getResources().openRawResource(R.raw.obdtroublecodes);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)
            );

            String line="";
            try {
                line = reader.readLine();
                //Log.d("Line read", line);
                //Read all the lines until DTC id is found
                while(line != null){
                    //Split by ","
                    String[] tokens = line.split(",");

                    //Search for DTCID
                    if(tokens[0].equals(DTCID))
                    {
                        //Categorize the problem
                        if(DTCID.startsWith("P"))
                            text.append(">Powertrain problem:\n");
                        else if(DTCID.startsWith("C"))
                            text.append(">Chassis problem:\n");
                        else if(DTCID.startsWith("B"))
                            text.append(">Body problem:\n");
                        else if(DTCID.startsWith("U"))
                            text.append(">Network problem:\n");

                        //set the value of DTCIDFOUND to true
                        DTCIDFOUND = true;

                        Log.i("Description",tokens[1]);
                        //Output the description of the DTC
                        if(text.length()==0)
                            text.setText(tokens[1]+"\n");
                        else
                            text.append(tokens[1]+"\n");
                        break;
                    }
                    //Read new line
                    line = reader.readLine();
                }
                if(!DTCIDFOUND) {
                    Toast.makeText(this, "DTC Not found in local DB", Toast.LENGTH_LONG).show();
                    //insert the unknown dtc into the db for further investigation
                    WriteDataToDB(DTCID,"UNKNOWN","UNKNOWN DTCS");
                    Toast.makeText(this, "Unknown DTC added in online DB", Toast.LENGTH_LONG).show();
                }

            }catch (Exception e) {
                e.printStackTrace();
            }

        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this,"Can't open .csv file",Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Function to write data into the Firebase Database
     * Will be used to store unknown DTC's and Log data of the response
     * @param dtc_code
     * @param Desc
     * @param Collection
     */
    public void WriteDataToDB(String dtc_code,String Desc,String Collection)
    {
        Log.d(TAG,dtc_code);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        //get date and time as the document name to be inserted
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String millisInString  = dateFormat.format(new Date());

        // Create a new object with the DTC id and the description unknown
        Map<String, Object> obj = new HashMap<>();
        //Insert the unknown dtc only if the parameter exists
        if(!dtc_code.equals(""))
            obj.put("DTCID", dtc_code);
        //This parameter will contain the description of the DTC or the full log data
        obj.put("DESCRIPTION", Desc);
        obj.put("TIMESTAMP", millisInString);

        // Add a new document with a generated ID
        db.collection(Collection)
                .document()
                .set(obj).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG,"Document added");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG,"Error adding document");
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
            byte [] bytes = input.getBytes();
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
            } catch (IOException ignored) { }
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
