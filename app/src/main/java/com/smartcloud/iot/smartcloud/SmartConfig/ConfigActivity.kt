@file:Suppress("DEPRECATION")

package com.smartcloud.iot.smartcloud.SmartConfig

/////////////////////////////////////////////
import android.Manifest
import android.Manifest.permission.CHANGE_WIFI_MULTICAST_STATE
import android.annotation.SuppressLint
import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import com.smartcloud.iot.smartcloud.R

import com.espressif.iot.esptouch.EsptouchTask
import com.espressif.iot.esptouch.IEsptouchResult
import com.espressif.iot.esptouch.IEsptouchTask
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent

import android.os.AsyncTask

import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.espressif.iot.esptouch.IEsptouchListener
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.content.ContextCompat
import android.support.v4.content.PermissionChecker.PERMISSION_DENIED
import android.support.v4.content.PermissionChecker.PERMISSION_GRANTED


@Suppress("DEPRECATION")
class ConfigActivity : AppCompatActivity() {

    private val TAG = "EsptouchDemoActivity"
    private var mTvApSsid:                 TextView?  = null
    private var mEdtApPassword:            EditText?  = null
    private var mEdtID:                    EditText?  = null
    private var mEdtName:                  EditText?  = null
    private var mBtnConfirm:               Button?    = null
    private var mWifiAdmin:                EspWifiAdminSimple? = null
    private val mSpinnerTaskCount:         Int                 = 1
            var mProgressDialog:           ProgressDialog?     = null
            var mEsptouchTask:             IEsptouchTask?      = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_config)


        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        //////////////////////////////////
        mWifiAdmin      =  EspWifiAdminSimple(this)
        mTvApSsid       =  findViewById(R.id.tx_ssid)
        mEdtApPassword  =  findViewById(R.id.et_password)
        mBtnConfirm     =  findViewById(R.id.btn_confirm)
        mEdtName        =  findViewById(R.id.et_name)
        mEdtID          =  findViewById(R.id.et_id)

        initSpinner()

    }

    private fun initSpinner()
    {
        val spinnerItemsInt        =  Array(6,{0})
        val length:Int             =  mSpinnerTaskCount
        val spinnerItemsInteger    =  Array(length,{0})


        for (i in 0 until length) {
            spinnerItemsInteger[i] = spinnerItemsInt[i]
        }

    }


    override fun onResume() {
        super.onResume()
        // display the connected ap's ssid
        val apSsid = mWifiAdmin!!.wifiConnectedSsid
        if (apSsid != null) {
            mTvApSsid!!.text = apSsid
        } else {
            mTvApSsid!!.text = "未连接WiFi"
        }
        // check whether the wifi is connected
        val isApSsidEmpty = TextUtils.isEmpty(apSsid)
        mBtnConfirm!!.isEnabled = !isApSsidEmpty
    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            when (item.itemId) {
                android.R.id.home -> {
                    this.finish() // back button
                    return true
                }

            }
        }
        return super.onOptionsItemSelected(item)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun btn_config_click(view: View)
    {

        val taskResultCountStr    =    Integer.toString(0)
        val apSsid                =    mTvApSsid!!.text.toString()
        val apPassword            =    mEdtApPassword?.text.toString()
        val apBssid               =    mWifiAdmin!!.wifiConnectedBssid

        if(mEdtApPassword!!.text.isNullOrBlank()|| mEdtName!!.text.isNullOrBlank())
        {
            Toast.makeText(this,"请填将信息填写完整",Toast.LENGTH_SHORT).show()

        }else{

            if(ContextCompat.checkSelfPermission(this,Manifest.permission.CHANGE_WIFI_MULTICAST_STATE)==PERMISSION_DENIED)
            {
                Toast.makeText(this,"权限不够",Toast.LENGTH_SHORT).show()
                if(shouldShowRequestPermissionRationale(CHANGE_WIFI_MULTICAST_STATE))
                {
                    Toast.makeText(this,"这个功能需要这项权限",Toast.LENGTH_LONG).show()
                }else{

                    requestPermissions(arrayOf(CHANGE_WIFI_MULTICAST_STATE),0)

                }


            }else{

                EsptouchAsyncTask2().execute(apSsid, apBssid, apPassword, taskResultCountStr)

            }
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        when(requestCode){
            0 -> {
                if(grantResults.isNotEmpty()&& grantResults[0]==PERMISSION_GRANTED){
                    val  taskResultCountStr  =  Integer.toString(0)
                    val  apSsid              =  mTvApSsid!!.text.toString()
                    val  apPassword          =  mEdtApPassword?.text.toString()
                    val  apBssid             =  mWifiAdmin!!.wifiConnectedBssid

                    if(mEdtApPassword!!.text.isNullOrBlank()|| mEdtName!!.text.isNullOrBlank())
                    {
                        Toast.makeText(this,"请将信息填写完整",Toast.LENGTH_SHORT).show()

                    }else{
                        EsptouchAsyncTask3().execute(apSsid, apBssid, apPassword, taskResultCountStr)

                    }

                }else{ Toast.makeText(this,"权限未申请成功",Toast.LENGTH_SHORT).show()  }
            }

        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }



    @SuppressLint("StaticFieldLeak")
    private inner class EsptouchAsyncTask2 : AsyncTask<String, Void, IEsptouchResult>() {

        private var mProgressDialog:  ProgressDialog? = null
        private var mEsptouchTask:    IEsptouchTask?  = null
        private val mLock = Any()
        // without the lock, if the user tap confirm and cancel quickly enough,
        // the bug will arise. the reason is follows:
        // 0. task is starting created, but not finished
        // 1. the task is cancel for the task hasn't been created, it do nothing
        // 2. task is created
        // 3. Oops, the task should be cancelled, but it is running


        override fun onPreExecute() {

            mProgressDialog = ProgressDialog(this@ConfigActivity)
            mProgressDialog!!
                    .setMessage("正在连接WiFi请等待...")
            mProgressDialog!!.setCanceledOnTouchOutside(false)
            mProgressDialog!!.setOnCancelListener {
                synchronized(mLock) {

                    if (mEsptouchTask != null) {
                        mEsptouchTask!!.interrupt()
                    }
                }
            }
            mProgressDialog!!.setButton(DialogInterface.BUTTON_POSITIVE,
                    "请等待...", { dialogInterface, i ->

                val intent= Intent()
                intent.putExtra("id", mEdtID!!.text.toString())
                intent.putExtra("name", mEdtName!!.text.toString())
                setResult(Activity.RESULT_OK,intent)

                this@ConfigActivity.finish()

            })
            mProgressDialog!!.show()
            mProgressDialog!!.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = false
        }

        override fun doInBackground(vararg params: String): IEsptouchResult {
            synchronized(mLock) {
                val apSsid      = params[0]
                val apBssid     = params[1]
                val apPassword  = params[2]
                mEsptouchTask   = EsptouchTask(apSsid, apBssid, apPassword,false,
                        this@ConfigActivity)
            }
            return mEsptouchTask!!.executeForResult()
        }

        override fun onPostExecute(result: IEsptouchResult) {
            mProgressDialog!!.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = true
            mProgressDialog!!.getButton(DialogInterface.BUTTON_POSITIVE).text = "确定"
            // it is unnecessary at the moment, add here just to show how to use isCancelled()
            if (!result.isCancelled) {
                if (result.isSuc) {
                    mProgressDialog!!.setMessage("连接成功, ssid = "
                            + result.bssid + ",IP地址 = "
                            + result.inetAddress.hostAddress)
                } else {
                    mProgressDialog!!.setMessage("获取设备状态失败\n-确认设备联网后点击确定 \n-若未连接可点击返回键尝试重新连接")
                }
            }
        }
    }


    private fun onEsptoucResultAddedPerform(result: IEsptouchResult) {
        runOnUiThread {
            val text = result.bssid.toString()+ " 正在连接WiFi..."
            Toast.makeText(this@ConfigActivity, text,
                    Toast.LENGTH_LONG).show()

            mProgressDialog!!.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = true
            mProgressDialog!!.getButton(DialogInterface.BUTTON_POSITIVE).text = "确定"


        }
    }


    private val myListener = IEsptouchListener { result -> onEsptoucResultAddedPerform(result) }


    @SuppressLint("StaticFieldLeak")
    private inner class EsptouchAsyncTask3 : AsyncTask<String, Void, List<IEsptouchResult>>() {


        // without the lock, if the user tap confirm and cancel quickly enough,
        // the bug will arise. the reason is follows:
        // 0. task is starting created, but not finished
        // 1. the task is cancel for the task hasn't been created, it do nothing
        // 2. task is created
        // 3. Oops, the task should be cancelled, but it is running
        private val mLock = Any()

        override fun onPreExecute() {
            mProgressDialog = ProgressDialog(this@ConfigActivity)
            mProgressDialog!!
                    .setMessage(" 正在配置WiFi...\n确认设备联网后点击确定")
            mProgressDialog!!.setCanceledOnTouchOutside(false)
            mProgressDialog!!.setOnCancelListener {
                synchronized(mLock) {

                    if (mEsptouchTask != null) {
                        mEsptouchTask!!.interrupt()
                    }
                }

            }

            mProgressDialog!!.setButton(ProgressDialog.BUTTON_NEGATIVE, "确认", DialogInterface.OnClickListener {
                dialog, which -> mProgressDialog!!.dismiss()

                var intent= Intent()
                intent.putExtra("id", mEdtID!!.text.toString())
                intent.putExtra("name", mEdtName!!.text.toString())
                setResult(Activity.RESULT_OK,intent)

                this@ConfigActivity.finish()

            })


            mProgressDialog!!.show()

            mProgressDialog!!.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = true
        }

        override fun doInBackground(vararg params: String): List<IEsptouchResult> {
            var taskResultCount = -1
            synchronized(mLock) {
                val apSsid = params[0]
                val apBssid = params[1]
                val apPassword = params[2]
                val taskResultCountStr = params[3]
                taskResultCount = Integer.parseInt(taskResultCountStr)
                mEsptouchTask = EsptouchTask(apSsid, apBssid, apPassword, false,this@ConfigActivity)
                mEsptouchTask!!.setEsptouchListener(myListener)
            }



            return mEsptouchTask!!.executeForResults(taskResultCount)
        }




        override fun onPostExecute(result: List<IEsptouchResult>) {
//            mProgressDialog!!.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = true
//            mProgressDialog!!.getButton(DialogInterface.BUTTON_POSITIVE).text = "确定"
            val firstResult = result[0]
            // check whether the task is cancelled and no results received
            if (!firstResult.isCancelled) {
                var count = 0
                // max results to be displayed, if it is more than maxDisplayCount,
                // just show the count of redundant ones
                val maxDisplayCount = 5
                // the task received some results including cancelled while
                // executing before receiving enough results
                if (firstResult.isSuc) {
                    val sb = StringBuilder()
                    for (resultInList in result) {
                        sb.append("WiFi连接成功, bssid = "
                                + resultInList.bssid
                                + ",IP地址 = "
                                + resultInList.inetAddress
                                .hostAddress + "\n")
                        count++
                        if (count >= maxDisplayCount) {
                            break
                        }
                    }
                    if (count < result.size) {
                        sb.append("\nthere's " + (result.size - count)
                                + " more result(s) without showing\n")
                    }
                    mProgressDialog!!.setMessage(sb.toString())
                } else {
                    mProgressDialog!!.setMessage("配网失败")
                }
            }
        }
    }




}
