package com.example.kamali.loc_based_alarm;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by kamali on 1/5/17.
 */
public class SetAlarmActivity extends AppCompatActivity {

    private TimePicker alarmTimePicker;
    static ArrayList<HashMap<String,String>> list;
    static SimpleAdapter adapter;
    ArrayList<String> areaentries;
    private static ListView lv;

    final int RQS_RINGTONEPICKER = 1;
    Ringtone ringTone;
    static Uri Selecteduri=null;
    private static final int TEXT_ID = 0;

    static final int FRAGMENT_GROUPID = 31;
    static final int FRAGMENT_GROUPID1=32;
    static final int MENU_ONCE = 1;
    static final int MENU_DAILY = 2;
    static final int MENU_MONTOFRI = 3;

    String Selectedcondition="None";
    static ArrayList <String> SelectedRepeatCondition=null;
    String AlarmName=null;
    String ringTonePath="default";
    boolean newalarm;
    ArrayList<String> selectedRepeatDays=null;


    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setalarmactivity_layout);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final TextView SetAlarmTextView=(TextView)findViewById(R.id.setalarmTextview);

        alarmTimePicker = (TimePicker)findViewById(R.id.alarmTimePicker);
        alarmTimePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {

            @Override


            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {

                String time;

                Calendar calSet = Calendar.getInstance();
                Calendar calcurrent = Calendar.getInstance();
                calSet.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calSet.set(Calendar.MINUTE, minute);
                calSet.set(Calendar.SECOND,00);

                long timediff=  calSet.getTimeInMillis()-calcurrent.getTimeInMillis();

                if (timediff < 0) {
                    timediff += 86400000;
                }

                long secondsInMilli = 1000;
                long minutesInMilli = secondsInMilli * 60;
                long hoursInMilli = minutesInMilli * 60;

                long elapsedHours = timediff / hoursInMilli;
                timediff = timediff % hoursInMilli;

                long elapsedMinutes = timediff / minutesInMilli;
                timediff = timediff % minutesInMilli;

                if(elapsedMinutes<1){
                   time =String.format("Alarm in less than 1 minute");
                }else if(elapsedHours==0){
                    time =String.format("Alarm in  %02d minutes from now",elapsedMinutes);

                }else {

                     time = String.format("Alarm in %d hours %02d minutes from now", elapsedHours, elapsedMinutes);
                }

                SetAlarmTextView.setText(time);

            }});

        lv = (ListView)findViewById(R.id.setalarmlist);
        list = new ArrayList<HashMap<String,String>>();

        HashMap<String, String> map2 = new HashMap<String, String>();
        map2.put("text1", "Alarm Name");
        map2.put("text2", "Press to Enter name");
        list.add(map2);

        HashMap<String, String> map = new HashMap<String, String>();
        map.put("text1", "Repeat");
        map.put("text2", "Never");
        list.add(map);

        HashMap<String, String> map1 = new HashMap<String, String>();
        map1.put("text1", "Ringtone");
        map1.put("text2", "Default");
        list.add(map1);

        HashMap<String, String> map3 = new HashMap<String, String>();
        map3.put("text1", "Select Condition");
        map3.put("text2", "None");
        list.add(map3);

        adapter = new SimpleAdapter(getBaseContext(),list,
                R.layout.setalarmactivity_customrowview,
                new String[] {"text1","text2"},
                new int[] {R.id.text1,R.id.text2}

        );

        lv.setAdapter(adapter);
        registerForContextMenu(lv);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

                if (position == 2) {
                    startRingTonePicker();
                }else if(position == 0) {
                    createAlarmNameDialog(view,position);
                }
                else if (position ==1){
                    CreateRepeatDialog(view,position);
                }

            }
        });

        Intent i = getIntent();
        areaentries=new ArrayList<>();
        areaentries= i.getStringArrayListExtra("key");
        newalarm=i.getExtras().getBoolean("new alarm");
        final AlarmListFragment.AlarmData alarmobj= i.getParcelableExtra("Alarmobject");

        if(newalarm == false) {
            HashMap<String, String> temp = new HashMap<String, String>();

            temp = list.get(0);
            if(alarmobj.AlarmTitle == null){
                temp.put("text2", "Press to Enter name");
            }else {
                temp.put("text2", alarmobj.AlarmTitle);
            }
            list.set(0, temp);
            AlarmName=alarmobj.AlarmTitle;
            adapter.notifyDataSetChanged();

            if(alarmobj.SelectedRepeatedCondition != null) {

                temp = list.get(1);
                String s = "";
                int itercount = 0;

                for (String a : alarmobj.SelectedRepeatedCondition) {
                    if (itercount != 0) {
                        s += ",";
                    }
                    s += a;
                    itercount++;

                }
                temp.put("text2", s);
                list.set(1, temp);
                SelectedRepeatCondition=alarmobj.SelectedRepeatedCondition;
                adapter.notifyDataSetChanged();
            }

            temp = list.get(2);
            Uri uri=Uri.parse(alarmobj.ringtonepath);
            ringTone = RingtoneManager.getRingtone(this, uri);
            temp.put("text2",  ringTone.getTitle(getApplicationContext()));
            list.set(2, temp);
            ringTonePath=alarmobj.ringtonepath;
            adapter.notifyDataSetChanged();

            temp = list.get(3);
            temp.put("text2", alarmobj.Selectedcondition);
            list.set(3, temp);
            Selectedcondition=alarmobj.Selectedcondition;
            adapter.notifyDataSetChanged();


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmTimePicker.setHour(alarmobj.alarmtimepickerhour);
                alarmTimePicker.setMinute(alarmobj.alarmTimePickerminute);
            }else{
                alarmTimePicker.setCurrentHour(alarmobj.alarmtimepickerhour);
                alarmTimePicker.setCurrentMinute(alarmobj.alarmTimePickerminute);

            }

        }


        Button okbutton = (Button)findViewById(R.id.setalarmactivityokbutton);
        okbutton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int hour;
                int minute;
                Intent returnIntent = new Intent();
                returnIntent.putExtra("Selectedcondition", Selectedcondition);
                returnIntent.putExtra("SelectedRepeatCondition", selectedRepeatDays);
                returnIntent.putExtra("Alarm Title", AlarmName);
                returnIntent.putExtra("ringtonepath", ringTonePath);

                /*Fill the selected time by the user*/
                if (Build.VERSION.SDK_INT >= 23) {
                    hour = alarmTimePicker.getHour();
                    minute = alarmTimePicker.getMinute();
                } else {
                    hour = alarmTimePicker.getCurrentHour();
                    minute = alarmTimePicker.getCurrentMinute();

                }

                returnIntent.putExtra("alamTimePickerhour", hour);
                returnIntent.putExtra("alamTimePickerminute", minute);
                returnIntent.putExtra("new alarm", newalarm);
                returnIntent.putExtra("Alarmobject", alarmobj);
                setResult(RESULT_OK, returnIntent);
                finish();


            }
        });

        Button cancelbutton = (Button)findViewById(R.id.setalarmactivitycancelbutton);
        cancelbutton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        Button previewbutton = (Button)findViewById(R.id.Previewalarm);
        previewbutton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), AlarmReceiverActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplicationContext().startActivity(i);


            }
        });




    }

    protected void onDestroy() {
        super.onDestroy();
        lv.setAdapter(null);
    }

    private void CreateRepeatDialog(View v ,int position) {
        AlertDialog dialog;
        final int listPosition = position;
        final ArrayList<Integer> mSelectedItems = new ArrayList();  // Where we track the selected items
        final String[] items={"Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"};
        final String[] items1={"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};

        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext(),R.style.MyAlertDialogTheme);
        LayoutInflater inflater = this.getLayoutInflater();

        View titleView = inflater.inflate(R.layout.repeatdialog_custom_title, null);
        builder.setCustomTitle(titleView)
       .setMultiChoiceItems(items, null,
               new DialogInterface.OnMultiChoiceClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which,
                                       boolean isChecked) {
                       if (isChecked) {
                           // If the user checked the item, add it to the selected items
                           mSelectedItems.add(which);
                       } else if (mSelectedItems.contains(which)) {
                           // Else, if the item is already in the array, remove it
                           mSelectedItems.remove(Integer.valueOf(which));
                       }
                   }
               })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        selectedRepeatDays = new ArrayList();

                        String s = "";
                        int itercount =0;

                        for(int a :mSelectedItems) {
                            if (itercount != 0) {
                                s += ",";
                            }
                            s += items1[a];
                            selectedRepeatDays.add(items1[a]);
                            itercount++;

                        }
                        HashMap<String, String> map1 = new HashMap<String, String>();
                        map1 = list.get(listPosition);
                        map1.put("text2", s);
                        list.set(listPosition, map1);
                        adapter.notifyDataSetChanged();


                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        dialog=builder.create();
        ListView listView= dialog.getListView();
        listView.setDivider(new ColorDrawable(Color.BLACK)); // set color
        listView.setDividerHeight(2); // set height

        dialog.show();

    }

    private void createAlarmNameDialog(View v ,int position) {
        final int listPosition = position;

        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
        builder.setTitle("Alarm Name");
        builder.setMessage("Enter name for Alarm");

        // Use an EditText view to get user input.
        final EditText input = new EditText(v.getContext());
        input.setId(TEXT_ID);
        builder.setView(input);

        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                AlarmName=input.getText().toString();
                HashMap<String,String> temp=new HashMap<String,String>();
                temp=list.get(listPosition);
                temp.put("text2",AlarmName);
                list.set(listPosition, temp);
                adapter.notifyDataSetChanged();

                return;
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                return;
            }
        });

        builder.create();
        builder.show();

    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        // Get the list item position
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        int position = info.position;

        if (v.getId()==R.id.setalarmlist) {

                if(position == 3) {
                int index =4;
                for (int i1 = 0; i1 < areaentries.size(); i1++) {
                    StringBuilder string= new StringBuilder();;
                    string.append(areaentries.get(i1).toString());
                    menu.add(FRAGMENT_GROUPID1, index++, Menu.NONE, "At " + string);
                    menu.add(FRAGMENT_GROUPID1, index, Menu.NONE, "Left "+string);
                }
                    menu.add(FRAGMENT_GROUPID1, index, Menu.NONE,"None");

            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int listPosition = info.position;

       if (item.getGroupId() == FRAGMENT_GROUPID1) {
            Selectedcondition= item.getTitle().toString();

            HashMap<String, String> map1 = new HashMap<String, String>();
            map1=list.get(listPosition);
            map1.put("text2",Selectedcondition);
            list.set(listPosition,map1);
            adapter.notifyDataSetChanged();

        }
        return super.onContextItemSelected(item);
    }

    private void startRingTonePicker(){
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select a new ringtone:");
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,RingtoneManager.TYPE_ALARM);
        startActivityForResult(intent, RQS_RINGTONEPICKER);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == RQS_RINGTONEPICKER && resultCode == RESULT_OK) {
            Selecteduri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            ringTone = RingtoneManager.getRingtone(this, Selecteduri);
            ringTonePath = Selecteduri.toString();

            HashMap<String,String> temp=new HashMap<String,String>();
            temp=list.get(2);
            temp.put("text2", ringTone.getTitle(this));
            list.set(2, temp);
            adapter.notifyDataSetChanged();

            Toast.makeText(this,
                    ringTone.getTitle(this),
                    Toast.LENGTH_LONG).show();
        }
    }



}


