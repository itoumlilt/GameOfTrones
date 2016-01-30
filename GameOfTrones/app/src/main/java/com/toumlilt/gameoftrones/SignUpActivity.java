package com.toumlilt.gameoftrones;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class SignUpActivity extends AppCompatActivity {

    private Button   signUpButton;
    private EditText signUpEditText;

    private SharedPreferences sharedPreferences;
    public static final String USER_PREFS = "USERPREFS";
    public static final String PREF_USERNAME = "USERNAMEkey";
    public static final String PREF_USERDESC = "USERDESCkey";

    public final static String EXTRA_MESSAGE = "com.toumlilt.gameottrones.MESSAGE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        this.signUpButton = (Button) findViewById(R.id.signUpButton);
        this.signUpEditText = (EditText) findViewById(R.id.signUpEditText);

        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.background_sound);
        mediaPlayer.start();

        this.sharedPreferences = getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE);

        /* testing if a user already exists */
        if(!sharedPreferences.getString(PREF_USERNAME, "").isEmpty()){
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra(EXTRA_MESSAGE, sharedPreferences.getString(PREF_USERNAME, ""));
            startActivity(intent);
        }

    }

    public void onClickSignUpButton(View view) {
        // Do something in response to signUpButton

        /* saving the new username */
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_USERNAME, signUpEditText.getText().toString());
        editor.commit();

        /* starting game activity */
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra(EXTRA_MESSAGE, signUpEditText.getText().toString());
        startActivity(intent);
    }
}
