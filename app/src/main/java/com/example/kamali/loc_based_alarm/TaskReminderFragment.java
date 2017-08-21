package com.example.kamali.loc_based_alarm;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Created by kamali on 4/17/17.
 */
public class TaskReminderFragment  extends Fragment {

    private static MainActivity mInstance;
    static ArrayList<String> listDataHeader =new ArrayList<String>();
    static HashMap<String, ArrayList<String>> listDataChild =new HashMap<String,ArrayList<String>>();;
    private static final int TEXT_ID = 0;
    String spinner_selectedItem=null;
    private static String CurrentAreaValue=null;
    private static String PrevAreaValue=null;
    static TextToSpeech t1;
    static taskreminder_expandablelistadapter expandablelistAdapter;
    static List<String> temptasklist=null;
    static int count=0;
    static Boolean Edittask= false;

    public TaskReminderFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static TaskReminderFragment newInstance() {
        TaskReminderFragment fragment = new TaskReminderFragment();
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        ExpandableListView expListView;

        View rootView = inflater.inflate(R.layout.taskreminderfragment_layout, container, false);
        mInstance = (MainActivity) getActivity();

        expListView = (ExpandableListView)rootView.findViewById(R.id.lvExp);
        expandablelistAdapter = new taskreminder_expandablelistadapter(mInstance, listDataHeader, listDataChild);
        // setting list adapter
        expListView.setAdapter(expandablelistAdapter);

        /*Adapter for edit task dialog*/

        final String[] items = {"Delete Task",
                "Edit Task",
        };

        // dialog list icons: some examples here
        final int[] icons = {
                R.drawable.ic_action_delete,
                R.drawable.ic_action_edit,
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
                    // view already defined, retrieve view holder
                    holder = (ViewHolder) convertView.getTag();
                }

                holder.title.setText(items[position]);

                holder.icon.setImageResource(icons[position]);
                return convertView;
            }
        };
        expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v, final int groupPosition, int childPosition, long id) {
                final View view=v;
                final String childdata=parent.getExpandableListAdapter().getChild(groupPosition, childPosition).toString();
                final String groupdata=parent.getExpandableListAdapter().getGroup(groupPosition).toString();
                final int childpos=childPosition;

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mInstance);
                alertDialogBuilder.setAdapter(listaddadapter, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: /*Delete Task*/
                                DeleteFromMap(childdata,groupdata);
                                expandablelistAdapter.notifyDataSetChanged();
                                dialog.cancel();
                                break;
                            case 1: /*Edit Task*/
                                Edittask = true;
                                createnewTaskDialog(view,childdata,childpos,groupPosition);
                                expandablelistAdapter.notifyDataSetChanged();
                                dialog.dismiss();
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

        expListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                int itemType = ExpandableListView.getPackedPositionType(id);

                if (itemType == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
                    int groupPosition = ExpandableListView.getPackedPositionGroup(id);
                    onCreateDialogDeletegroup(view,groupPosition);
                    //do your per-group callback here
                    return true; //true if we consumed the click, false if not

                } else {
                    // null item; we don't consume the click
                    return false;
                }
            }
            });




        Button newtask = (Button)rootView.findViewById(R.id.newtaskbutton);
        newtask.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                createnewTaskDialog(v,null,-1,-1);

            }
        });



        return rootView;
    }

    public void onDestroy() {

        if(t1 != null) {
            t1.stop();
            t1.shutdown();
        }
        super.onDestroy();

    }


    private void createnewTaskDialog(View v,String data,int childpos,int groupPosition) {

        final int childposition=childpos;
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(v.getContext());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.taskreminderfragment_newtask_customdialog, null);
        dialogBuilder.setView(dialogView);

        final EditText edt = (EditText) dialogView.findViewById(R.id.edit_text);
        if( data != null){
            edt.setText(data);
        }

        t1 = new TextToSpeech(v.getContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.ERROR) {
                        t1.setLanguage(Locale.UK);
                    }
                    if (Build.VERSION.SDK_INT >= 15) {
                        t1.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                            @Override
                            public void onDone(String utteranceId) {

                                while (count != 0) {
                                    SpeakText(temptasklist);
                                    count--;

                                }
                            }

                            @Override
                            public void onError(String utteranceId) {
                            }

                            @Override
                            public void onStart(String utteranceId) {
                            }
                        });
                    } else {
                        t1.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
                            @Override
                            public void onUtteranceCompleted(String utteranceId) {

                                while (count != 0) {
                                    if (!t1.isSpeaking()) {
                                        SpeakText(temptasklist);
                                    }
                                    count--;
                                }
                            }
                        });
                    }
                }
            });
        /*Spinner Initializations*/
        ArrayList<String> tempspinnerArray = new ArrayList<String>();
        ArrayList<String> spinnerArray = new ArrayList<String>();

        AddAreaFragment.GetAreaDetails(tempspinnerArray);
        int index = 0;

        for (int i1 = 0; i1 < tempspinnerArray.size(); i1++) {

            StringBuilder string = new StringBuilder();
            ;
            string.append(tempspinnerArray.get(i1).toString());
            spinnerArray.add(index, "Entered " + string);
            index++;
            spinnerArray.add(index, "Left " + string);
            index++;

        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                v.getContext(), android.R.layout.simple_spinner_item, spinnerArray);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner sItems = (Spinner) dialogView.findViewById(R.id.viewSpin);
        sItems.setAdapter(adapter);
        if(Edittask == true) {
            sItems.setSelection(groupPosition);
        }

        sItems.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                spinner_selectedItem = parent.getItemAtPosition(position).toString();

            }

            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        View titleView = inflater.inflate(R.layout.taskreminderfragment_customdialogtitle, null);
        dialogBuilder.setCustomTitle(titleView);
        dialogBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                if (spinner_selectedItem != null) {
                    if (listDataHeader.contains(spinner_selectedItem) != true)
                        listDataHeader.add(spinner_selectedItem);


                    for (int i = 0; i < listDataHeader.size(); i++) {

                        if (listDataHeader.get(i).equals(spinner_selectedItem)) {

                            AddtoMap(edt.getText().toString(), listDataHeader.get(i),childposition);
                            expandablelistAdapter.notifyDataSetChanged();
                        }
                    }
                }

            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    public void onCreateDialogDeletegroup(View v,int grouppos) {

        final int groupposition=grouppos;

        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
        CharSequence[] array = {"Delete group"};
        builder.setTitle("Select the Action ")
                .setSingleChoiceItems(array,0,new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String grouptodelete=listDataHeader.get(groupposition);
                        listDataChild.remove(grouptodelete);
                        listDataHeader.remove(groupposition);
                        expandablelistAdapter.notifyDataSetChanged();


                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();

                    }
                });

        AlertDialog b = builder.create();
        b.show();
    }


    public static void NotifyLocationChange(String text) {

        CurrentAreaValue=text;

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                String mymessage = String.format("CurrentAreaValue = %s ", CurrentAreaValue);
                Toast.makeText(mInstance, mymessage, Toast.LENGTH_SHORT).show();
                if (CurrentAreaValue != null && CurrentAreaValue.equals(PrevAreaValue) != true) {

                    for (int i = 0; i < listDataHeader.size(); i++) {
                        if (CurrentAreaValue.equals(listDataHeader.get(i))) {
                            temptasklist = listDataChild.get(listDataHeader.get(i));
                            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mInstance);
                            int preftaskreminder = Integer.parseInt(sharedPrefs.getString("prefTaskReminder", "0"));
                            count = preftaskreminder;
                            SpeakText(temptasklist);
                            count--;
                        }
                    }
                    PrevAreaValue = CurrentAreaValue;
                }//if (CurrentAreaValue != null && CurrentAreaValue != PrevAreaValue)
            }
        }, 10000);



    }

    public static void SpeakText( List<String> temptasklist){
        String words = "";
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "LocAlarmTexttoSpeechID");

        if (Build.VERSION.SDK_INT >= 23) {
            for(String text: temptasklist) {
                words += " " + text;

            }
            t1.speak(words, TextToSpeech.QUEUE_ADD,null,"LocAlarmTexttoSpeechID");

        }else{
            for(String text: temptasklist) {
                words += " " + text;

            }
            t1.speak(words, TextToSpeech.QUEUE_ADD, map);
        }
    }


    public void AddtoMap(String item,String area,int childposition){

        ArrayList<String> itemsList = listDataChild.get(area);

        // if list does not exist create it
        if(itemsList == null) {
            itemsList = new ArrayList<String>();
            itemsList.add(item);

        } else {
            if(childposition != -1) {
                itemsList.set(childposition, item);
            }
            else {
                // add if item is not already in list
                itemsList.add(item);
            }
        }
        listDataChild.put(area, itemsList);

    }

    public void DeleteFromMap(String item,String area){

        ArrayList<String> itemsList = listDataChild.get(area);
        Iterator<String> it = itemsList.iterator();
        while (it.hasNext()) {
            String childtask=it.next();
            if(childtask.equals(item)){
                it.remove();
            }
        }
            listDataChild.put(area, itemsList);
    }

    }
