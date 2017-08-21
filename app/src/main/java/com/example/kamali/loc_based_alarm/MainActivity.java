package com.example.kamali.loc_based_alarm;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements AlarmListFragment.GetAreaDetails{

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private Messenger mServiceMessenger = null;
    private Messenger mActivityMessenger =null;
    private boolean isBound;
    private static int Listposition;
    String AreatoLearn=null;
    final int RQS_APPSETTINGSACTIVITY = 2;

    int prefsnoozeduration =5 ;
    Boolean prefvibrate = false ;
    Boolean prefDeleteAlarm =false;
    int preftaskreminder =0;
    float Alarmvolume;
    public static Activity activity = null;
    CountDownTimer CountDownTimer=null;
    int PLACE_PICKER_REQUEST = 1;
    int useplacepickermap=1;



    private ServiceConnection myConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mServiceMessenger = new Messenger(service);
            isBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            mServiceMessenger = null;
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        activity = this;

        // Create the adapter that will return a fragment for each of the four
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(5);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        /*Check for permission from the user*/
        permissioncheck();

        PreferenceManager.setDefaultValues(this, R.xml.appsetting_xml, true);


    }


    public void permissioncheck(){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
             ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number can be used
         }
        else{

            /*BInd to location service*/
            mActivityMessenger = new Messenger(new ResponseHandler());
            Intent intent = new Intent("com.example.RemoteService");
            intent.setPackage("com.example.kamali.loc_based_alarm");
            intent.putExtra("Messenger", mActivityMessenger);
            bindService(intent, myConnection, Context.BIND_AUTO_CREATE);

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1001: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {


                }
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        /*Start the user settings screen*/
        if (id == R.id.action_settings) {
            Intent i = new Intent(getApplicationContext(), AppSettingsActivity.class);
            startActivityForResult(i, RQS_APPSETTINGSACTIVITY);


            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RQS_APPSETTINGSACTIVITY:
               // showUserSettings();
                break;

        }

    }



    public static  void restartapp(Context context){
        Intent restartIntent = context.getApplicationContext().getPackageManager()
                .getLaunchIntentForPackage(context.getApplicationContext().getPackageName());
        PendingIntent intent = PendingIntent.getActivity(context.getApplicationContext(), 0, restartIntent, Intent.FLAG_ACTIVITY_CLEAR_TOP);
        AlarmManager manager = (AlarmManager) context.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        manager.set(AlarmManager.RTC, System.currentTimeMillis() + 1, intent);
        activity.finish();
        System.exit(2);



    }

    private void showUserSettings() {
        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);

        String prefLocationStrategy = sharedPrefs.getString("preflocationstrategy", "0");

        prefsnoozeduration =Integer.parseInt(sharedPrefs.getString("prefsnoozeduration", "5"));

        prefvibrate = sharedPrefs.getBoolean("prefvibrate", false);
        prefDeleteAlarm = sharedPrefs.getBoolean("prefDeleteAlarm", false);
        preftaskreminder=Integer.parseInt(sharedPrefs.getString("prefTaskReminder", "0"));
        Alarmvolume = sharedPrefs.getFloat("seekBarPreference", 20);
        int Arearadius = Integer.parseInt(sharedPrefs.getString("prefarearadius", "1"));

        StringBuilder builder = new StringBuilder();
        builder.append("Location Strategy " + prefLocationStrategy);
        builder.append("snooze duration: " + prefsnoozeduration + "\n");
        builder.append("vibrate phone: " + prefvibrate + "\n");
        builder.append("Delete Alarm: " + prefDeleteAlarm + "\n");
        builder.append("call task reminder repeatedly " + preftaskreminder +"\n");
        builder.append("Alarm volume " + Alarmvolume+"\n");
        builder.append("Arearadius" + Arearadius);

        Toast.makeText(MainActivity.this,builder,Toast.LENGTH_LONG).show();

    }

    protected void onDestroy() {
        super.onDestroy();
        unbindService(myConnection);
    }


    public void GetAreaDetails(ArrayList<String> areadetails){
        AddAreaFragment.GetAreaDetails(areadetails);

    }
    // class that handles the Service response
    private class ResponseHandler extends Handler {

        String result =null;
        String Arearesult=null;

        @Override
        public void handleMessage(Message msg) {
            int respCode = msg.what;

            switch (respCode) {
                case RemoteService.MSG_GET_LOCATION_RESPONSE:
                    ArrayList respData = msg.getData().getStringArrayList("respData");
                    if(respData.size()== 0){
                        showfailedlearningAlert();
                    }else {
                        Arearesult = msg.getData().getString("Area");
                        SharedPreferences sharedPrefs = PreferenceManager
                                .getDefaultSharedPreferences(getApplicationContext());
                        int preflocsettings = Integer.parseInt(sharedPrefs.getString("preflocationstrategy", "0"));
                        if (preflocsettings == 0) {
                            AddAreaFragment.setcellid(respData, Listposition, null);
                            AlarmListFragment.NotifyLocationChange("At " + Arearesult);
                            TaskReminderFragment.NotifyLocationChange("Entered " + Arearesult);
                        } else {
                            AddAreaFragment.setlatlong(respData, -1, Arearesult);
                        }
                    }
                    break;
                case RemoteService.MSG_ENTERED_AREA:
                    //Toast.makeText(getApplicationContext(), "MSG_ENTERED_AREA from mainactiviy", Toast.LENGTH_SHORT).show();
                    result = msg.getData().getString("data");
                    AlarmListFragment.NotifyLocationChange("At " + result);
                    TaskReminderFragment.NotifyLocationChange("Entered " + result);
                    break;
                case RemoteService.MSG_LEFT_AREA:
                  // Toast.makeText(getApplicationContext(),"MSG_LEFT_AREA from mainactiviy", Toast.LENGTH_SHORT).show();
                    result = msg.getData().getString("data");
                    AlarmListFragment.NotifyLocationChange("Left " + result);
                    TaskReminderFragment.NotifyLocationChange("Left " + result);
                    break;
                case RemoteService.MSG_NEW_CELLID:
                    String date = msg.getData().getString("data");
                    String cellid = msg.getData().getString("data1");
                    String key= msg.getData().getString("data2");
                    RecentcellidFragment.SetListItem(date, cellid, key);
                    break;
                case RemoteService.MSG_ADD_CELLID_TO_AREA_RESPONSE:
                    ArrayList respData1 = msg.getData().getStringArrayList("respData");
                    Arearesult = msg.getData().getString("Area");
                    AddAreaFragment.setcellid(respData1, -1, Arearesult);
                    break;

                case RemoteService.MSG_REMOVE_CELLID_FROM_AREA_RESPONSE:
                    ArrayList respData2 = msg.getData().getStringArrayList("respData");
                    Arearesult = msg.getData().getString("Area");
                    AddAreaFragment.setcellid(respData2,-1,Arearesult);
                    break;
                case RemoteService.MSG_ENABLE_GPS:
                    showlocationsettingsAlert();

                default:
                    super.handleMessage(msg);

            }
        }
    }

    public  void showlocationsettingsAlert() {
        CountDownTimer.cancel();
        AddAreaFragment.Deletelearningtime(0, Listposition);
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("Enable Location")
                .setMessage(("Your GPS Settings is set to 'Off'.\nPlease enable GPS or select Network cellid " +
                        "under settings." ))
                .setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    }
                })
                .create();
        alertDialog.show();
    }

    private void showfailedlearningAlert() {
        AlertDialog alertDialog = new  AlertDialog.Builder(this)
                .setTitle("Location learning failed")
                .setMessage("There was some problem learning the area!\n\n")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    }
                })
                .create();
        alertDialog.show();
        AddAreaFragment.Deletelearningtime(0, Listposition);
    }

    /*Interface functions to the remote service*/

    public void StartLearning(String Area ,int position,int learningduration ){
        if (!isBound) return;
        Listposition=position;
        AreatoLearn=Area;

            CountDownTimer = new CountDownTimer(learningduration, 1000) {
                boolean firstTime = true;

                public void onTick(long millisUntilFinished) {
                    if (firstTime) {

                        Message msg = Message
                                .obtain(null, RemoteService.MSG_LEARN_LOCATION);
                        Bundle b = new Bundle();
                        b.putString("Area", AreatoLearn);
                        msg.setData(b);
                        try {

                            mServiceMessenger.send(msg);
                            firstTime = false;
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }

                    AddAreaFragment.updatelearningtime(millisUntilFinished / 1000, Listposition);
                }

                public void onFinish() {
                    StopLearning(AreatoLearn,0,0);
                }
            }.start();

    }

    public void StopLearning(String Area,double latitude,double longitude){
        Message msg = Message
                .obtain(null, RemoteService.MSG_STOP_LEARNING);
        msg.replyTo = new Messenger(new ResponseHandler());
        Bundle b = new Bundle();
        b.putString("Area",Area);
        b.putDouble("Latitude", latitude);
        b.putDouble("Longitude",longitude);
        msg.setData(b);
        try {

            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }
    public void DeleteArea(String Area) {
        Message msg = Message
                .obtain(null, RemoteService.MSG_DELETE_AREA);
        Bundle b = new Bundle();
        b.putString("Area",Area);
        msg.setData(b);
        try {

            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    public void AddNewArea(String Area,String cellid){
        Message msg = Message
                .obtain(null, RemoteService.MSG_ADD_AREA);
        Bundle b = new Bundle();
        b.putString("Area",Area);
        b.putString("cellid",cellid);
        msg.setData(b);
        try {

            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    public void AddCellidToArea(String Area,String cellid){
        Message msg = Message
                .obtain(null, RemoteService.MSG_ADD_CELLID_TO_AREA);
        Bundle b = new Bundle();
        b.putString("Area",Area);
        b.putString("cellid",cellid);
        msg.setData(b);
        try {

            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }


    public void RemoveCellidFromArea(String Area,String cellid){
        Message msg = Message
                .obtain(null, RemoteService.MSG_REMOVE_CELLID_FROM_AREA);
        Bundle b = new Bundle();
        b.putString("Area",Area);
        b.putString("cellid",cellid);
        msg.setData(b);
        try {

            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }


    /**
     * A FragmentPagerAdapter that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch(position){
                case 0: return AddAreaFragment.newInstance();
                case 1: return  AlarmListFragment.newInstance();
                case 2: return TaskReminderFragment.newInstance();
                case 3: return RecentcellidFragment.newInstance();

            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 4 total pages.
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "AREAS";
                case 1:
                    return "ALARMS";
                case 2:
                    return "TASKS";
                case 3:
                    return "RECENT";

            }
            return null;
        }
    }
}
