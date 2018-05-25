package com.smartcloud.iot.smartcloud.SmartConfig

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.smartcloud.iot.smartcloud.R

/**
 * Created by wjy10 on 2018/5/15.
 */
class MainListAdapter(private var activity: Activity, private var items: ArrayList<MyClass>?) : BaseAdapter() {

    private class ViewHolder(row: View?){
        var tv_Name:TextView?   = null
        var tv_ID:TextView?     = null
        init {
            this.tv_Name= row?.findViewById(R.id.tv_name)
            this.tv_ID=row?.findViewById(R.id.tv_sensor_id)
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View{

        val view:View?
        val viewHolder: ViewHolder
        if(convertView==null){

            val inflater = activity?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.item, null)
            viewHolder = ViewHolder(view)
            view?.tag = viewHolder

        }else{
            view=convertView
            viewHolder=view.tag as ViewHolder
        }

        var myClass = items?.get(position)
        viewHolder.tv_Name?.text= myClass?.Name
        viewHolder.tv_ID?.text= myClass?.sensor_id

        return view as View
    }

    override fun getItem(i: Int): MyClass? {
        return items?.get(i)
    }
    override fun getItemId(i: Int): Long {
        return i.toLong()
    }
    override fun getCount(): Int {
        return items!!.size
    }





}