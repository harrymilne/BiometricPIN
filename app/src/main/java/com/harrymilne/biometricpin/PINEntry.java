package com.harrymilne.biometricpin;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.util.Log;
import android.widget.Spinner;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.lang.System;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class PINEntry extends AppCompatActivity {

    private static final String TAG = "biometric_pin";

    private Spinner userSelector;
    private TextView status;
    private TextView pinStatus;
    private User[] users;
    private int currentUser;

    private Button[] numpad = new Button[10];
    private int[] numpad_id = {R.id.numpad0, R.id.numpad1, R.id.numpad2, R.id.numpad3, R.id.numpad4, R.id.numpad5, R.id.numpad6, R.id.numpad7, R.id.numpad8, R.id.numpad9};

    private int num_pos;
    private long[][] held_ms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pinentry);

        Log.i(TAG, "onCreate");

        final Button addUserButton = (Button)findViewById(R.id.addUser);
        final EditText userField = (EditText)findViewById(R.id.usernameField);
        final EditText pinField = (EditText)findViewById(R.id.pinField);
        final Button clearButton = (Button)findViewById(R.id.numpadCLEAR);
        final Button doneButton = (Button)findViewById(R.id.numpadDONE);
        userField.clearFocus();

        status = (TextView)findViewById(R.id.status);
        userSelector = (Spinner)findViewById(R.id.userSelector);
        pinStatus = (TextView)findViewById(R.id.pinStatus);

        userSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadUser(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        addUserButton.setOnClickListener(
                new Button.OnClickListener() {
                    @Override
                    public void onClick(View v){
                        String name = userField.getText().toString();
                        if (!name.equals("Name")) {
                            User user = new User(name);
                            DBManager db = new DBManager(getApplicationContext(), null, null, 0);

                            db.addUser(user);
                            db.close();
                            loadUserData();
                            int last_idx = users.length-1;
                            userSelector.setSelection(last_idx);
                            userField.clearFocus();
                            userField.setText("");
                            InputMethodManager inputManager = (InputMethodManager)
                                    getSystemService(Context.INPUT_METHOD_SERVICE);

                            inputManager.hideSoftInputFromWindow((null == getCurrentFocus()) ? null : getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                            currentUser = last_idx;
                        }
                    }
                }
        );

        pinField.setOnTouchListener(
                new EditText.OnTouchListener(){
                    @Override
                    public boolean onTouch(View view, MotionEvent motion) {
                        view.clearFocus();
                        return true;
                    }
                }
        );


        clearButton.setOnClickListener(
                new Button.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (users[currentUser].get_pin() != null) {
                            held_ms = new long[users[currentUser].get_pin().length()][2];
                        }
                        num_pos = 0;
                        pinField.setText("");
                        loadUserData();
                    }
                }
        );

        doneButton.setOnClickListener(
                new Button.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        num_pos = 0;
                        //if we know the length of the pin
                        if (users[currentUser].get_pin() != null) {
                            String pin = users[currentUser].get_pin();
                            String input = pinField.getText().toString();
                            long[] intervals = new long[pin.length()*2-1];
                            long last_ms = 0;
                            int idx = 0;
                            for (int i = 0; i < held_ms.length; i++) {
                                if (held_ms[i][0] == 0) break;
                                if (last_ms != 0) {
                                    intervals[idx] = held_ms[i][0] - last_ms;
                                    idx++;
                                }
                                intervals[idx] = held_ms[i][1] - held_ms[i][0];
                                idx++;
                                last_ms = held_ms[i][1];
                            }

                            //create array of Timing objects
                            Timing[] timings = new Timing[intervals.length];
                            for (int i = 0; i < intervals.length; i++) {
                                Timing t = new Timing(
                                        users[currentUser].get_id(),
                                        users[currentUser].get_timing_count() + 1,
                                        i,
                                        intervals[i]
                                );
                                timings[i] = t;
                                //status.setText(String.format("%s %d", status.getText(), intervals[i]));
                            }
                            TimingVector t = new TimingVector(timings);
                            if (input.equals(pin)) {
                                DBManager db = new DBManager(getApplicationContext(), null, null, 0);
                                int set_num = users[currentUser].get_timing_count()/(users[currentUser].get_pin().length()*2-1);
                                if (set_num < 10) {
                                    db.addTimingVector(t);
                                    db.close();
                                    loadUserData();
                                } else {
                                    int matches = 0;
                                    double[] comparison = db.compareAllVectors(t);
                                    for (int i = 0; i < comparison.length; i++) {
                                        if (comparison[i] < 100) {
                                            matches++;
                                        }
                                        if (matches > 2) {
                                            status.setText("Success!");
                                        } else {
                                            status.setText("Denied!");
                                        }
                                    }
                                }
                                db.close();
                            } else {
                                status.setText("Incorrect PIN!");
                            }

                        } else {
                            //initialise users pin
                            String pin = pinField.getText().toString();
                            users[currentUser].set_pin(pin);
                            held_ms = new long[pin.length()][2];
                            DBManager db = new DBManager(getApplicationContext(), null, null, 0);
                            db.updateUser(users[currentUser]);
                            db.close();
                            status.setText("User requires 10 more PIN entries");
                            pinStatus.setText("PIN: " + users[currentUser].get_pin());
                            Log.i(TAG, String.format("user pin set to '%s'", pin));
                        }
                        pinField.setText("");
                    }
                }
        );

        for(int i = 0; i < numpad.length; i++) {
            numpad[i] = (Button) findViewById(numpad_id[i]);
            numpad[i].setOnClickListener(
                    new Button.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            for (int i = 0; i < numpad_id.length; i++) {
                                if (v.getId() == numpad_id[i]) {
                                    pinField.setText(String.format(Locale.UK, "%s%d", pinField.getText(), i));
                                    num_pos++;
                                    break;
                                }
                            }
                        }
                    }
                );
            numpad[i].setOnTouchListener(
                    new Button.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent motion) {
                            if (users[currentUser].get_pin() != null)
                                if (num_pos < held_ms.length) {
                                    switch (motion.getAction()) {
                                        case MotionEvent.ACTION_DOWN:
                                            held_ms[num_pos][0] = System.currentTimeMillis();
                                            Log.i(TAG, String.format(Locale.UK, "DOWN: %d", held_ms[num_pos][0]));
                                            break;
                                        case MotionEvent.ACTION_UP:
                                            held_ms[num_pos][1] = System.currentTimeMillis();

                                            Log.i(TAG, String.format(Locale.UK, "UP: %d", held_ms[num_pos][1]));
                                            break;
                                    }
                                }
                            return false;
                        }
                    }
            );
        }

        loadUserData();
    }

    private void disableNumpad() {
        for (int i = 0; i < numpad.length; i++) {
            numpad[i].setEnabled(false);
        }
        final Button clearButton = (Button) findViewById(R.id.numpadCLEAR);
        clearButton.setEnabled(false);
        final Button doneButton = (Button) findViewById(R.id.numpadDONE);
        doneButton.setEnabled(false);
    }

    private void enableNumpad() {
        for (int i = 0; i < numpad.length; i++) {
            numpad[i].setEnabled(true);
        }
        final Button clearButton = (Button) findViewById(R.id.numpadCLEAR);
        clearButton.setEnabled(true);
        final Button doneButton = (Button) findViewById(R.id.numpadDONE);
        doneButton.setEnabled(true);
    }

    private void loadUserData() {
        int position = userSelector.getSelectedItemPosition();
        if (position == -1) {
            position = 0;
        }
        // database handler
        DBManager db = new DBManager(getApplicationContext(), null, null, 0);

        // Spinner Drop down elements
        users = db.getAllUsers();
        currentUser = position;

        String[] labels = new String[users.length];
        for(int i = 0; i < users.length; i++) {
            labels[i] = users[i].get_name();
        }
        // Creating adapter for spinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, labels);

        // Drop down layout style - list view with radio button
        dataAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // attaching data adapter to spinner
        userSelector.setAdapter(dataAdapter);
        userSelector.setSelection(position);
        if (users.length != 0) {
            enableNumpad();
            loadUser(position);
        } else {
            disableNumpad();
        }
    }

    private void loadUser(int idx) {
        currentUser = idx;
        if(users[currentUser].get_pin() == null) {
            pinStatus.setText("");
            status.setText("PIN not initialised");
        } else {
            int set_num = users[currentUser].get_timing_count()/(users[currentUser].get_pin().length()*2-1);
            Log.i(TAG, "set_num: " + Integer.toString(set_num));
            if (set_num < 10) {
                status.setText(String.format("User requires %d more PIN entries", 10 - set_num));
            } else {
                status.setText("Enter PIN:");
            }
            held_ms = new long[users[currentUser].get_pin().length()][2];
            pinStatus.setText("PIN: " + users[currentUser].get_pin());
        }
    }

}
