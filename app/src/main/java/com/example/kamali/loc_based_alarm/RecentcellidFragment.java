package com.example.kamali.loc_based_alarm;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
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
import android.support.v4.app.Fragment;

import java.util.ArrayList;
import java.util.HashMap;

public class RecentcellidFragment extends Fragment{

    static SimpleAdapter adapter;
    static ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
    static final int FRAGMENT_GROUPID = 35;
    static final int MENU_ADDTOAREA = 1;
    static final int MENU_REMOVEAREA = 2;
    private static final int TEXT_ID = 1;
    ArrayList<String> areadetails;
    private static MainActivity mInstance;


    public RecentcellidFragment() {
    }

    public static RecentcellidFragment newInstance() {
        RecentcellidFragment fragment = new RecentcellidFragment();
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.recent_cellid_fragment, container, false);
        mInstance = (MainActivity) getActivity();
        ListView lv = (ListView) rootView.findViewById(R.id.list);
        adapter = new SimpleAdapter(getActivity().getBaseContext(),list,
                R.layout.custom_recentcellid_view,
                new String[] {"Time","cellid","Area"},
                new int[] {R.id.text1,R.id.text2,R.id.text3}

        );
        lv.setAdapter(adapter);
        registerForContextMenu(lv);

        return rootView;
    }

    public interface GetAreaDetails{
        public void GetAreaDetails(ArrayList<String> areadetails);
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId()==R.id.list) {
            menu.add(FRAGMENT_GROUPID,  MENU_ADDTOAREA, Menu.NONE, "Add to Area");
            menu.add(FRAGMENT_GROUPID, MENU_REMOVEAREA, Menu.NONE, "Remove from Area");
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getGroupId() == FRAGMENT_GROUPID) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            switch (item.getItemId()) {
                case  MENU_ADDTOAREA:
                    Dialog dialog = onCreateDialogAddtoArea(info);
                    dialog.show();
                    return true;
                case MENU_REMOVEAREA:
                    Dialog dialogRemove = onCreateDialogRemoveArea(info);
                    dialogRemove.show();

                    return true;
                default:
                    return super.onContextItemSelected(item);
            }
        }
        return super.onContextItemSelected(item);
    }

    public Dialog onCreateDialogAddtoArea(final AdapterView.AdapterContextMenuInfo info) {

        ArrayList <String> array=new ArrayList<String>();
        HashMap<String,String> temp=list.get(info.position);
        final String cellid=temp.get("cellid");
        final String Area=temp.get("Area");

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        areadetails = new ArrayList<String>();
        AddAreaFragment.GetAreaDetails(areadetails);
        array.add("Create new area");
        for(String area:areadetails){
            array.add(area);

        }
        CharSequence[] cs=array.toArray(new CharSequence[array.size()]);
        builder.setTitle("Select the area to add " + cellid + " to:");
                builder.setSingleChoiceItems(cs, -1, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                createAreaDialog(cellid, info.targetView, info.position);
                                dialog.cancel();
                                break;
                            default:
                                ListView lw = ((AlertDialog) dialog).getListView();
                                Object checkedItem = lw.getAdapter().getItem(lw.getCheckedItemPosition());

                                for (int i = 0; i < list.size(); i++) {

                                    String listcellid = list.get(i).get("cellid");

                                    if (cellid.equals(listcellid)) {

                                        String listArea = list.get(i).get("Area");

                                        if (listArea.equals("not mapped to any area") == true){
                                            listArea=String.valueOf(checkedItem);
                                        } else if (listArea.equals(String.valueOf(checkedItem)) != true) {
                                            listArea = Area + "," + String.valueOf(checkedItem);
                                        }
                                        HashMap<String, String> temp = new HashMap<String, String>();
                                        temp = list.get(i);
                                        temp.put("Area", listArea);
                                        list.set(i, temp);
                                        adapter.notifyDataSetChanged();

                                    }
                                }
                                String Texttoremove="Cellid: ";
                                String newString=cellid.replaceAll(Texttoremove,"");
                                mInstance.AddCellidToArea(String.valueOf(checkedItem), newString);

                                break;
                        }

                    }

                });

        return builder.create();
    }

    private void createAreaDialog(String cell_id,View v,int pos) {
        final int position=pos;
        final String cellid=cell_id;

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

                for (int i = 0; i < list.size(); i++) {

                    String listcellid = list.get(i).get("cellid");

                    if (cellid.equals(listcellid)) {

                        String Area = list.get(i).get("Area");

                        if (Area.equals("not mapped to any area") == true){
                            Area=input.getText().toString();
                        } else if (Area.equals(input.getText().toString()) != true) {
                            Area = Area + "," + input.getText().toString();
                        }
                        HashMap<String, String> temp = new HashMap<String, String>();
                        temp = list.get(i);
                        temp.put("Area", Area);
                        list.set(i, temp);
                        adapter.notifyDataSetChanged();

                    }
                }
                AddAreaFragment.AddnewArea(input.getText().toString(), cellid);
                   /*Notify the service of the new added area*/
                String Texttoremove="Cellid: ";
                String newString=cellid.replaceAll(Texttoremove,"");
                mInstance.AddNewArea(input.getText().toString(), newString);
                dialog.cancel();
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

    private Dialog onCreateDialogRemoveArea(final AdapterView.AdapterContextMenuInfo info) {

        ArrayList <String> array=new ArrayList<String>();
        HashMap<String,String> temp=list.get(info.position);
        final String cellid=temp.get("cellid");
        String Area=temp.get("Area");

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        areadetails = new ArrayList<String>();
        AddAreaFragment.GetAreaDetails(areadetails);

        for(String area:areadetails){
            array.add(area);

        }
        CharSequence[] cs=array.toArray(new CharSequence[array.size()]);
        builder.setTitle("Select the area to remove " + cellid + " from:");
        builder.setSingleChoiceItems(cs, -1, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                }

            }

        });
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                ListView lw = ((AlertDialog) dialog).getListView();
                Object checkedItem = lw.getAdapter().getItem(lw.getCheckedItemPosition());

                for (int i = 0; i < list.size(); i++) {

                    String listcellid = list.get(i).get("cellid");

                    if (cellid.equals(listcellid)) {

                        String listArea = list.get(i).get("Area");
                        String Texttoremove=String.valueOf(checkedItem);
                        String newString=listArea.replace(Texttoremove, "");

                        String finalString=newString.replace(",","");

                        if(finalString == ""){
                            finalString= "not mapped to any area";

                        }

                        HashMap<String, String> temp = new HashMap<String, String>();
                        temp = list.get(i);
                        temp.put("Area", finalString);
                        list.set(i, temp);

                        adapter.notifyDataSetChanged();

                    }
                }

                String Texttoremove="Cellid: ";
                String newcellid=cellid.replaceAll(Texttoremove, "");
                mInstance.RemoveCellidFromArea(String.valueOf(checkedItem), newcellid);


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

       return builder.create();


    }

    public static void SetListItem(String Time,String Cellid,String Area) {

        HashMap<String, String> temp = new HashMap<String, String>();
        temp.put("Time", Time);
        temp.put("cellid", "Cellid: " + Cellid);
        temp.put("Area", Area);
        list.add(temp);
        adapter.notifyDataSetChanged();

        if (Area.equals("not mapped to any area") != true) {

            for (int i = 0; i < list.size(); i++) {
                String listcellid = list.get(i).get("cellid");

                if (listcellid.equals(Cellid)) {
                    HashMap<String, String> tempvar = new HashMap<String, String>();
                    tempvar = list.get(i);
                    tempvar.put("Area", Area);
                    list.set(i, tempvar);
                    adapter.notifyDataSetChanged();

                }
            }


        }
    }
}
