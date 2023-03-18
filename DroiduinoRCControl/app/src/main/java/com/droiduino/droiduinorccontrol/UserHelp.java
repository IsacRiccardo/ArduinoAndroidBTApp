package com.droiduino.droiduinorccontrol;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

public class UserHelp extends AppCompatActivity {

    public TextView SupportedPIDs;
    public TextView DTCNumber;
    public TextView ReadDTC;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_help);

        //Set color title to white
        Objects.requireNonNull(getSupportActionBar()).setTitle(Html.fromHtml("<font color=#FFFFFF>" + getString(R.string.app_name)+ "</font>"));

        //Get the id's of the targeted textview's
        SupportedPIDs = findViewById(R.id.DescriptionCommand1);
        DTCNumber = findViewById(R.id.DescriptionCommand2);
        ReadDTC = findViewById(R.id.DescriptionCommand3);

        SpannableString ss_PID = new SpannableString(SupportedPIDs.getText());
        SpannableString ss_DTCnb = new SpannableString(DTCNumber.getText());
        SpannableString ss_DTC = new SpannableString(ReadDTC.getText());

        StyleSpan bold = new StyleSpan(Typeface.BOLD);
        ss_PID.setSpan(bold,0, "Description  Get Supported PID's".length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss_DTCnb.setSpan(bold,0,"Description get number of DTC's".length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss_DTC.setSpan(bold,0,"Description Read DTC's".length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        //Apply the modified text to textview
        SupportedPIDs.setText(ss_PID);
        DTCNumber.setText(ss_DTCnb);
        ReadDTC.setText(ss_DTC);

    }
}
