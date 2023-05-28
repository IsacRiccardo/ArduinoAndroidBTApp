package com.droiduino.droiduinorccontrol;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;

import org.apache.commons.lang3.StringUtils;

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
import java.util.Objects;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private Button btnPID;
    private Button btnDTCNumber;
    private Button btnGetDTC;
    private Button btnClearDTC;
    private FloatingActionButton SendToDB;

    private Button DetectProtocol;
    private Button BvButton;
    private Button ConsoleButton;

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

    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update

    @SuppressLint({"ClickableViewAccessibility", "MissingInflatedId", "ObjectAnimatorBinding"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        //text
        text = findViewById(R.id.textView);
        Dtext = findViewById(R.id.PrintPanelTitle);
        TextView Commands = findViewById(R.id.CommandsTitle);
        //buttons
        btnPID = findViewById(R.id.buttonPID);
        btnDTCNumber = findViewById(R.id.buttonDtcNumber);
        btnGetDTC = findViewById(R.id.buttonGetDTC);
        SendToDB = findViewById(R.id.floatingActionButton);
        BvButton = findViewById(R.id.buttonBV);
        btnClearDTC = findViewById(R.id.buttonClearDTC);
        DetectProtocol = findViewById(R.id.buttonDP);
        ConsoleButton = findViewById(R.id.buttonConsole);

        //Disable buttons until BT connection is established
        btnPID.setEnabled(false);
        btnDTCNumber.setEnabled(false);
        btnGetDTC.setEnabled(false);
        Dtext.setEnabled(false);
        btnClearDTC.setEnabled(false);
        SendToDB.setEnabled(false);
        if(!SendToDB.isEnabled())
            SendToDB.setVisibility(View.GONE);
        BvButton.setEnabled(false);
        DetectProtocol.setEnabled(false);
        Commands.setEnabled(false);
        ConsoleButton.setEnabled(false);

        Objects.requireNonNull(getSupportActionBar()).setTitle(Html.fromHtml("<font color=#FFFFFF>" + getString(R.string.app_name)+ "</font>"));

        // If a bluetooth device has been selected from SelectDeviceActivity
        deviceName = getIntent().getStringExtra("deviceName");
        if (deviceName != null){
            // Get the device address to make BT Connection
            String deviceAddress = getIntent().getStringExtra("deviceAddress");
            // Show progress and connection status
            progressBar.setVisibility(View.VISIBLE);

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
                                //if connected enable the commands interface
                                Toast.makeText(MainActivity.this, "Device Connected", Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(View.GONE);
                                btnPID.setEnabled(true);
                                btnDTCNumber.setEnabled(true);
                                btnGetDTC.setEnabled(true);
                                btnClearDTC.setEnabled(true);
                                SendToDB.setEnabled(true);
                                BvButton.setEnabled(true);
                                if(SendToDB.isEnabled())
                                    SendToDB.setVisibility(View.VISIBLE);
                                DetectProtocol.setEnabled(true);
                                text.setEnabled(true);
                                ConsoleButton.setEnabled(true);
                                text.setMovementMethod(new ScrollingMovementMethod());
                                break;
                            case -1:
                                Toast.makeText(MainActivity.this, "Failed to connect", Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(View.GONE);
                                SendToDB.setVisibility(View.GONE);
                                break;
                        }
                        break;

                    case MESSAGE_READ:
                        //Clearing the textview if PID is false
                        //If PID is 1 than do not clear it because information about subfunctions must be displayed
                        if(!PID)
                            text.setText("");
                        else
                            text.append("\n");

                        // Read message from Arduino
                        String arduinoMsg = msg.obj.toString();

                        //If message starts with positive response than begin processing all the response,
                        //based on what command has been sent
                        if(arduinoMsg.contains("Positive Response"))
                        {
                            text.append(">Positive Response\n");

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
                                        text.setText(">Service 1 Supported Subfunctions:\n");
                                    }else
                                        text.append(">Service 1 Supported Subfunctions:\n");

                                    int i = 0;

                                    //Iterate through the array of tokens
                                    while(i<tokens.length - 1){
                                        //Log.d("INT",Integer.valueOf(tokens[i]).toString());

                                        //At the position of the supported PID we set the value to 1
                                        //We make a vector of appearances
                                        SupportedPIDs[Integer.parseInt(tokens[i])] = 1;

                                        i++;
                                    }

                                    int j=1;

                                    //Iterate through the vector of appearances and set the textview
                                    while(j<SupportedPIDs.length)
                                    {
                                        if(SupportedPIDs[j]==1)
                                        {
                                            switch (j)
                                            {
                                                case 1:
                                                    text.append("\n>Monitor status since DTCs cleared.\n");
                                                    break;
                                                case 2:
                                                    text.append("\n>DTC that caused freeze frame to be stored.\n");
                                                    break;
                                                case 3:
                                                    text.append("\n>Fuel system status.\n");
                                                    break;
                                                case 4:
                                                    text.append("\n>Calculated engine load.\n");
                                                    break;
                                                case 5:
                                                    text.append("\n>Engine coolant temperature.\n");
                                                    break;
                                                case 6:
                                                    text.append("\n>Short term fuel trim—Bank 1.\n");
                                                    break;
                                                case 7:
                                                    text.append("\n>Long term fuel trim—Bank 1.\n");
                                                    break;
                                                case 8:
                                                    text.append("\n>Short term fuel trim—Bank 2.\n");
                                                    break;
                                                case 9:
                                                    text.append("\n>Long term fuel trim—Bank 2.\n");
                                                    break;
                                                case 10:
                                                    text.append("\n>Fuel pressure.\n");
                                                    break;
                                                case 11:
                                                    text.append("\n>Intake manifold absolute pressure.\n");
                                                    break;
                                                case 12:
                                                    text.append("\n>Engine speed.\n");
                                                    break;
                                                case 13:
                                                    text.append("\n>Vehicle speed.\n");
                                                    break;
                                                case 14:
                                                    text.append("\n>Timing advance.\n");
                                                    break;
                                                case 15:
                                                    text.append("\n>Intake air temperature.\n");
                                                    break;
                                                case 16:
                                                    text.append("\n>Mass air flow sensor (MAF) air flow rate.\n");
                                                    break;
                                                case 17:
                                                    text.append("\n>Throttle position.\n");
                                                    break;
                                                case 18:
                                                    text.append("\n>Commanded secondary air status.\n");
                                                    break;
                                                case 19:
                                                    text.append("\n>Oxygen sensors present (in 2 banks).\n");
                                                    break;
                                                case 20:
                                                    text.append("\n>Oxygen Sensor 1\n" +
                                                                "A: Voltage\n" +
                                                                "B: Short term fuel trim.\n");
                                                    break;
                                                case 21:
                                                    text.append("\n>Oxygen Sensor 2\n" +
                                                                "A: Voltage\n" +
                                                                "B: Short term fuel trim\n");
                                                    break;
                                                case 22:
                                                    text.append("\n>Oxygen Sensor 3\n" +
                                                                "A: Voltage\n" +
                                                                "B: Short term fuel trim\n");
                                                    break;
                                                case 23:
                                                    text.append("\n>Oxygen Sensor 4\n" +
                                                                "A: Voltage\n" +
                                                                "B: Short term fuel trim\n");
                                                    break;
                                                case 24:
                                                    text.append("\n>Oxygen Sensor 5\n" +
                                                                "A: Voltage\n" +
                                                                "B: Short term fuel trim\n");
                                                    break;
                                                case 25:
                                                    text.append("\n>Oxygen Sensor 6\n" +
                                                                "A: Voltage\n" +
                                                                "B: Short term fuel trim\n");
                                                    break;
                                                case 26:
                                                    text.append("\n>Oxygen Sensor 7\n" +
                                                                "A: Voltage\n" +
                                                                "B: Short term fuel trim\n");
                                                    break;
                                                case 27:
                                                    text.append("\n>Oxygen Sensor 8\n" +
                                                                "A: Voltage\n" +
                                                                "B: Short term fuel trim\n");
                                                    break;
                                                case 28:
                                                    text.append("\n>OBD standards this vehicle conforms to.\n");
                                                    break;
                                                case 29:
                                                    text.append("\n>Oxygen sensors present (in 4 banks).\n");
                                                    break;
                                                case 30:
                                                    text.append("\n>Auxiliary input status.\n");
                                                    break;
                                                case 31:
                                                    text.append("\n>Run time since engine start.\n");
                                                    break;
                                                case 32:
                                                    text.append("\n>Additional subfunctions supported but not shown\n");
                                                    break;
                                            }
                                        }
                                        j++;
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
                                        text.append(">Engine check light is ON\n");
                                    else
                                        text.append(">Engine check light is OFF\n");

                                    text.append(">Number of DTC's: ");
                                    //Extract number of DTC from response
                                    String DTCNB = RespData.substring(1);
                                    text.append(DTCNB);

                                    //Reset command variable
                                    CMD = 0;
                                    break;

                                case 35:
                                    //Read DTC response process
                                    String DTCCodes;

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

                                    // If VoltageDesc starts with "V" we know that a command for getting the voltage has been sent
                                    // BatteryDesc will be: V + Voltage Value + V
                                    if(BatteryVDesc.startsWith("V"))
                                    {
                                        //Get the voltage value plus unit of measure
                                        String Voltage = BatteryVDesc.substring(1);

                                        //Processing the response

                                        //Get index where the unit of measure is
                                        int um_index = Voltage.indexOf("V");

                                        //Verify is the unit of measure is present
                                        if(Voltage.contains("V"))
                                        {
                                            //Get only voltage value
                                            String Voltage_Value = Voltage.substring(0,um_index);
                                            Log.i("Value",Voltage_Value);

                                            int dot_index = Voltage_Value.indexOf(".");

                                            //Separate whole from decimal
                                            String whole_part = Voltage_Value.substring(0,dot_index);
                                            String decimal_part = Voltage_Value.substring(dot_index+1,um_index);
                                            Log.i("Whole part",whole_part);
                                            Log.i("Decimal part",decimal_part);

                                            //Verify the validity of the values
                                            if(StringUtils.isNumeric(whole_part) && StringUtils.isNumeric(decimal_part))
                                            {
                                                text.append(">Battery Voltage: " + Voltage + "\n");
                                            }
                                            else{
                                                text.append(">Try again, the value received was not numerical\n");
                                                text.append(">Caused by the OBD2 module\n");
                                            }
                                        }
                                        else{
                                            text.append("Format error, try again\n");
                                        }
                                        Log.d("Battery Voltage", Voltage);

                                    }

                                    //Reset command variable
                                    CMD = 0;
                                    break;

                                case 38:
                                    String ProtocolDesc = arduinoMsg.substring("Positive Response".length());

                                    //If ProtocolDesc starts with "P" we know that a command for getting the protocol has been sent
                                    //ProtocolDesc will be: P + Protocol
                                    if(ProtocolDesc.startsWith("P"))
                                    {
                                        String Protocol = ProtocolDesc.substring(6);

                                        Log.d("PROTOCOL", Protocol);
                                        text.append(">Protocol: " + Protocol + "\n");
                                    }

                                    //Reset command variable
                                    CMD = 0;
                                    break;

                                // The default case will treat the response for the PID's requests
                                default:
                                    String str = arduinoMsg.substring("Positive Response".length());
                                    Log.d(TAG,str);
                                    if(str.startsWith("G"))
                                    {
                                        //Engine load
                                        int EngLoad;
                                        String hex = str.substring(1);

                                        //interpret hex string to hex and convert to dec
                                        EngLoad = Integer.parseInt(hex.trim(), 16);

                                        //Append the engine load to textview
                                        text.append(">Engine Load: " + Math.round((float) EngLoad / 2.55) + "%\n");

                                    }else if(str.startsWith("H"))
                                    {
                                        //Coolant temperature
                                        int CoolantTemp;
                                        String hex = str.substring(1);

                                        //interpret hex string to hex and convert to dec
                                        CoolantTemp = Integer.parseInt(hex.trim(), 16);

                                        //Append the engine load to textview
                                        text.append(">Coolant Temperature: " + (CoolantTemp - 40) + " Celsius\n");

                                        //User feedback
                                        if(CoolantTemp - 40 > 200)
                                            text.append("Warning, high temperature\n");

                                    }else if(str.startsWith("I"))
                                    {
                                        //Fuel pressure
                                        int FuelPressure;
                                        String hex = str.substring(1);

                                        //interpret hex string to hex and convert to dec
                                        FuelPressure = Integer.parseInt(hex.trim(), 16);

                                        //Append the engine load to textview
                                        text.append(">Fuel pressure: " + Integer.toString(3*FuelPressure) + " kPa\n");

                                        //User feedback
                                        if(3*FuelPressure > 600)
                                            text.append("Warning, high fuel pressure\n");

                                    }else if(str.startsWith("L")) {
                                        //RPM
                                        int RPM_A,RPM_B;
                                        String hex = str.substring(1);

                                        String Hex_A = hex.substring(0,2);

                                        String Hex_B = hex.substring(2);

                                        //interpret hex string to hex and convert to dec
                                        RPM_A = Integer.parseInt(Hex_A.trim(), 16);
                                        RPM_B = Integer.parseInt(Hex_B.trim(), 16);

                                        Log.d(TAG,Hex_A);
                                        Log.d(TAG,Hex_B);

                                        //Append the engine load to textview
                                        text.append(">Engine speed: " + (256 * RPM_A + RPM_B) / 4 + " RPM\n");

                                    }else if(str.startsWith("M")) {
                                        //Throttle Position
                                        int TP;

                                        String hex = str.substring(1);

                                        TP=Integer.parseInt(hex.trim(),16);
                                        Log.d(TAG,Integer.toString(TP));

                                        //Append the engine load to textview
                                        text.append(">Throttle Position: " + (100 * TP) / 255 + "%\n");

                                    }else if(text.length()!=0)
                                            text.append(">" + str + "\n");
                                        break;

                            }

                        }
                        // If it does not contain positive response than that means that:
                        // 1 - we have a negative response
                        // 2 - we have a raw response because a command has been sent from the console
                        else
                        {
                            Log.e("ArduinoMSG",arduinoMsg);
                            text.append(">" + arduinoMsg+ "\n");
                        }
                        break;
                }
            }
        };


        //Button for getting suppported PID's
        btnPID.setOnClickListener(view -> {
            //Sending the command
            CMD = 33;
            connectedThread.write("ff33");
        });

        btnPID.setOnLongClickListener(view -> {

            //Animate button
            Animation animation = AnimationUtils.loadAnimation(this,R.anim.blink_anim);
            btnPID.startAnimation(animation);

            //Set the textview
            text.setText("GENERAL INFO\n");
            text.append("Some subfunctions may not be implemented!\n");

            //Variable to inform that the textview shouldn't be cleared anymore
            PID=true;

            //Starting the thread
            MyThread thread = new MyThread();
            thread.start();

            return true;
        });

        //Button for getting the DTC's number
        btnDTCNumber.setOnClickListener(view -> {
            //Sending the command
            CMD = 34;
            connectedThread.write("ff34");
        });

        //Button for getting the DTC codes
        btnGetDTC.setOnClickListener(view -> {
            //Sending the command
            CMD = 35;
            connectedThread.write("ff35");
        });

        //Button for clearing the DTC codes
        btnClearDTC.setOnClickListener(view -> {
            //TODO implement a warning
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("CLEAR DTC'S")
                    .setMessage("Are you sure you want to delete the Diagnostic Trouble Codes?")

                    // Specifying a listener allows you to take an action before dismissing the dialog.
                    // The dialog is automatically dismissed when a dialog button is clicked.
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Continue with delete operation
                            //Sending the command
                            CMD = 36;
                            connectedThread.write("ff36");
                        }
                    })

                    // A null listener allows the button to dismiss the dialog and take no further action.
                    .setNegativeButton(android.R.string.cancel, null)
                    .setIcon(R.drawable.baseline_warning_amber_24)
                    .show();

        });

        //Button used for logging the response
        SendToDB.setOnClickListener(view -> {
            //Animate button
            Animation animation = AnimationUtils.loadAnimation(this,R.anim.blink_anim);
            SendToDB.startAnimation(animation);

            if(!text.getText().toString().equals(">The error description will appear here..."))
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
        });

        BvButton.setOnClickListener(view -> {
            //Sending the command
            CMD = 37;
            connectedThread.write("ff37");
        });

        DetectProtocol.setOnClickListener(view -> {
            //Sending the command
            CMD = 38;
            connectedThread.write("ff38");
        });

        ConsoleButton.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            @SuppressLint("InflateParams")
            View AlertView = getLayoutInflater().inflate(R.layout.alert_dialog,null);

            final EditText command = (EditText) AlertView.findViewById(R.id.etCommand);
            Button Send_c = (Button) AlertView.findViewById(R.id.btnSend);

            builder.setView(AlertView);
            AlertDialog dialog = builder.create();
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.show();

            Send_c.setOnClickListener(view1 -> {
                if(!command.getText().toString().isEmpty()) {
                    String c_Text = command.getText().toString();
                    switch (c_Text.length()) {
                        case 1:
                        case 3:
                            Toast.makeText(MainActivity.this, "Invalid command", Toast.LENGTH_LONG).show();
                            break;
                        case 2:
                            connectedThread.write("ff" + c_Text + '\n');
                            break;
                        case 4:
                            connectedThread.write(c_Text + '\n');
                            break;
                        default:
                            Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
                else{
                    Toast.makeText(MainActivity.this,"Please insert Command",Toast.LENGTH_SHORT).show();
                }
                dialog.cancel();
            });


        });
    }

    /**
     * On create function for menu
     * @param featureId
     * @param menu
     * @return
     */
    @SuppressLint("RestrictedApi")
    @Override
    //Menu creation override
    public boolean onCreatePanelMenu(int featureId, @NonNull Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu,menu);

        if(menu instanceof MenuBuilder){
            MenuBuilder m = (MenuBuilder) menu;
            m.setOptionalIconsVisible(true);
        }
        return true;
    }

    /**
     * Describe what happens when a button from menu is clicked
     * @param item
     * @return
     */
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId())
        {
            case R.id.help:
                //Display help for user
                Intent intentHelp = new Intent(MainActivity.this, UserHelp.class);
                startActivity(intentHelp);
                break;
            case R.id.connect:
                //Connect to bluetooth device
                Intent intentConn = new Intent(MainActivity.this, SelectDeviceActivity.class);
                startActivity(intentConn);
                break;
            case R.id.contact:
                //Access contact page
                Intent intentCont = new Intent(MainActivity.this, ContactPage.class);
                startActivity(intentCont);
                break;
            case R.id.gmaps:
                //Access google maps and search for mechanic
                Uri gmmIntentUri = Uri.parse("geo:0,0?q=mechanic");
                Intent intentGmaps = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                intentGmaps.setPackage("com.google.android.apps.maps");
                startActivity(intentGmaps);
                break;
        }
        return true;
    }

    /**
     * Thread to send all the commands supported to get all the information
     * !Command 0x0100 (Get Supported Subfunctions) must be executed first
     */
    public class MyThread extends Thread {
        public void run(){

            Looper.prepare();//Call looper.prepare()

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    //Disable commands while thread is running
                    btnPID.setEnabled(false);
                    btnDTCNumber.setEnabled(false);
                    btnGetDTC.setEnabled(false);
                    Dtext.setEnabled(false);
                    btnClearDTC.setEnabled(false);
                    SendToDB.setVisibility(View.GONE);
                    BvButton.setEnabled(false);
                    DetectProtocol.setEnabled(false);
                    ConsoleButton.setEnabled(false);
                }
            });

            //Variable to test if previous Get Supported PIDs command has been executed
            boolean SupportedPIDFlag = false;
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
                        SupportedPIDFlag = true;
                        if(index < 10)
                        {
                            String Comanda;
                            Comanda = "ff0" + index;
                            Log.e("Commmm", Comanda);
                            MainActivity.connectedThread.write(Comanda);
                        }
                        else {
                            String Comanda;
                            Comanda = "ff" + index;
                            Log.e("Commmm", Comanda);
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
            if(!SupportedPIDFlag)
            {
                //Let the user know that he must execute the "Get Supported PIDs" command first
                Toast.makeText(MainActivity.this,"Please execute Get Supported PIDs first",Toast.LENGTH_LONG).show();
            }
            //All the commands have been transmitted, reset the PID variable
            //The textview can be cleared when a new request is sent
            PID=false;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    //Enable commands while thread is running
                    btnPID.setEnabled(true);
                    btnDTCNumber.setEnabled(true);
                    btnGetDTC.setEnabled(true);
                    Dtext.setEnabled(true);
                    btnClearDTC.setEnabled(true);
                    SendToDB.setVisibility(View.VISIBLE);
                    BvButton.setEnabled(true);
                    DetectProtocol.setEnabled(true);
                    ConsoleButton.setEnabled(true);
                }
            });

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

            String line;
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
