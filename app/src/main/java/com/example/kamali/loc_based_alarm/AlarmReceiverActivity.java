package com.example.kamali.loc_based_alarm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

/**
 * Created by kamali on 1/5/17.
 */
public class AlarmReceiverActivity extends AppCompatActivity {

    int prefsnoozeduration ;


    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.alarmreceiver_activity_layout);
        Intent i = getIntent();

        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        prefsnoozeduration =Integer.parseInt(sharedPrefs.getString("prefsnoozeduration","5"));

        final AlarmListFragment.AlarmData alarmobj= i.getParcelableExtra("Alarmobject");
        AlarmListFragment.playRingtone(alarmobj);
        Button alarmsnooze= (Button)findViewById(R.id.snoozealarm);
        Button cancelalarmbutton= (Button)findViewById(R.id.cancelalarm);

        alarmsnooze.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                AlarmListFragment.stopRingtone(alarmobj);
                AlarmListFragment.SnoozeAlarm(alarmobj,prefsnoozeduration);
                finish();
            }
        });

        cancelalarmbutton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AlarmListFragment.stopRingtone(alarmobj);
                finish();

            }
        });
    }
}
