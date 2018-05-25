package com.smartcloud.iot.smartcloud

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import cn.bmob.v3.Bmob
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.QueryListener
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat


@Suppress("NAME_SHADOWING")
class ChartActivity : AppCompatActivity() {

    var sensor_id :           String        =    ""
    var name :                String        =    ""
    var tempChart :           LineChart?    =    null
    var humChart :            LineChart?    =    null
    var Lasttime                            =    "1970-01-01 00:00:00"
    private var job:          Job?          =    null
    private var tv_hum :      TextView?     =    null
    private var tv_tmp :      TextView?     =    null




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chart)

        name        =   this.intent.getStringExtra("name")
        sensor_id   =   this.intent.getStringExtra("id")

        Log.e("name", name)
        Log.e("id",sensor_id)

        val actionBar = supportActionBar

        if (actionBar != null) {

            //actionBar.setHomeButtonEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)

        }

        /************************************************************/
        tempChart    =  findViewById(R.id.chart_tmp)
        humChart     =  findViewById(R.id.chart_hum)
        tv_hum       =  findViewById(R.id.tv_hm)
        tv_tmp       =  findViewById(R.id.tv_tp)

        val Ctmp = DynamicLineChartManager(tempChart!!,"Temperature", Color.GREEN)
        val Chum = DynamicLineChartManager(humChart!!,"Humility",Color.BLUE)
        Ctmp.setYAxis(50f,-20f,5)
        Chum.setYAxis(100f,0f,5)
        Ctmp.setDescription("实时温度")
        Chum.setDescription("实时湿度")
        //doChart(Ctmp,Chum,50,tv_tmp!!,tv_hum!!)
        //开启后台线程
        val Background = newFixedThreadPoolContext(2, "bg")
        job = launch(Background) {
            try {
                doChart(Ctmp, Chum, 50, tv_tmp!!, tv_hum!!)
            }catch (e:java.net.UnknownHostException){

                Log.e("ERROR","ADD ERROR")
            }
            while (true) {
                try {
                    doChart(Ctmp, Chum, 1, tv_tmp!!, tv_hum!!)
                    delay(5000L)
                }catch (e:java.net.UnknownHostException){
                    Log.e("info", "crashed" + e.toString())

                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job!!.cancel()

    }


    @SuppressLint("SimpleDateFormat")
    fun doChart(Ctmp:DynamicLineChartManager, Chum:DynamicLineChartManager, ItemCount:Int, tv_Temp:TextView, tv_Hum:TextView){
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        ////////////////////////////////////////////
        Bmob.initialize(this, "appKey")
        val query = BmobQuery<String>("tableName")
        /////////////////////////////////////////////

        query.addWhereEqualTo("SensorID",sensor_id.toInt())
        query.order("-createdAt")
        query.setLimit(ItemCount)
        query.findObjectsByTable(object : QueryListener<JSONArray>() {
            @SuppressLint("SetTextI18n")
            override fun done(ary: JSONArray, e: BmobException?) {
                if (e == null) {
                    if(ary.length() == 0){
                        Log.e("ERROR","getnull")
                    }
                    val obj: JSONObject = ary.get(0) as JSONObject
                    val timeorg = obj.getString("createdAt")
                    val time = dateFormatter.parse(timeorg)
                    val last = dateFormatter.parse(Lasttime)

                    if(time.compareTo(last)!=0 || ItemCount == 50) {
                        val temp = obj.getInt("temp")
                        val hum = obj.getInt("hum")
                        tv_Temp.text = temp.toString() + "℃"
                        tv_Hum.text = hum.toString() + "%"
                        Lasttime = timeorg;

                        for(i in 0..(ary.length() - 1)) {
                            val obj: JSONObject = ary.get(ary.length()-i-1) as JSONObject
                            val temp = obj.getInt("temp")
                            val hum = obj.getInt("hum")
                            val time = obj.getString("createdAt").split(" ")[1]
                            Ctmp.addEntry(temp, time.split(":")[2])
                            Chum.addEntry(hum, time.split(":")[2])
                            Log.d("INFO", "查询成功"+ time)
                        }
                        tempChart!!.invalidate()
                        humChart!!.invalidate()
                    }else{
                        //throw e!!
                        Log.d("INFO","没有更新")
                    }

                } else {
                    Log.e("ERROR","查询失败")
                }
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) when (item.itemId) {
            android.R.id.home -> {
                this.finish() // back button
                return true
            }

        }
        return super.onOptionsItemSelected(item)
    }

    inner class DynamicLineChartManager//一条曲线
    (mLineChart: LineChart, name: String, color: Int) {

        private var lineChart:       LineChart?    = mLineChart
        private var leftAxis:        YAxis?        =  null
        private var rightAxis:       YAxis?        =  null
        private var xAxis:           XAxis?        =  null
        private var lineData:        LineData?     =  null
        private var lineDataSet:     LineDataSet?  =  null
        private val lineDataSets     =  ArrayList<ILineDataSet>()
        private val timeList         = ArrayList<String>() //存储x轴的时间

        /**
         * 初始化LineChar
         */
        private fun initLineChart() {

            lineChart!!.setDrawGridBackground(false)
            //显示边界
            lineChart!!.setDrawBorders(true)
            lineChart!!.animateXY(2500,2500)
            //折线图例 标签 设置
            val legend = lineChart!!.legend
            legend.form = Legend.LegendForm.LINE
            legend.textSize = 11f
            //显示位置
            legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
            legend.orientation = Legend.LegendOrientation.HORIZONTAL
            legend.setDrawInside(false)

            //X轴设置显示位置在底部
            xAxis!!.position = XAxis.XAxisPosition.BOTTOM
            xAxis!!.granularity = 1f
            xAxis!!.labelCount = 10

            xAxis!!.valueFormatter = IAxisValueFormatter { value, axis -> timeList[value.toInt() % timeList.size] }

            leftAxis!!.axisMinimum = 0f
            rightAxis!!.axisMinimum = 0f
        }

        /**
         * 初始化折线(一条线)
         *
         * @param name
         * @param color
         */
        private fun initLineDataSet(name: String, color: Int) {

            lineDataSet = LineDataSet(null, name)
            lineDataSet!!.lineWidth = 1.5f
            lineDataSet!!.circleRadius = 1.5f
            lineDataSet!!.color = color
            lineDataSet!!.setCircleColor(color)
            lineDataSet!!.highLightColor = color
            //设置曲线填充
            lineDataSet!!.setDrawFilled(true)
            lineDataSet!!.axisDependency = YAxis.AxisDependency.LEFT
            lineDataSet!!.valueTextSize = 10f
            lineDataSet!!.mode = LineDataSet.Mode.CUBIC_BEZIER
            //添加一个空的 LineData
            lineData = LineData()
            lineChart!!.data = lineData
            lineChart!!.invalidate()

        }

        /**
         * 动态添加数据（一条折线图）
         *
         * @param number
         */
        fun addEntry(number: Int,time:String) {

            //最开始的时候才添加 lineDataSet（一个lineDataSet 代表一条线）
            if (lineDataSet!!.entryCount == 0) {
                lineData!!.addDataSet(lineDataSet)
            }
            lineChart!!.data = lineData
            //避免集合数据过多，及时清空（做这样的处理，并不知道有没有用，但还是这样做了）
            if (timeList.size > 11) {
                timeList.clear()
            }

            timeList.add(time)

            val entry = Entry(lineDataSet!!.entryCount.toFloat(), number.toFloat())
            lineData!!.addEntry(entry, 0)
            //通知数据已经改变
            lineData!!.notifyDataChanged()
            lineChart!!.notifyDataSetChanged()
            //设置在曲线图中显示的最大数量
            lineChart!!.setVisibleXRangeMaximum(10f)
            //移到某个位置
            lineChart!!.moveViewToX((lineData!!.entryCount - 5).toFloat())
        }

        /**
         * 设置Y轴值
         *
         * @param max
         * @param min
         * @param labelCount
         */
        fun setYAxis(max: Float, min: Float, labelCount: Int) {
            if (max < min) {
                return
            }
            leftAxis!!.axisMaximum = max
            leftAxis!!.axisMinimum = min
            leftAxis!!.setLabelCount(labelCount, false)

            rightAxis!!.axisMaximum = max
            rightAxis!!.axisMinimum = min
            rightAxis!!.setLabelCount(labelCount, false)
            lineChart!!.invalidate()
        }

        /**
         * 设置描述信息
         *
         * @param str
         */
        fun setDescription(str: String) {
            val description = Description()
            description.text          = str
            lineChart!!.description   = description
            lineChart!!.invalidate()
        }

        fun remove(){
            lineDataSet!!.removeLast()
        }

        init {
            leftAxis = lineChart!!.axisLeft
            rightAxis = lineChart!!.axisRight
            xAxis = lineChart!!.xAxis
            initLineChart()
            initLineDataSet(name, color)
        }
    }
}


