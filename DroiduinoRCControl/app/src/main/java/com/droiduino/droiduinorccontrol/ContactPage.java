package com.droiduino.droiduinorccontrol;

import android.os.Bundle;
import android.text.Html;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

public class ContactPage extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_page);

        Objects.requireNonNull(getSupportActionBar()).setTitle(Html.fromHtml("<font color=#FFFFFF>" + "Contact Page"+ "</font>"));
    }
}
