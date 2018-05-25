@file:Suppress("DEPRECATION")

package com.smartcloud.iot.smartcloud.SmartConfig

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.content.ContextCompat
import android.support.v4.content.PermissionChecker
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.espressif.iot.esptouch.EsptouchTask
import com.espressif.iot.esptouch.IEsptouchListener
import com.espressif.iot.esptouch.IEsptouchResult
import com.espressif.iot.esptouch.IEsptouchTask
import com.smartcloud.iot.smartcloud.R

@Suppress("DEPRECATION")
class reConfigActivity : AppCompatActivity() {

    private val TAG = "EsptouchDemoActivity"
    private var mreTvApSsid:                 TextView?           = null
    private var mreEdtApPassword:            EditText?           = null
    private var mreBtnConfirm:               Button?             = null
    private var mWifiAdmin:                  EspWifiAdminSimple? = null
    private val mSpinnerTaskCount:           Int                 = 1
            var mProgressDialog:             ProgressDialog?     = null
            var mEsptouchTask:               IEsptouchTask?      = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_re_config)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        //////////////////////////////////
        mWifiAdmin        =  EspWifiAdminSimple(this)
        mreTvApSsid       =  findViewById(R.id.re_tx_ssid)
        mreEdtApPassword  =  findViewById(R.id.re_et_password)
        mreBtnConfirm     =  findViewById(R.id.re_btn_confirm)

        initSpinner()

    }

    private fun initSpinner()
    {
        val spinnerItemsInt       =  Array(6,{0})
        val length:Int            =  mSpinnerTaskCount
        val spinnerItemsInteger   =  Array(length,{0})

        for (i in 0 until length) {
            spinnerItemsInteger[i] = spinnerItemsInt[i]
        }

    }


    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()
        // display the connected ap's ssid
        val apSsid = mWifiAdmin!!.wifiConnectedSsid
        if (apSsid != null) {
            mreTvApSsid!!.text = apSsid
        } else {
            mreTvApSsid!!.text = "未连接WiFi"
        }
        // check whether the wifi is connected
        val isApSsidEmpty = TextUtils.isEmpty(apSsid)
        mreBtnConfirm!!.isEnabled = !isApSsidEmpty
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
    fun btn_reconfig_click(view: View)
    {

        val taskResultCountStr    =    Integer.toString(0)
        val apSsid                =    mreTvApSsid!!.text.toString()
        val apPassword            =    mreEdtApPassword?.text.toString()
        val apBssid               =    mWifiAdmin!!.wifiConnectedBssid

        if(mreEdtApPassword!!.text.isNullOrBlank())
        {
            Toast.makeText(this,"请填写密码", Toast.LENGTH_SHORT).show()

        }else{

            if(ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_MULTICAST_STATE)== PermissionChecker.PERMISSION_DENIED)
            {
                Toast.makeText(this,"权限不够", Toast.LENGTH_SHORT).show()
                if(shouldShowRequestPermissionRationale(Manifest.permission.CHANGE_WIFI_MULTICAST_STATE))
                {
                    Toast.makeText(this,"这个功能需要这项权限", Toast.LENGTH_LONG).show()

                }else{

                    requestPermissions(arrayOf(Manifest.permission.CHANGE_WIFI_MULTICAST_STATE),0)

                }


            }else{

                EsptouchAsyncTask2().execute(apSsid, apBssid, apPassword, taskResultCountStr)

            }
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        when(requestCode){
            0 -> {
                if(grantResults.isNotEmpty()&& grantResults[0]== PermissionChecker.PERMISSION_GRANTED){
                    val taskResultCountStr = Integer.toString(0)
                    val apSsid = mreTvApSsid!!.text.toString()
                    val apPassword = mreEdtApPassword?.text.toString()
                    val apBssid = mWifiAdmin!!.wifiConnectedBssid
                    if(mreEdtApPassword!!.text.isNullOrBlank())
                    {
                        Toast.makeText(this,"请将信息填写完整", Toast.LENGTH_SHORT).show()

                    }else{

                        EsptouchAsyncTask2().execute(apSsid, apBssid, apPassword, taskResultCountStr)

                    }

                }else{ Toast.makeText(this,"权限未申请成功", Toast.LENGTH_SHORT).show()  }
            }

        // other 'case' lines to check for other
        // permissions this app might request
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }



    @Suppress("DEPRECATION")
    private inner class EsptouchAsyncTask2 : AsyncTask<String, Void, IEsptouchResult>() {

        private var mProgressDialog: ProgressDialog? = null
        private var mEsptouchTask:   IEsptouchTask?  = null
        // without the lock, if the user tap confirm and cancel quickly enough,
        // the bug will arise. the reason is follows:
        // 0. task is starting created, but not finished
        // 1. the task is cancel for the task hasn't been created, it do nothing
        // 2. task is created
        // 3. Oops, the task should be cancelled, but it is running
        private val mLock = Any()

        override fun onPreExecute() {

            mProgressDialog = ProgressDialog(this@reConfigActivity)
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
                    "请等待...") { dialog, which -> }
            mProgressDialog!!.show()
            mProgressDialog!!.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = false
        }

        override fun doInBackground(vararg params: String): IEsptouchResult {
            synchronized(mLock) {
                val apSsid = params[0]
                val apBssid = params[1]
                val apPassword = params[2]
                mEsptouchTask = EsptouchTask(apSsid, apBssid, apPassword,false,
                        this@reConfigActivity)
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
                    mProgressDialog!!.setMessage("连接失败")
                }
            }
        }
    }


    private fun onEsptoucResultAddedPerform(result: IEsptouchResult) {
        runOnUiThread {
            val text = result.bssid.toString()+ " 已连接"
            Toast.makeText(this@reConfigActivity, text,
                    Toast.LENGTH_LONG).show()

            mProgressDialog!!.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = true
            mProgressDialog!!.getButton(DialogInterface.BUTTON_POSITIVE).text = "确定"


        }
    }


    private val myListener = IEsptouchListener { result -> onEsptoucResultAddedPerform(result) }


    @Suppress("DEPRECATION")
    private inner class EsptouchAsyncTask3 : AsyncTask<String, Void, List<IEsptouchResult>>() {


        // without the lock, if the user tap confirm and cancel quickly enough,
        // the bug will arise. the reason is follows:
        // 0. task is starting created, but not finished
        // 1. the task is cancel for the task hasn't been created, it do nothing
        // 2. task is created
        // 3. Oops, the task should be cancelled, but it is running
        private val mLock = Any()

        override fun onPreExecute() {
            mProgressDialog = ProgressDialog(this@reConfigActivity)
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

                val intent= Intent()
                intent.putExtra("name","")
                setResult(Activity.RESULT_OK,intent)

                this@reConfigActivity.finish()

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
                mEsptouchTask = EsptouchTask(apSsid, apBssid, apPassword, false,this@reConfigActivity)
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
