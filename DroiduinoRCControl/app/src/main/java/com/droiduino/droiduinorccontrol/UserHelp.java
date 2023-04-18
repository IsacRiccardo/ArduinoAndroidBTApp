package com.droiduino.droiduinorccontrol;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import net.cachapa.expandablelayout.ExpandableLayout;

import java.util.Objects;

public class UserHelp extends AppCompatActivity {

    public TextView SupportedPIDs;
    public TextView DTCNumber;
    public TextView ReadDTC;
    public TextView ClearDTC;
    public TextView GetProtocol;
    public TextView GetVoltage;
    public TextView PIDdesc;

    //Cardview variables for first command
    public ExpandableLayout expandable_view;
    public ImageButton text_show_more_less;

    //Cardview variables for second command
    public ExpandableLayout expandable_view_2;
    public ImageButton text_show_more_less_2;

    //Cardview variables for third command
    public ExpandableLayout expandable_view_3;
    public ImageButton text_show_more_less_3;

    //Cardview variables for fourth command
    public ExpandableLayout expandable_view_4;
    public ImageButton text_show_more_less_4;

    //Cardview variables for fifth command
    public ExpandableLayout expandable_view_5;
    public ImageButton text_show_more_less_5;

    //Cardview variables for sixth command
    public ExpandableLayout expandable_view_6;
    public ImageButton text_show_more_less_6;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_help);

        //PID's
        expandable_view = findViewById(R.id.expandable_view);
        text_show_more_less = findViewById(R.id.show_more);

        //DTC number
        expandable_view_2 = findViewById(R.id.expandable_view_2);
        text_show_more_less_2 = findViewById(R.id.show_more_2);
        //Read DTC
        expandable_view_3 = findViewById(R.id.expandable_view_3);
        text_show_more_less_3 = findViewById(R.id.show_more_3);
        //Clear DTC
        expandable_view_4 = findViewById(R.id.expandable_view_4);
        text_show_more_less_4 = findViewById(R.id.show_more_4);
        //Detect protocol
        expandable_view_5 = findViewById(R.id.expandable_view_5);
        text_show_more_less_5 = findViewById(R.id.show_more_5);
        //Detect voltage
        expandable_view_6 = findViewById(R.id.expandable_view_6);
        text_show_more_less_6 = findViewById(R.id.show_more_6);

        //Set color title to white
        Objects.requireNonNull(getSupportActionBar()).setTitle(Html.fromHtml("<font color=#FFFFFF>" + "User Help"+ "</font>"));

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
        ss_PID.setSpan(bold,0, "Description  get Supported Subfunctions".length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss_DTCnb.setSpan(bold,0,"Description get number of DTC's".length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss_DTC.setSpan(bold,0,"Description read DTC's".length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss_Cleardtc.setSpan(bold,0,"Description clear DTC's".length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss_Protocol.setSpan(bold,0,"Description detect protocol".length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss_Voltage.setSpan(bold,0,27,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
    public void showmore_PID (View view) {
        if(!expandable_view.isExpanded())
        {
            text_show_more_less.setImageResource(R.drawable.baseline_keyboard_arrow_up_24);
            expandable_view.expand();
        }else{
            text_show_more_less.setImageResource(R.drawable.baseline_keyboard_arrow_down_24);;
            expandable_view.collapse();
        }
    }
    @SuppressLint("SetTextI18n")
    public void showmore_DTC_Number (View view) {
        if(!expandable_view_2.isExpanded())
        {
            text_show_more_less_2.setImageResource(R.drawable.baseline_keyboard_arrow_up_24);
            expandable_view_2.expand();
        }else{
            text_show_more_less_2.setImageResource(R.drawable.baseline_keyboard_arrow_down_24);;
            expandable_view_2.collapse();
        }

    }

    public void showmore_get_dtc(View view) {
        if(!expandable_view_3.isExpanded())
        {
            text_show_more_less_3.setImageResource(R.drawable.baseline_keyboard_arrow_up_24);
            expandable_view_3.expand();
        }else{
            text_show_more_less_3.setImageResource(R.drawable.baseline_keyboard_arrow_down_24);;
            expandable_view_3.collapse();
        }
    }

    public void showmore_clear_dtc(View view) {
        if(!expandable_view_4.isExpanded())
        {
            text_show_more_less_4.setImageResource(R.drawable.baseline_keyboard_arrow_up_24);
            expandable_view_4.expand();
        }else{
            text_show_more_less_4.setImageResource(R.drawable.baseline_keyboard_arrow_down_24);;
            expandable_view_4.collapse();
        }
    }

    public void showmore_DP(View view) {
        if(!expandable_view_5.isExpanded())
        {
            text_show_more_less_5.setImageResource(R.drawable.baseline_keyboard_arrow_up_24);
            expandable_view_5.expand();
        }else{
            text_show_more_less_5.setImageResource(R.drawable.baseline_keyboard_arrow_down_24);;
            expandable_view_5.collapse();
        }
    }

    public void showmore_DV(View view) {
        if(!expandable_view_6.isExpanded())
        {
            text_show_more_less_6.setImageResource(R.drawable.baseline_keyboard_arrow_up_24);
            expandable_view_6.expand();
        }else{
            text_show_more_less_6.setImageResource(R.drawable.baseline_keyboard_arrow_down_24);;
            expandable_view_6.collapse();
        }
    }
}
