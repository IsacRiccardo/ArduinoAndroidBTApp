package com.droiduino.droiduinorccontrol;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.api.Distribution;

import org.apache.commons.collections4.Get;

import java.util.Objects;

public class UserHelp extends AppCompatActivity {

    public TextView SupportedPIDs;
    public TextView DTCNumber;
    public TextView ReadDTC;
    public TextView ClearDTC;
    public TextView GetProtocol;
    public TextView GetVoltage;
    public TextView PIDdesc;
    public LinearLayout expandable_view;
    public ImageButton text_show_more_less;

    public CardView cardView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_help);

        expandable_view = findViewById(R.id.expandable_view);
        text_show_more_less = findViewById(R.id.show_more);
        cardView = findViewById(R.id.cardView1);

        //Set color title to white
        Objects.requireNonNull(getSupportActionBar()).setTitle(Html.fromHtml("<font color=#FFFFFF>" + getString(R.string.app_name)+ "</font>"));

        //Get the id's of the targeted textview's
        SupportedPIDs = findViewById(R.id.DescriptionCommand1);
        DTCNumber = findViewById(R.id.DescriptionCommand2);
        ReadDTC = findViewById(R.id.DescriptionCommand3);
        ClearDTC = findViewById(R.id.DescriptionCommand4);
        GetVoltage = findViewById(R.id.DescriptionCommand5);
        GetProtocol = findViewById(R.id.DescriptionCommand6);
        PIDdesc = findViewById(R.id.pid_desc);

        SpannableString ss_PID = new SpannableString(SupportedPIDs.getText());
        SpannableString ss_DTCnb = new SpannableString(DTCNumber.getText());
        SpannableString ss_DTC = new SpannableString(ReadDTC.getText());
        SpannableString ss_Cleardtc = new SpannableString(ClearDTC.getText());
        SpannableString ss_Protocol = new SpannableString(GetProtocol.getText());
        SpannableString ss_Voltage = new SpannableString(GetVoltage.getText());
        SpannableString ss_PIDdesc = new SpannableString(PIDdesc.getText());

        StyleSpan bold = new StyleSpan(Typeface.BOLD);
        ss_PID.setSpan(bold,0, "Description  Get Supported PID's".length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss_DTCnb.setSpan(bold,0,"Description get number of DTC's".length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss_DTC.setSpan(bold,0,"Description Read DTC's".length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss_Cleardtc.setSpan(bold,0,"Description clear DTC's".length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss_Protocol.setSpan(bold,0,"Description detect protocol".length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss_Voltage.setSpan(bold,0,"Description get battery voltage".length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss_PIDdesc.setSpan(bold,0,"PID's Description presented in Hexadecimal".length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        //Apply the modified text to textview
        SupportedPIDs.setText(ss_PID);
        DTCNumber.setText(ss_DTCnb);
        ReadDTC.setText(ss_DTC);
        ClearDTC.setText(ss_Cleardtc);
        GetVoltage.setText(ss_Voltage);
        GetProtocol.setText(ss_Protocol);
        PIDdesc.setText(ss_PIDdesc);
    }

    @SuppressLint("SetTextI18n")
    public void showmore (View view) {
        if(expandable_view.getVisibility()== View.GONE)
        {
            text_show_more_less.setImageResource(R.drawable.baseline_keyboard_arrow_up_24);
            TransitionManager.beginDelayedTransition(cardView,new AutoTransition());
            expandable_view.setVisibility(View.VISIBLE);
        }else{
            text_show_more_less.setImageResource(R.drawable.baseline_keyboard_arrow_down_24);;
            TransitionManager.beginDelayedTransition(cardView,new AutoTransition());
            expandable_view.setVisibility(View.GONE);
        }
    }
}
