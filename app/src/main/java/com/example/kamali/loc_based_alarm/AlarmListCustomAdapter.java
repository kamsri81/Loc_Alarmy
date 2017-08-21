package com.example.kamali.loc_based_alarm;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kamali on 2/9/17.
 */

public class AlarmListCustomAdapter extends ArrayAdapter<AlarmListFragment.AlarmData> {

    Context context;
    ArrayList <AlarmListFragment.AlarmData>alarmdata;

    public AlarmListCustomAdapter(Context context, int resourceId,ArrayList<AlarmListFragment.AlarmData>alarmdata) {
        super(context, resourceId,alarmdata);
        this.context = context;
        this.alarmdata=alarmdata;

    }

    @Override
    public int getCount() {
        return alarmdata.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    /*private view holder class*/
    private class ViewHolder {
        public TextView time;
        public TextView AlarmTitle;
        public TextView Selectedcondition;
        public TextView SelectedRepeatcondition;
        Switch swobject;
    }


    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        final AlarmListFragment.AlarmData alarmdata = getItem(position);
        alarmdata.listposition=position;


        LayoutInflater mInflater = (LayoutInflater) context
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.custom_alarmlist_rowview, null);
            holder = new ViewHolder();
            holder.time = (TextView) convertView.findViewById(R.id.timetext);
            holder.Selectedcondition = (TextView) convertView.findViewById(R.id.selectedconditiontext);
            holder.AlarmTitle = (TextView) convertView.findViewById(R.id.AlarmTitle);
            holder.swobject = (Switch) convertView.findViewById(R.id.switch1);
            holder.swobject = (Switch) convertView.findViewById(R.id.switch1);
            convertView.setTag(holder);
        } else
            holder = (ViewHolder) convertView.getTag();

        final View cView =convertView;
        holder.time.setText(alarmdata.gettime());
        holder.AlarmTitle.setText(alarmdata.AlarmTitle);


        StringBuilder string=new StringBuilder();
        string.append("Ring ");
        int itercount =0;
        if(alarmdata.getSelectedRepeatedcondition()!=null) {

            for(String a :alarmdata.SelectedRepeatedCondition) {


                if (alarmdata.SelectedRepeatedCondition.size() == 7) {
                    string.append("everyday ");
                    break;
                } else {

                    if (itercount != 0) {
                        string.append(",");
                    }
                    string.append(a);
                    itercount++;
                }
                string.append(" ");
            }

        }
        else {
            string.append("once ");
        }
        if(alarmdata.getSelectedcondition().equals("None")!=true) {
            string.append("when " + alarmdata.getSelectedcondition());
        }

        holder.Selectedcondition.setText(string);

        if(alarmdata.tooglealarmmanually == true){
            holder.swobject.setChecked(false);

        }else {
            holder.swobject.setChecked(true);
        }

        if(alarmdata.alarmrequestcode == 0 )
            AlarmListFragment.setAlarm(alarmdata);

        holder.swobject .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                if (isChecked) {
                    //cView.setBackgroundColor(0xffffff);
                    cView.setBackgroundResource(R.color.colorwhite);
                    AlarmListFragment.setAlarm(alarmdata);

                } else {
                    cView.setBackgroundResource(R.color.colorbluegreymedium);
                    AlarmListFragment.CancelAlarm(alarmdata);


                }

            }
        });

        return convertView;


    }
}
