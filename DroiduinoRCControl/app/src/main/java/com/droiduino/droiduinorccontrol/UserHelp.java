package com.droiduino.droiduinorccontrol;

import android.os.Bundle;
import android.text.Html;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

public class UserHelp extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_help);

        Objects.requireNonNull(getSupportActionBar()).setTitle(Html.fromHtml("<font color=#FFFFFF>" + getString(R.string.app_name)+ "</font>"));
    }
}
