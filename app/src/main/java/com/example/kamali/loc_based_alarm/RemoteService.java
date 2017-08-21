package com.example.kamali.loc_based_alarm;

import android.Manifest;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
//import android.location.LocationListener;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class RemoteService extends Service implements LocationListener {

    static final int MSG_LEARN_LOCATION = 1;
    static final int MSG_GET_LOCATION_RESPONSE = 2;
    static final int MSG_GET_DEFAULT_HOME = 3;
    static final int MSG_LEFT_AREA = 4;
    static final int MSG_ENTERED_AREA = 5;
    static final int MSG_STOP_LEARNING = 6;
    static final int MSG_DELETE_AREA = 7;
    static final int MSG_NEW_CELLID = 8;
    static final int MSG_GETKEYMAPPINGS = 9;
    static final int MSG_KEYMAPPINGRESPONSE = 10;
    static final int MSG_ADD_AREA = 11;
    static final int MSG_ADD_CELLID_TO_AREA = 12;
    static final int MSG_ADD_CELLID_TO_AREA_RESPONSE = 13;
    static final int MSG_REMOVE_CELLID_FROM_AREA = 14;
    static final int MSG_REMOVE_CELLID_FROM_AREA_RESPONSE = 15;
    static final int MSG_ENABLE_GPS = 16;

    final Messenger myMessenger = new Messenger(new IncomingHandler());
    private Messenger mActivityMessenger = null;
    private Location location;
    private GsmCellLocation mycellLocation;
    private TelephonyManager telephonyManager;
    private LocationManager locationManager;
    private int mycellid;
    private String currentarea;
    private String prevarea = null;
    int preflocsettings;
    MyCellLocationChangeListener mycelllocationchangelistener = new MyCellLocationChangeListener();
    protected GoogleApiClient mGoogleApiClient;

    int startlearning = 0;
    static HashMap<String, ArrayList<String>> areadetails = new HashMap<String, ArrayList<String>>();
    static ArrayList<LocationData> Locationlist = new ArrayList<LocationData>();
    int useplacepickermap=1;

    public void onCreate() {
        super.onCreate();
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        preflocsettings = Integer.parseInt(sharedPrefs.getString("preflocationstrategy", "0"));

        if (preflocsettings == 1) {

            IntentFilter filter = new IntentFilter("com.example.kamali.loc_based_alarm.ACTION_RECEIVE_GEOFENCE");
            registerReceiver(receiver, filter);
            buildGoogleApiClient();

        }
        Log.v(" RemoteService", "onCreate called");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v("RemoteService", "onUnbind called");
        return false;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        mActivityMessenger = arg0.getParcelableExtra("Messenger");
        return myMessenger.getBinder();
    }

    public void onDestroy() {
        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        preflocsettings = Integer.parseInt(sharedPrefs.getString("preflocationstrategy", "0"));

        if (preflocsettings != 1) {
            telephonyManager.listen(mycelllocationchangelistener, PhoneStateListener.LISTEN_NONE);
        } else {

            /*Disconnect the google api client and unregister the geofence receiver*/
            if (mGoogleApiClient != null && (mGoogleApiClient.isConnected() || mGoogleApiClient.isConnecting())) {
                mGoogleApiClient.disconnect();
            }
            if(receiver != null)
            unregisterReceiver(receiver);
        }

    }

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            int respCode = msg.what;

            switch (respCode) {
                case RemoteService.MSG_LEARN_LOCATION: {
                    currentarea = msg.getData().getString("Area");
                    StartLearningArea(currentarea);
                }
                break;

                case RemoteService.MSG_STOP_LEARNING: {
                    currentarea = msg.getData().getString("Area");
                    Double latitude=msg.getData().getDouble("Latitude");
                    Double longitude=msg.getData().getDouble("Longitude");
                    StopLearningArea(currentarea,latitude,longitude);

                }
                break;
                case RemoteService.MSG_DELETE_AREA: {
                    currentarea = msg.getData().getString("Area");
                    DeleteAreaFromMap(currentarea);

                }

                break;

                case RemoteService.MSG_ADD_AREA: {
                    String area = msg.getData().getString("Area");
                    String cellid = msg.getData().getString("cellid");
                    AddtoList(cellid, area);

                }
                break;
                case RemoteService.MSG_ADD_CELLID_TO_AREA: {
                    String area = msg.getData().getString("Area");
                    String cellid = msg.getData().getString("cellid");
                    AddtoList(cellid, area);

                    Message resp = Message.obtain(null, MSG_ADD_CELLID_TO_AREA_RESPONSE);
                    Bundle bResp = new Bundle();
                    ArrayList<String> itemsList = areadetails.get(area);
                    bResp.putStringArrayList("respData", itemsList);
                    bResp.putString("Area", area);
                    resp.setData(bResp);
                    try {

                        mActivityMessenger.send(resp);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                }
                break;

                case RemoteService.MSG_REMOVE_CELLID_FROM_AREA: {
                    String area = msg.getData().getString("Area");
                    String cellid = msg.getData().getString("cellid");
                    DeleteFromList(cellid, area);
                    Message resp = Message.obtain(null, MSG_REMOVE_CELLID_FROM_AREA_RESPONSE);
                    Bundle bResp = new Bundle();
                    ArrayList<String> itemsList = areadetails.get(area);
                    bResp.putStringArrayList("respData", itemsList);
                    bResp.putString("Area", area);
                    resp.setData(bResp);
                    try {

                        mActivityMessenger.send(resp);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                break;
            }//switch
        }
    }


    public void StartLearningArea(String area) {
        startlearning = 1;

        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        preflocsettings = Integer.parseInt(sharedPrefs.getString("preflocationstrategy", "0"));

        if (preflocsettings != 1) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            telephonyManager.listen(mycelllocationchangelistener, PhoneStateListener.LISTEN_CELL_LOCATION);

        }

    }

    public void StopLearningArea(String area,double latitude,double longitude) {
        startlearning = 0;
        Message resp = Message.obtain(null, MSG_GET_LOCATION_RESPONSE);
        Bundle bResp = new Bundle();
        if(preflocsettings == 1){
            LocationData locationdata=null;
            String mymessage = String.format("%f,%f",latitude, longitude);

            if (Locationlist != null) {
                if(Locationlist.size()==0){
                    locationdata = new LocationData(area, latitude,longitude, 0);
                    locationdata.Latlongmessage = mymessage;
                    Locationlist.add(locationdata);

                }else {
                    for (int i = 0; i < Locationlist.size(); i++) {
                        locationdata = Locationlist.get(i);
                        if ((locationdata.Key).equals(area)) {
                            removeGeoFences(locationdata);
                            locationdata.Latitude = latitude;
                            locationdata.Longitude = longitude;
                            locationdata.Latlongmessage = mymessage;
                            break;
                        } else {
                            locationdata = new LocationData(area,latitude, longitude, 0);
                            locationdata.Latlongmessage = mymessage;
                            Locationlist.add(locationdata);
                            break;
                        }
                    }
                }
                createGeofences(locationdata);
                ArrayList<String> itemsList = itemsList = new ArrayList<String>();
                itemsList.add(locationdata.Latlongmessage);
                bResp.putStringArrayList("respData", itemsList);
            }//if (Locationlist != null)
        }else { //if(preflocsettings == 1)

            ArrayList<String> itemsList = areadetails.get(area);
            bResp.putStringArrayList("respData", itemsList);
        }
        bResp.putString("Area", area);
        resp.setData(bResp);
        try {

            mActivityMessenger.send(resp);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    public void DeleteAreaFromMap(String area) {
        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        preflocsettings = Integer.parseInt(sharedPrefs.getString("preflocationstrategy", "0"));
        if (preflocsettings == 1) {
            for (int i = 0; i < Locationlist.size(); i++) {
                LocationData locationdata = Locationlist.get(i);
                if ((locationdata.Key).equals(area)) {
                    removeGeoFences(locationdata);
                    Locationlist.remove(i);
                }
            }

        }else {

            if (areadetails != null) {
                areadetails.remove(area);

            }
        }

    }

    public void AddtoList(String item, String area) {

        ArrayList<String> itemsList = areadetails.get(area);

        // if list does not exist create it
        if (itemsList == null) {
            itemsList = new ArrayList<String>();
            itemsList.add(item);
            areadetails.put(area, itemsList);
        } else {
            // add if item is not already in list
            if (!itemsList.contains(item)) itemsList.add(item);
        }

    }


    public void DeleteFromList(String item, String area) {

        ArrayList<String> itemsList = areadetails.get(area);
        Iterator<String> it = itemsList.iterator();
        while (it.hasNext()) {
            String cellid = it.next();
            if (cellid.equals(item)) {
                it.remove();
            }
        }
        areadetails.put(area, itemsList);


    }

    public String getkeyfromcellid(int gsmcid) {
        String Area = "not mapped to any area";
        for (Map.Entry<String, ArrayList<String>> entry : areadetails.entrySet()) {
            ArrayList<String> v = entry.getValue();
            if (v.contains(String.valueOf(gsmcid))) {
                Area = entry.getKey();

            }
        }
        return Area;
    }


    public String getkeyfromlocation(String location) {
        String Area = "not mapped to any area";
        for (Map.Entry<String, ArrayList<String>> entry : areadetails.entrySet()) {
            ArrayList<String> v = entry.getValue();
            if (v.contains(location)) {
                Area = entry.getKey();

            }
        }
        return Area;
    }


    public String getCurrentTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat mdformat = new SimpleDateFormat("HH:mm:ss a");
        String strDate = "Time: " + mdformat.format(calendar.getTime());
        return strDate;

    }

    private void SendMessagetoClient(String data, String data1, String data2, int messageid) {
        Message resp = Message.obtain(null, messageid);
        Bundle bResp = new Bundle();
        bResp.putString("data", data);
        bResp.putString("data1", data1);
        bResp.putString("data2", data2);
        resp.setData(bResp);
        try {

            mActivityMessenger.send(resp);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    private void GetCellLocation() {
        try {
            mycellLocation = (GsmCellLocation) telephonyManager.getCellLocation();
            mycellid = mycellLocation.getCid();
            Toast.makeText(getApplicationContext(), mycellid, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class LocationData {
        public String Key;
        public double Latitude;
        public double Longitude;
        public int requestcode;
        String Latlongmessage;

        public LocationData(String Key, double Latitude, double Longitude, int requestcode) {
            this.Key = Key;
            this.Latitude = Latitude;
            this.Longitude = Longitude;
            this.requestcode = requestcode;

        }

        public String getkey() {
            return Key;
        }

        public String getLatitude() {
            return Double.toString(Latitude);
        }

        public String getLongitude() {
            return Double.toString(Longitude);
        }

    }

    public class MyCellLocationChangeListener extends PhoneStateListener {
        public void onCellLocationChanged(CellLocation location) {
            super.onCellLocationChanged(location);

            int gsmcid = 0;
            if (location != null) {

                if (location instanceof GsmCellLocation) {
                    GsmCellLocation gcLoc = (GsmCellLocation) location;
                    gsmcid = gcLoc.getCid();
                }

                String key = getkeyfromcellid(gsmcid);
                String strDate = getCurrentTime();
                SendMessagetoClient(strDate, String.valueOf(gsmcid), key, MSG_NEW_CELLID);

                if (startlearning == 1) {
                    AddtoList(String.valueOf(gsmcid), currentarea);
                    return;
                } else {

                    if (areadetails.isEmpty() != true) {
                        String Area = getkeyfromcellid(gsmcid);
                        if (Area.equals("not mapped to any area") == true) {
                            SendMessagetoClient(currentarea, null, null, MSG_LEFT_AREA);
                        } else {
                            currentarea = Area;
                            if (currentarea != null) {
                                SendMessagetoClient(Area, null, null, MSG_ENTERED_AREA);

                            }
                        }
                    }//areadetaial.isEmpty() !=true
                }//else(start_learning ==1)
            }//(location !=null)
            super.onCellLocationChanged(location);
        }

    }

    /*google play services api*/

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(connectionAddListener)
                .addOnConnectionFailedListener(connectionFailedListener)
                .addApi(LocationServices.API)
                .build();
        if (!mGoogleApiClient.isConnecting() || !mGoogleApiClient.isConnected()) {
            if (mGoogleApiClient != null) {
                mGoogleApiClient.connect();
            }
        }
    }

    private GoogleApiClient.ConnectionCallbacks connectionAddListener =
            new GoogleApiClient.ConnectionCallbacks() {
                @Override
                public void onConnected(Bundle bundle) {
                }

                @Override
                public void onConnectionSuspended(int i) {
                }
            };

    private GoogleApiClient.OnConnectionFailedListener connectionFailedListener =
            new GoogleApiClient.OnConnectionFailedListener() {
                @Override
                public void onConnectionFailed(ConnectionResult connectionResult) {

                }
            };

    private void getlocation() {

        LocationRequest mLocationRequest;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000)        // 10 seconds, in milliseconds
                .setFastestInterval(2000); // 1 second, in milliseconds

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    public void onLocationChanged(Location location) {


    }

    public void onStatusChanged(String s, int i, Bundle bundle) {
    }


    public void onProviderEnabled(String s) {
    }


    public void onProviderDisabled(String s) {
    }

    public void createGeofences(LocationData locationdata) {
        Geofence fence = null;
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, "Google API Client not connected!", Toast.LENGTH_SHORT).show();
            return;
        }
        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        int arearadius = Integer.parseInt(sharedPrefs.getString("prefarearadius", "1"));

        try {
            String id = UUID.randomUUID().toString();
            fence = new Geofence.Builder()
                    .setRequestId(id)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .setCircularRegion(locationdata.Latitude, locationdata.Longitude, arearadius) // Try changing your radius
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .build();
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
        }


        try {
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    getGeofencingRequest(fence),
                    getGeofencePendingIntent(locationdata)
            );
        } catch (SecurityException securityException) {
        }
       //getlocation();
    }

    private void removeGeoFences(LocationData locationdata) {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            try {
                Intent intent = new Intent("com.example.kamali.loc_based_alarm.ACTION_RECEIVE_GEOFENCE");
                PendingIntent geoFencePendingIntent = PendingIntent.getBroadcast(this, locationdata.requestcode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                LocationServices.GeofencingApi.removeGeofences(
                        mGoogleApiClient,
                        geoFencePendingIntent
                );
            } catch (SecurityException securityException) {

            }
        }
    }

    private GeofencingRequest getGeofencingRequest(Geofence fence) {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER );
        builder.addGeofence(fence);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent(LocationData locationdata) {
        Intent intent = new Intent("com.example.kamali.loc_based_alarm.ACTION_RECEIVE_GEOFENCE");
        intent.putExtra("name", locationdata.getkey());
        int uniqueInt = (int) (System.currentTimeMillis() & 0xfffffff);
        PendingIntent geoFencePendingIntent = PendingIntent.getBroadcast(this, uniqueInt, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        locationdata.requestcode=uniqueInt;
        return geoFencePendingIntent;

    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            GeofencingEvent event = GeofencingEvent.fromIntent(intent);
            if (event != null) {
                if (event.hasError()) {
                    int errorCode=event.getErrorCode();
                    Log.e("LocaAlarmy", "Location Services error: " + errorCode);
                } else {
                    String area = intent.getExtras().getString("name");
                    int transition = event.getGeofenceTransition();
                    if (transition == Geofence.GEOFENCE_TRANSITION_ENTER ) {
                        SendMessagetoClient(area, null, null, MSG_ENTERED_AREA);
                    } else if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                        SendMessagetoClient(area, null, null, MSG_LEFT_AREA);
                    }
                }

            }

        }
    };

}





