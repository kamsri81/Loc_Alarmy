package com.example.kamali.loc_based_alarm;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import java.util.Calendar;

/**
 * Created by kamali on 1/5/17.
 */
public class AlarmReceiver extends WakefulBroadcastReceiver {


    @Override
    public void onReceive(final Context context, Intent intent) {

        AlarmListFragment.AlarmData alarmobj= intent.getParcelableExtra("Alarmobject");

        String CurrentArea=AlarmListFragment.getcurrentarea();

            if (alarmobj.SelectedRepeatedCondition == null){
                AlarmListFragment.SetAlarmListToggleState(alarmobj,true);

                if(CurrentArea != null && alarmobj.Selectedcondition.equals("None")!=true) {
                    if(CurrentArea.equals(alarmobj.Selectedcondition)==true){
                        AlarmListFragment.SetAlarmListToggleState(alarmobj,true);
                    }
                    }

            } else{

                /*Set the alarm for the next day*/
                if(android.os.Build.VERSION.SDK_INT >= 19){
                    /*Cancel the old alarm*/
                    AlarmListFragment.CancelAlarm(alarmobj);
                    AlarmListFragment.setAlarm(alarmobj);
                }

                Calendar calendar = Calendar.getInstance();
                int day = calendar.get(Calendar.DAY_OF_WEEK);
                switch (day) {
                    case Calendar.MONDAY:
                        if(alarmobj.SelectedRepeatedCondition.contains("Mon")!=true)
                            return;
                        break;
                    case Calendar.TUESDAY:
                        if(alarmobj.SelectedRepeatedCondition.contains("Tue")!=true)
                            return;
                         break;
                    case Calendar.WEDNESDAY:
                        if(alarmobj.SelectedRepeatedCondition.contains("Wed")!=true)
                            return;
                         break;
                    case Calendar.THURSDAY:
                        if(alarmobj.SelectedRepeatedCondition.contains("Thu")!=true)
                            return;

                         break;
                    case Calendar.FRIDAY:
                        if(alarmobj.SelectedRepeatedCondition.contains("Fri")!=true)
                            return;

                        break;
                    case Calendar.SATURDAY:
                        if(alarmobj.SelectedRepeatedCondition.contains("Sat")!=true)
                            return;

                         break;
                    case Calendar.SUNDAY:
                        if(alarmobj.SelectedRepeatedCondition.contains("Sun")!=true)
                            return;
                        break;
                }


        }


        if(CurrentArea != null && alarmobj.Selectedcondition.equals("None")!=true) {
            if (CurrentArea.equals(alarmobj.Selectedcondition)!=true) {
                return;
            }
        }
        Intent i = new Intent(context, AlarmReceiverActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra("Alarmobject", alarmobj);
        context.startActivity(i);

    }
}