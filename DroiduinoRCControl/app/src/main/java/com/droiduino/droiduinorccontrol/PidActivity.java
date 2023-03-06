package com.droiduino.droiduinorccontrol;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;

public class PidActivity extends AppCompatActivity {

    public int[] SupPIDs;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pid_activity);

        //Set description text
        TextView pageDesc = findViewById(R.id.PageDescText);
        pageDesc.setEnabled(false);

        //Extract the array that has all the supported PID's
        Intent intent = getIntent();
        SupPIDs = intent.getIntArrayExtra("Supported PID");

        //TODO test if all the elements are 0 => GET SUPPORTED PID'S service has not been sent
        if(true)
        {
            Toast.makeText(this,"Supported PID's not determined",Toast.LENGTH_LONG).show();
            pageDesc.setText("Go back and determined the supported PID's");
        }else
            pageDesc.setText("Send additional requests");

        Log.d("Array", Arrays.toString(SupPIDs));

        //TODO implement activity page funcitonality

    }
}
