package com.example.soni_innogeek.frags

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.soni_innogeek.databinding.FragmentEfficiencyCardBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class efficiencyCardFrag : Fragment() {

    private var _binding: FragmentEfficiencyCardBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: FirebaseDatabase
    private lateinit var powerRef: DatabaseReference
    private lateinit var modeRef: DatabaseReference

    private lateinit var efficiencyChart: LineChart
    private val efficiencyEntries = mutableListOf<Pair<Float, Long>>()
    private var dataPointIndex = 0f

    private var maxEfficiency = Float.MIN_VALUE
    private var minEfficiency = Float.MAX_VALUE

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEfficiencyCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        efficiencyChart = binding.efficiencyChart
        setupChart()

        database = FirebaseDatabase.getInstance()
        powerRef = database.getReference("SensorData/Power")
        modeRef = database.getReference("mode")

        powerRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val power = snapshot.getValue(Float::class.java) ?: return
                checkEfficiency(power)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun checkEfficiency(power: Float) {
        val ideal = 85f
        val efficiency = (power / ideal) * 100f

        binding.currentefficiency.text = "Current Efficiency: %.2f%%".format(efficiency)

        if (efficiency > maxEfficiency) {
            maxEfficiency = efficiency
            binding.maxefficiency.text = "Max Efficiency: %.2f%%".format(maxEfficiency)
        }

        if (efficiency < minEfficiency) {
            minEfficiency = efficiency
            binding.minefficiency.text = "Min Efficiency: %.2f%%".format(minEfficiency)
        }

        if (efficiency < 75f) {
            modeRef.setValue("2")
            Toast.makeText(requireContext(), "Cleaning Activated", Toast.LENGTH_SHORT).show()
            sendNotification()
        }

        updateGraph(efficiency)
    }

    private fun sendNotification() {
        val channelId = "solar_efficiency_channel"
        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Solar Efficiency Alerts", NotificationManager.IMPORTANCE_HIGH)
            channel.description = "Notifications for solar efficiency drops"
            channel.enableLights(true)
            channel.lightColor = Color.RED
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(requireContext(), channelId)
            .setContentTitle("Alert! Solar panel efficiency is low.")
            .setContentText("Cleaning mechanism triggered.")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }

    private fun updateGraph(efficiency: Float) {
        val timestamp = System.currentTimeMillis()
        efficiencyEntries.add(Pair(dataPointIndex, timestamp))

        val entry = Entry(dataPointIndex++, efficiency)
        val lineData = efficiencyChart.data ?: return
        val dataSet = lineData.getDataSetByIndex(0) as LineDataSet

        dataSet.addEntry(entry)

        if (dataSet.entryCount > 20) {
            dataSet.removeEntry(0) // compatible with API 24+
            efficiencyEntries.removeAt(0) // compatible with API 24+
        }

        lineData.notifyDataChanged()
        efficiencyChart.data = lineData
        efficiencyChart.notifyDataSetChanged()

        efficiencyChart.xAxis.valueFormatter = TimestampAxisFormatter(efficiencyEntries)
        efficiencyChart.invalidate()
    }

    private fun setupChart() {
        val entries = ArrayList<Entry>()
        val dataSet = LineDataSet(entries, "Efficiency %")
        dataSet.color = Color.BLUE
        dataSet.setDrawCircles(false)
        dataSet.lineWidth = 2f
        dataSet.setDrawValues(false)

        val lineData = LineData(dataSet)
        efficiencyChart.data = lineData

        efficiencyChart.description.isEnabled = false
        efficiencyChart.setTouchEnabled(true)
        efficiencyChart.setScaleEnabled(true)

        val xAxis = efficiencyChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setAvoidFirstLastClipping(true)

        val leftAxis = efficiencyChart.axisLeft
        val rightAxis = efficiencyChart.axisRight
        rightAxis.isEnabled = false

        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = 100f
    }

    inner class TimestampAxisFormatter(private val timestamps: List<Pair<Float, Long>>) : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val timestamp = timestamps.find { it.first == value }?.second ?: return ""
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
