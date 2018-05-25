package com.smartcloud.iot.smartcloud

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import com.smartcloud.iot.smartcloud.SmartConfig.ConfigActivity
import com.smartcloud.iot.smartcloud.SmartConfig.MainListAdapter
import com.smartcloud.iot.smartcloud.SmartConfig.MyClass
import com.smartcloud.iot.smartcloud.SmartConfig.reConfigActivity
import java.net.UnknownHostException


@Suppress("NAME_SHADOWING")
class MainActivity : AppCompatActivity() {

    private var mPop:     PopupWindow?          = null
    private var listView: ListView?             = null
    private var mAdapter: MainListAdapter?      = null
    private var list:     ArrayList<MyClass>    = ArrayList()
            var Counts:   Int                   = 0


    @SuppressLint("ApplySharedPref", "InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.lst_1)

        listView!!.setOnItemLongClickListener { adapterView, view, i, l ->


            val v         =  layoutInflater.inflate(R.layout.pop,null)
            val tv_remove =  v.findViewById<TextView>(R.id.pop_remove)


            tv_remove.setOnClickListener {

                mPop!!.dismiss()
                list.removeAt(i)
                Counts--

                val mySharedPreferences = getSharedPreferences("smartCloudConfig", Activity.MODE_PRIVATE)
                val editor              = mySharedPreferences.edit()
                editor.putInt("COUNTS",Counts)

                for ((i, item) in list.withIndex()){

                    editor.putString("name"+i.toString() , item.Name)
                    editor.putString("id"+ i.toString(),item.sensor_id)

                }

                editor.commit()
                mAdapter           =  MainListAdapter(this,list)
                listView?.adapter  =  mAdapter
                mAdapter?.notifyDataSetChanged()

            }

            mPop=PopupWindow(v,view.width,ViewGroup.LayoutParams.WRAP_CONTENT)
            mPop!!.isOutsideTouchable  =  true
            mPop!!.isFocusable         =  true
            mPop!!.showAsDropDown(view)

            return@setOnItemLongClickListener true
        }

        listView!!.setOnItemClickListener { adapterView, view, i, l ->

            val item_name  =  list[i].Name
            val item_id    =  list[i].sensor_id

            try {

                val intent =  Intent(this,ChartActivity::class.java)
                intent.putExtra("name",item_name)
                intent.putExtra("id",item_id)
                startActivity(intent)

            }
            catch (e:UnknownHostException)
            {
                Toast.makeText(this,"网络错误！",Toast.LENGTH_LONG).show()
            }

        }

        val mySharedPreferences = getSharedPreferences("smartCloudConfig", Activity.MODE_PRIVATE)
            Counts              = mySharedPreferences.getInt("COUNTS",0)

        if(Counts!=0)
        {
            for (i in 1..Counts)
            {

                list.add(MyClass(mySharedPreferences.getString("name"+ i.toString(),"none"),
                        mySharedPreferences.getString("id"+ i.toString(),"none")))

            }
            mAdapter = MainListAdapter(this,list)
            listView?.adapter = mAdapter
            mAdapter?.notifyDataSetChanged()

        }

    }



    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.act_main_menu,menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            when (item.itemId) {

                R.id.item_add ->{
                    var intent  =  Intent(this,ConfigActivity::class.java)
                    startActivityForResult(intent,0)

                }
            }
        }
        return super.onOptionsItemSelected(item)
    }
    @SuppressLint("ApplySharedPref")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data != null) {

            val mySharedPreferences = getSharedPreferences("smartCloudConfig", Activity.MODE_PRIVATE)
            val editor              = mySharedPreferences.edit()

            Counts++
            editor.putInt("COUNTS",Counts)
            editor.putString("name"+ Counts.toString(), data.getStringExtra("name"))
            editor.putString("id"+ Counts.toString(), data.getStringExtra("id"))


            /////////////////////////////add item

            list.add(MyClass(data.getStringExtra("name"),data.getStringExtra("id")))

            mAdapter           =  MainListAdapter(this,list)
            listView?.adapter  =  mAdapter
            mAdapter?.notifyDataSetChanged()

            Toast.makeText(this,data.getStringExtra("id")+data.getStringExtra("name")+"已添加",Toast.LENGTH_LONG).show()
            editor.commit()
        }
    }

    fun pop_edit_click(view: View){
        val intent=Intent(this,reConfigActivity::class.java)
        startActivity(intent)
    }


}
