 package com.example.kamali.loc_based_alarm;



 import android.content.Context;
 import android.content.DialogInterface;
 import android.media.AudioManager;
 import android.media.MediaPlayer;
 import android.media.RingtoneManager;
 import android.net.Uri;
 import android.util.AttributeSet;
 import android.view.LayoutInflater;
 import android.view.View;
 import android.preference.DialogPreference;
 import android.widget.SeekBar;
 import android.widget.TextView;
 import android.widget.SeekBar.OnSeekBarChangeListener;


 public class appsettings_seekbarpreference extends DialogPreference implements OnSeekBarChangeListener {

     // Namespaces to read attributes
     private static final String PREFERENCE_NS = "http://schemas.android.com/apk/res/com.mnm.seekbarpreference";
     private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

     // Attribute names
     private static final String ATTR_DEFAULT_VALUE = "defaultValue";
     private static final String ATTR_MIN_VALUE = "minValue";
     private static final String ATTR_MAX_VALUE = "maxValue";

     // Default values for defaults
     private static final int DEFAULT_CURRENT_VALUE = 50;
     private static final int DEFAULT_MIN_VALUE = 0;
     private static final int DEFAULT_MAX_VALUE = 100;

     // Real defaults
     private final int mDefaultValue;
     private final int mMaxValue;
     private final int mMinValue;

     // Current value
     private int mCurrentValue;

     // View elements
     private SeekBar mSeekBar;
     private TextView mValueText;

     MediaPlayer mp;
     AudioManager audioManager;
      int initVolume;
     private Context mContext;
     float SelectedAlarmvolume;

     public appsettings_seekbarpreference(Context context, AttributeSet attrs) {
         super(context, attrs);

         // Read parameters from attributes
         mMinValue = attrs.getAttributeIntValue(PREFERENCE_NS, ATTR_MIN_VALUE, DEFAULT_MIN_VALUE);
         mMaxValue = attrs.getAttributeIntValue(PREFERENCE_NS, ATTR_MAX_VALUE, DEFAULT_MAX_VALUE);
         mDefaultValue = attrs.getAttributeIntValue(ANDROID_NS, ATTR_DEFAULT_VALUE, DEFAULT_CURRENT_VALUE);
         mContext=context;
     }

     @Override
     protected View onCreateDialogView() {
         mCurrentValue = mDefaultValue;

         LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
         View view = inflater.inflate(R.layout.appsettings_sliderdialog, null);

         // Setup SeekBar
         mSeekBar = (SeekBar) view.findViewById(R.id.seek_bar);
         mSeekBar.setMax(mMaxValue - mMinValue);
         mSeekBar.setProgress(mCurrentValue - mMinValue);
         mSeekBar.setOnSeekBarChangeListener(this);

         // Setup text label for current value
         mValueText = (TextView) view.findViewById(R.id.current_value);
         mValueText.setText(Integer.toString(mCurrentValue));

         Uri alerttone = RingtoneManager.getActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_ALARM);
         if (alerttone == null){
             alerttone = RingtoneManager.getActualDefaultRingtoneUri( mContext,RingtoneManager.TYPE_RINGTONE);
         }
         mp = MediaPlayer.create(mContext, alerttone);
         audioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
         initVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
         final int maxVolume  = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
         audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,maxVolume, 0);

         return view;
     }

     @Override
     public void onDismiss(DialogInterface dialog)
     {
         audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, initVolume, 0);
         if(mp != null) {
             mp.stop();
             mp.release();
             mp=null;
         }

         if (shouldPersist()) {
             persistFloat(SelectedAlarmvolume);
         }
     }

     @Override
     protected void onDialogClosed(boolean positiveResult) {
         super.onDialogClosed(positiveResult);

         if (!positiveResult) {
             return;
         }
         if (shouldPersist()) {
             persistFloat(SelectedAlarmvolume);
         }

         notifyChanged();
     }


     public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
         // Update current value
         mCurrentValue = value + mMinValue;
         // Update label with current value
         mValueText.setText(Integer.toString(mCurrentValue));

         SelectedAlarmvolume= (float) (1 - (Math.log(100 - value) / Math.log(100)));

         if(mp!= null) {
             mp.setVolume(SelectedAlarmvolume, SelectedAlarmvolume);
             mp.start();
         }
     }

     public void onStartTrackingTouch(SeekBar seek) {
         // Not used
     }

     public void onStopTrackingTouch(SeekBar seek) {
         // Not used
     }

 }