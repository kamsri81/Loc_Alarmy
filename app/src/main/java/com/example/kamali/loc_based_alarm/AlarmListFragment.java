package com.example.kamali.loc_based_alarm;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import android.os.Handler;

/**
 * A placeholder fragment containing a simple view.
 */
public class AlarmListFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */

    private static MainActivity mInstance;
    private View rootView;
    private static AlarmManager alarmManager;
    static Uri Selecteduri = null;
    static MediaPlayer mp;
    static GetAreaDetails m_callback;
    ArrayList<String> areadetails;
    final int RQS_SETALARMACTIVITY = 1;
    static String Selectedcondition=null;
    static ArrayList <String> SelectedRepeatCondition;
    static String AlarmTitle=null;
    static int alarmtimepickerhour;
    static int alarmtimepickermin ;
    static String ringtonepath=null;
    private static ListView listview;

    static ArrayList<AlarmData> alarmlist ;
    static  AlarmListCustomAdapter adapter;

    private BroadcastReceiver mEnteredAreaReceiver;
    private BroadcastReceiver mLeftAreaReceiver;
    private static String CurrentAreaValue=null;
    private static Vibrator v;



    public AlarmListFragment() {
    }

    public static AlarmListFragment newInstance() {
        AlarmListFragment fragment = new AlarmListFragment();
        return fragment;
    }

    public void onAttach(Context context){
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            Activity activity = (Activity) context;
            m_callback = (GetAreaDetails)activity ;
        } catch (ClassCastException e) {
            throw new ClassCastException(((Activity) context).toString()
                    + " must implement GetAreaDetails");
        }
    }

    public interface GetAreaDetails{
        public void GetAreaDetails(ArrayList<String> areadetails);
    }


    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.alarmlistfragment, container, false);
        mInstance = (MainActivity) getActivity();

        listview = (ListView) rootView.findViewById(R.id.alarmlistview);
        alarmlist=new ArrayList <AlarmData>();
        adapter = new AlarmListCustomAdapter(getActivity().getBaseContext(),
                R.layout.custom_alarmlist_rowview,alarmlist);
        listview.setAdapter(adapter);

        final String[] items = {"Delete Alarm",
                "Duplicate Alarm",
                "Edit Alarm",
                "Preview Alarm"
        };

        // dialog list icons:
        final int[] icons = {
                R.drawable.ic_action_delete,
                R.drawable.ic_action_duplicate,
                R.drawable.ic_action_edit,
                R.drawable.ic_action_preview
        };

        final ListAdapter listaddadapter = new ArrayAdapter<String>(
                mInstance, R.layout.custom_alarmlist_additional, items) {

            ViewHolder holder;

            class ViewHolder {
                ImageView icon;
                TextView title;
            }

            public View getView(int position, View convertView, ViewGroup parent) {
                final LayoutInflater inflater = (LayoutInflater) mInstance
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                if (convertView == null) {
                    convertView = inflater.inflate(
                            R.layout.custom_alarmlist_additional, null);

                    holder = new ViewHolder();
                    holder.icon = (ImageView) convertView
                            .findViewById(R.id.icon);
                    holder.title = (TextView) convertView
                            .findViewById(R.id.title);
                    convertView.setTag(holder);
                } else {
                    holder = (ViewHolder) convertView.getTag();
                }

                holder.title.setText(items[position]);

                holder.icon.setImageResource(icons[position]);
                return convertView;
            }
        };

        listview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                final int listposition = pos;
                final AlarmListFragment.AlarmData alarmdata = alarmlist.get(listposition);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mInstance);
                alertDialogBuilder.setAdapter(listaddadapter, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: /*Delete Alarm*/
                                CancelAlarm(alarmdata);
                                alarmlist.remove(listposition);
                                adapter.notifyDataSetChanged();
                                dialog.cancel();
                                break;
                            case 1: /*Duplicate Alarm*/
                                alarmlist.add(alarmdata);
                                adapter.notifyDataSetChanged();
                                dialog.cancel();
                                break;
                            case 2: /*Edit Alarm*/
                                alarmdata.listposition = listposition;
                                Intent i = new Intent(mInstance, SetAlarmActivity.class);
                                areadetails = new ArrayList<String>();
                                m_callback.GetAreaDetails(areadetails);
                                i.putStringArrayListExtra("key", areadetails);
                                i.putExtra("new alarm", false);
                                i.putExtra("Alarmobject", alarmdata);
                                startActivityForResult(i, RQS_SETALARMACTIVITY);
                                break;
                            case 3:/*Preview Alarm*/
                                Intent j = new Intent(mInstance, AlarmReceiverActivity.class);
                                j.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                j.putExtra("Alarmobject", alarmdata);
                                mInstance.startActivity(j);
                                dialog.cancel();
                                break;
                            default:
                                break;
                        }
                    }
                });

                AlertDialog alertDialog = alertDialogBuilder.create();
                ListView listView = alertDialog.getListView();
                listView.setDivider(new ColorDrawable(Color.BLACK)); // set color
                listView.setDividerHeight(2); // set height
                alertDialog.getWindow().setLayout(600, 600); //Controlling width and height.
                alertDialog.show();
                return false;


            }

        });

        rootView.findViewById(R.id.add_alarm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mInstance, SetAlarmActivity.class);
                areadetails = new ArrayList<String>();
                m_callback.GetAreaDetails(areadetails);
                i.putStringArrayListExtra("key", areadetails);
                i.putExtra("new alarm",true);
                startActivityForResult(i, RQS_SETALARMACTIVITY);

            }
        });
        alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        return rootView;
    }

    public void onDestroy() {
        super.onDestroy();

        if(mp != null) {
            mp.stop();
            mp.release();
            mp=null;
        }

    }

    public static class AlarmData implements Parcelable {
        public String time;
        public int alarmtimepickerhour;
        public int alarmTimePickerminute;
        public String Selectedcondition;
        public ArrayList<String> SelectedRepeatedCondition;
        public String AlarmTitle;
        public String ringtonepath;
        public int alarmrequestcode;
        public int listposition;
        public boolean tooglealarmmanually;
        public AlarmData(String Selectedcondition,ArrayList<String>  SelectedRepeatedCondition,String time, int alarmtimepickerhour,int alamTimePickerminute ,String AlarmTitle,boolean togglealarmmanually,String ringtonepath) {
            this.Selectedcondition = Selectedcondition;
            this.SelectedRepeatedCondition = SelectedRepeatedCondition;
            this.time=time;
            this.alarmtimepickerhour=alarmtimepickerhour;
            this.alarmTimePickerminute=alamTimePickerminute;
            this.alarmrequestcode=0;
            this.AlarmTitle=AlarmTitle;
            this.tooglealarmmanually=togglealarmmanually;
            this.ringtonepath=ringtonepath;
        }

        public String gettime() {
            return time;
        }
        public String getSelectedcondition() {
            return Selectedcondition;
        }
        public ArrayList<String> getSelectedRepeatedcondition() {
            return SelectedRepeatedCondition;
        }

        protected AlarmData(Parcel in) {
            time = in.readString();
            Selectedcondition = in.readString();
            SelectedRepeatedCondition = in.readArrayList(null);
            AlarmTitle=in.readString();
            alarmrequestcode = in.readInt();
            listposition = in.readInt();
            alarmtimepickerhour=in.readInt();
            alarmTimePickerminute=in.readInt();
            tooglealarmmanually=in.readInt()==1;
            ringtonepath=in.readString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(time);
            dest.writeString(Selectedcondition);
            dest.writeList(SelectedRepeatedCondition);
            dest.writeString(AlarmTitle);
            dest.writeInt(alarmrequestcode);
            dest.writeInt(listposition);
            dest.writeInt(alarmtimepickerhour);
            dest.writeInt(alarmTimePickerminute);
            dest.writeInt(tooglealarmmanually ? 1 : 0);
            dest.writeString(ringtonepath);

        }
        public static final Parcelable.Creator<AlarmData> CREATOR = new Parcelable.Creator<AlarmData>() {
            @Override
            public AlarmData createFromParcel(Parcel in) {
                return new AlarmData(in);
            }

            @Override
            public AlarmData[] newArray(int size) {
                return new AlarmData[size];
            }
        };

    }

    public static void NotifyLocationChange(String text) {
        final String currenttext=text;

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                CurrentAreaValue = currenttext;
                String mymessage = String.format("CurrentAreaValue = %s ", CurrentAreaValue);
                Toast.makeText(mInstance, mymessage, Toast.LENGTH_SHORT).show();
            }
        }, 10000);

    }

    public static String getcurrentarea() {
        return CurrentAreaValue;
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {

         boolean new_alarm;
        if(requestCode == RQS_SETALARMACTIVITY && resultCode == Activity.RESULT_OK){
            Selectedcondition = data.getExtras().getString("Selectedcondition");
            SelectedRepeatCondition = data.getStringArrayListExtra("SelectedRepeatCondition");
            alarmtimepickerhour = data.getIntExtra("alamTimePickerhour", 0);
            alarmtimepickermin = data.getIntExtra("alamTimePickerminute", 0);
            AlarmTitle=data.getExtras().getString("Alarm Title");
            ringtonepath=data.getExtras().getString("ringtonepath");
            new_alarm=data.getExtras().getBoolean("new alarm");

            boolean isPM = (alarmtimepickerhour >= 12);
            String time =String.format("%02d:%02d %s", (alarmtimepickerhour == 12 || alarmtimepickerhour == 0) ? 12 : alarmtimepickerhour % 12, alarmtimepickermin, isPM ? "PM" : "AM");

            if(new_alarm == true) {
                AlarmData alarmobj = new AlarmData(Selectedcondition, SelectedRepeatCondition, time,alarmtimepickerhour, alarmtimepickermin,AlarmTitle,false,ringtonepath);
                alarmlist.add(alarmobj);
                adapter.notifyDataSetChanged();
            }else{

                AlarmListFragment.AlarmData alarmobj= data.getParcelableExtra("Alarmobject");
                /*Cancel the old alarm*/
                CancelAlarm(alarmobj);
                alarmobj.Selectedcondition=Selectedcondition;
                alarmobj.SelectedRepeatedCondition=SelectedRepeatCondition;
                alarmobj.time=time;
                alarmobj.AlarmTitle=AlarmTitle;
                alarmobj.alarmtimepickerhour=alarmtimepickerhour;
                alarmobj.alarmTimePickerminute=alarmtimepickermin;
                alarmobj.alarmrequestcode=0;
                alarmobj.ringtonepath=ringtonepath;
                alarmlist.get(alarmobj.listposition);
                alarmlist.set(alarmobj.listposition, alarmobj);
                adapter.notifyDataSetChanged();

            }

        }
    }

    public static void SetAlarmListToggleState(AlarmData alarmobj,boolean state){
        alarmlist.get(alarmobj.listposition);
        alarmlist.set(alarmobj.listposition, alarmobj);
        alarmobj.tooglealarmmanually=state;
        adapter.notifyDataSetChanged();

    }

    public static void setAlarm(AlarmData alarmobj ) {

        Calendar calNow = Calendar.getInstance();
        Calendar calSet = (Calendar) calNow.clone();

        if (Build.VERSION.SDK_INT >= 23) {
            calSet.set(Calendar.HOUR_OF_DAY, alarmobj.alarmtimepickerhour);
            calSet.set(Calendar.MINUTE, alarmobj.alarmTimePickerminute);
            calSet.set(Calendar.SECOND,00);
        } else {
            calSet.set(Calendar.HOUR_OF_DAY,  alarmobj.alarmtimepickerhour);
            calSet.set(Calendar.MINUTE,alarmobj.alarmTimePickerminute);
            calSet.set(Calendar.SECOND,00);
        }

            if (calSet.compareTo(calNow) <= 0) {
                //Today Set time passed, count to tomorrow
                calSet.add(Calendar.DATE, 1);
            }

        Intent myIntent = new Intent((MainActivity)mInstance, AlarmReceiver.class);
        int uniqueInt = (int) (System.currentTimeMillis() & 0xfffffff);
        alarmobj.alarmrequestcode=uniqueInt;
        myIntent.putExtra("Alarmobject", alarmobj);

        PendingIntent pendingIntent = PendingIntent.getBroadcast((MainActivity) mInstance, uniqueInt, myIntent,PendingIntent.FLAG_UPDATE_CURRENT);

        if(alarmobj.SelectedRepeatedCondition != null) {
            if(android.os.Build.VERSION.SDK_INT >= 19) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calSet.getTimeInMillis() , pendingIntent);

            }else {
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                        calSet.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
            }
        }else {
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calSet.getTimeInMillis(), pendingIntent);
            } else if(android.os.Build.VERSION.SDK_INT >= 19) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calSet.getTimeInMillis(), pendingIntent);
            }
            else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, calSet.getTimeInMillis(), pendingIntent);
            }

        }
    }

    public  static void CancelAlarm(AlarmData alarmobj) {
        Intent myIntent = new Intent((MainActivity)mInstance, AlarmReceiver.class);
        PendingIntent pendingIntent= PendingIntent.getBroadcast((MainActivity) mInstance, alarmobj.alarmrequestcode, myIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        if (alarmManager!= null) {
            alarmManager.cancel(pendingIntent);
        }

    }

    public static  void SnoozeAlarm(AlarmData alarmobj,int snoozetime) {

        Intent myIntent = new Intent(mInstance, AlarmReceiver.class);
        myIntent.putExtra("Alarmobject", alarmobj);
        PendingIntent pendingIntent= PendingIntent.getBroadcast((MainActivity) mInstance, alarmobj.alarmrequestcode, myIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + snoozetime * 60 * 1000, pendingIntent);
    }

    public static void playRingtone(AlarmData alarmobj) {

        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(mInstance);
        Boolean prefvibrate = sharedPrefs.getBoolean("prefvibrate", false);
        Float prefAlarmvolume = sharedPrefs.getFloat("seekBarPreference", 20);

        if( prefvibrate == true){
            /*Vibrate the phone*/
            v = (Vibrator)mInstance.getSystemService(Context.VIBRATOR_SERVICE);
            if (v.hasVibrator()) {
                Log.v("Can Vibrate", "YES");
            } else {
                Log.v("Can Vibrate", "NO");
            }
            if(v != null) {
                long pattern[] = { 0, 100, 200, 300, 400 };
                v.vibrate(pattern, 0);
            }
        }

        if(alarmobj.ringtonepath.equals("default")) {
            Uri alerttone = RingtoneManager.getActualDefaultRingtoneUri((MainActivity) mInstance,RingtoneManager.TYPE_ALARM);
            if (alerttone == null){
                alerttone = RingtoneManager.getActualDefaultRingtoneUri((MainActivity) mInstance,RingtoneManager.TYPE_RINGTONE);
            }
            mp = MediaPlayer.create(mInstance, alerttone);
            mp.setLooping(true);
            mp.start();
        } else {

            if (mp != null) {
                stopRingtone(alarmobj);
            }
            if (alarmobj.ringtonepath.startsWith("content://")) {
                try {
                    Uri uri = Uri.parse(alarmobj.ringtonepath);
                    alarmobj.ringtonepath = getRingtonePathFromContentUri(uri,mInstance.getContentResolver());
                } catch (Exception e) {
                }
            }

            try {
                mp = new MediaPlayer();
                mp.setLooping(true);
                mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mp.setDataSource(alarmobj.ringtonepath);
                mp.setVolume(prefAlarmvolume, prefAlarmvolume);
                mp.prepare();
                mp.start();

            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static void stopRingtone(AlarmData alarmobj) {

        if(mp != null) {
            mp.stop();
            mp.release();
            mp=null;
        }

        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(mInstance);
        Boolean prefvibrate = sharedPrefs.getBoolean("prefvibrate", false);
        Boolean prefDeleteAlarm = sharedPrefs.getBoolean("prefDeleteAlarm", false);

        if(prefvibrate == true) {
            if (v != null) {
                v.cancel();
            }
        }
        if (prefDeleteAlarm == true){
            /*Delete the alarm list*/
            CancelAlarm(alarmobj);
            alarmlist.remove(alarmobj.listposition);
            adapter.notifyDataSetChanged();

        }
    }
    private static String getRingtonePathFromContentUri(Uri selectedVideoUri,
                                                        ContentResolver contentResolver) {
        String filePath;
        String[] filePathColumn = {MediaStore.MediaColumns.DATA};

        Cursor cursor = contentResolver.query(selectedVideoUri, filePathColumn, null, null, null);
        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        filePath = cursor.getString(columnIndex);
        cursor.close();
        return filePath;
    }

}



