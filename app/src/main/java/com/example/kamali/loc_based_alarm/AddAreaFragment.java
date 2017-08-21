package com.example.kamali.loc_based_alarm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A placeholder fragment containing a simple view.
 */
public class AddAreaFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final int TEXT_ID = 0;
    static final int FRAGMENT_GROUPID = 30;
    static final int MENU_LEARNAREA = 1;
    static final int MENU_RENAMEAREA = 2;
    static final int MENU_DELETEAREA = 3;
    private static MainActivity mInstance;

    static ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
    static SimpleAdapter adapter;
    int itemclicked;
    HashMap<String,String> init_listitem=new HashMap<String,String>();
    int PLACE_PICKER_REQUEST = 1;
    int useplacepickermap=1;
    static String SelectedArea;

    public AddAreaFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static AddAreaFragment newInstance() {
        AddAreaFragment fragment = new AddAreaFragment();
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.addareafragment, container, false);
        mInstance = (MainActivity) getActivity();
        Button NewAreaButton = (Button)rootView.findViewById(R.id.button);
        ListView lv = (ListView) rootView.findViewById(R.id.list);
        adapter = new SimpleAdapter(getActivity().getBaseContext(),list,
                R.layout.custom_row_view,
                new String[] {"Area","cellid"},
                new int[] {R.id.text1,R.id.text2}

        );
        lv.setAdapter(adapter);

        if(list.isEmpty()!= true) {

            init_listitem = list.get(0);
            init_listitem.put("Area", "Home");
            list.set(0, init_listitem);
            adapter.notifyDataSetChanged();
        }else {
            init_listitem.put("Area", "Home");
            list.add(init_listitem);
            adapter.notifyDataSetChanged();
        }

        NewAreaButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                createAreaDialog(v);

            }
        });
        registerForContextMenu(lv);

        return rootView;
    }


    public void onDestroy() {
        super.onDestroy();

    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId()==R.id.list) {
            menu.add(FRAGMENT_GROUPID,  MENU_LEARNAREA, Menu.NONE, "LearnArea");
            menu.add(FRAGMENT_GROUPID, MENU_DELETEAREA, Menu.NONE, "DeleteArea");
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getGroupId() == FRAGMENT_GROUPID) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            switch (item.getItemId()) {
                case  MENU_LEARNAREA:
                    SharedPreferences sharedPrefs = PreferenceManager
                            .getDefaultSharedPreferences(mInstance);
                    int preflocsettings = Integer.parseInt(sharedPrefs.getString("preflocationstrategy", "0"));

                    if(preflocsettings ==1 ) {
                        try {

                            Intent intent =new PlacePicker.IntentBuilder().build(getActivity());
                             SelectedArea=list.get(info.position).get("Area");
                            startActivityForResult(intent, PLACE_PICKER_REQUEST);

                        } catch (GooglePlayServicesRepairableException
                                | GooglePlayServicesNotAvailableException e) {
                            e.printStackTrace();
                        }
                    }
                    else {

                        Dialog dialog = onCreateDialogSingleChoice(info);
                        dialog.show();
                    }
                    return true;
                case MENU_DELETEAREA:
                    ((MainActivity) getActivity()).DeleteArea(list.get(info.position).get("Area"));
                    list.remove(info.position);
                    adapter.notifyDataSetChanged();
                    return true;
                default:
                    return super.onContextItemSelected(item);
            }
        }
        return super.onContextItemSelected(item);
    }


    public Dialog onCreateDialogSingleChoice(final AdapterView.AdapterContextMenuInfo info) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final CharSequence[] array = {"30 sec" ,"1 min" ,"2 min"};
        builder.setTitle("Select Learning Time")
        .setSingleChoiceItems(array, -1, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch(which){
                    case 0:
                        itemclicked = 30000;
                        break;
                    case 1:
                        itemclicked = 60000;
                        break;
                    case 2:
                        itemclicked = 120000;
                        break;
                    default:
                        itemclicked= 60000;
                        break;
                }

            }

        })
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        ((MainActivity) getActivity()).StartLearning(list.get(info.position).get("Area"), info.position,itemclicked);

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();

                    }
                });

        return builder.create();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if( requestCode == PLACE_PICKER_REQUEST ) {
            String CurrentArea=SelectedArea;
            Place place =   PlacePicker.getPlace(data,getActivity());
            Double latitude = place.getLatLng().latitude;
            Double longitude = place.getLatLng().longitude;
            String address = String.valueOf(latitude) + String.valueOf(longitude);
            ((MainActivity) getActivity()).StopLearning(CurrentArea, latitude, longitude);
        }


    }


    static void updatelearningtime(long updatetime,int listposition){

        HashMap<String,String> tempnew=new HashMap<String,String>();
        tempnew=list.get(listposition);
        tempnew.put("cellid", "LEARNING :seconds remaining: " + updatetime);
        list.set(listposition,tempnew);
        adapter.notifyDataSetChanged();

    }


    static void Deletelearningtime(long updatetime,int listposition){

        HashMap<String,String> tempnew=new HashMap<String,String>();
        tempnew=list.get(listposition);
        tempnew.put("cellid", "");
        list.set(listposition,tempnew);
        adapter.notifyDataSetChanged();

    }

    static void setcellid(ArrayList cellids,int position,String Area){

        Iterator<String> it = cellids.iterator();
        HashMap<String,String> tempnew=new HashMap<String,String>();
        String StringtoDisplay;
        if(cellids.size()==0){
            StringtoDisplay="";

        }else {
           StringtoDisplay = "Cellid: ";
        }
        int itercount=0;
        while (it.hasNext()) {
            if(itercount!=0)
            StringtoDisplay +=",";
            StringtoDisplay += it.next();
            itercount++;
        }
        if(cellids.size()==0){
            StringtoDisplay="";

        }
        if(position == -1 && Area != null){
            for(int i=0;i< list.size();i++){
                HashMap<String, String> map =list.get(i);
                for (String key : map.keySet()) {
                    if(map.get(key).equals(Area)){
                       tempnew=list.get(i);
                        tempnew.put("cellid",  StringtoDisplay);
                        list.set(i,tempnew);
                        adapter.notifyDataSetChanged();
                    }
                }


            }

        }
        else {
            tempnew = list.get(position);
            tempnew.put("cellid",  StringtoDisplay);
            list.set(position,tempnew);
            adapter.notifyDataSetChanged();
        }
    }

    static void setlatlong(ArrayList location,int position,String Area){

        if(location != null) {

            HashMap<String, String> tempnew = new HashMap<String, String>();
            String StringtoDisplay = "";

            if (location.size() == 0) {
                StringtoDisplay = "";
            }
            Iterator<String> it = location.iterator();

            while (it.hasNext()) {
                int itercount = 0;
                String[] result = it.next().split(",");
                StringtoDisplay = "Latitude: " + result[itercount];
                itercount++;
                StringtoDisplay += ", Longitude: " + result[itercount];

            }
            if(position == -1 && Area != null){
                for(int i=0;i< list.size();i++){
                    HashMap<String, String> map =list.get(i);
                    for (String key : map.keySet()) {
                        if(map.get(key).equals(Area)){
                            tempnew=list.get(i);
                            tempnew.put("cellid",  StringtoDisplay);
                            list.set(i,tempnew);
                            adapter.notifyDataSetChanged();
                        }
                    }


                }

            }else {
                tempnew = list.get(position);
                tempnew.put("cellid", StringtoDisplay);
                list.set(position, tempnew);
                adapter.notifyDataSetChanged();
            }

        }


    }

    static void SetCellidforArea(String Area,String cellid){

        for(int i=0;i< list.size();i++){
            HashMap<String, String> map =list.get(i);
            for (String key : map.keySet()) {
                if(map.get(key).equals(Area)){
                    String displaytext=map.get("cellid");
                    displaytext=displaytext+ ","+ cellid;
                    map.put("cellid",displaytext);
                    list.set(i,map);
                    adapter.notifyDataSetChanged();
                }
            }


        }

    }

    static void GetAreaDetails(ArrayList<String> areadetails){
        for(HashMap<String, String> map: list) {
                String key = map.get("Area");
                areadetails.add(key);
        }

    }

    public static void AddnewArea(String Area,String cellid) {
        HashMap<String,String> temp=new HashMap<String, String>();
        temp.put("Area",Area);
        temp.put("cellid",cellid);
        list.add(temp);
        adapter.notifyDataSetChanged();



    }

    private void createAreaDialog(View v) {

        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
        builder.setTitle("Configure Area");
        builder.setMessage("Enter name for new area");

        // Use an EditText view to get user input.
        final EditText input = new EditText(v.getContext());
        input.setId(TEXT_ID);
        builder.setView(input);

        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                HashMap<String,String> temp=new HashMap<String, String>();
                temp.put("Area",input.getText().toString());
                list.add(temp);
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
}
