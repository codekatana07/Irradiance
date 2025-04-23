package com.example.soni_innogeek.frags

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.soni_innogeek.databinding.FragmentHumidityCardBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.graphics.toColorInt
import java.util.Calendar

class humidityCardFrag : Fragment() {
    private var _binding: FragmentHumidityCardBinding? = null
    private val binding get() = _binding!!

    private lateinit var humidityChart: LineChart
    private val entries = ArrayList<Entry>()
    private var maxHumidity: Float? = null
    private var minHumidity: Float? = null
    private var currentHumidity: Float? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHumidityCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChart()
        setupHumidityData()
    }

    private fun setupChart() {
        humidityChart = binding.humidityChart.apply {
            setBackgroundColor(Color.parseColor("#212121"))
            description.text = "Humidity Timeline (%)"
            description.textColor = Color.WHITE
            legend.isEnabled = false

            // Configure X-Axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.WHITE
                granularity = 1f
                setDrawAxisLine(true)
                setDrawGridLines(true)
                gridColor = Color.parseColor("#424242")
                axisLineColor = Color.WHITE
                axisMinimum = 0f
                axisMaximum = 12f  // For 12-hour window
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val hour = value.toInt() % 24
                        return String.format("%02d:00", hour)
                    }
                }
            }

            // Configure Y-Axis
            axisLeft.apply {
                textColor = Color.WHITE
                setDrawGridLines(true)
                gridColor = Color.parseColor("#424242")
                axisLineColor = Color.WHITE
                axisMinimum = 0f
                axisMaximum = 100f  // Humidity percentage range
                setLabelCount(10, true)
            }

            axisRight.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(true)

            // Always show empty chart
            setNoDataText("No humidity data available")
            setNoDataTextColor(Color.WHITE)
        }
    }

    private fun setupHumidityData() {
        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("WeatherData1/WeatherData1")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                processHumidityData(snapshot)
                updateChart()
                updateHumidityUI()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Humidity data error: ${error.message}", Toast.LENGTH_SHORT).show()
                // Keep the axes but remove any lines
                entries.clear()
                humidityChart.clear()
                humidityChart.invalidate()
            }
        })
    }

    private fun processHumidityData(snapshot: DataSnapshot) {
        entries.clear()
        maxHumidity = null
        minHumidity = null
        currentHumidity = null
        val cutoffTime = System.currentTimeMillis() - 12 * 3600 * 1000
        Log.d("HumidityDebug", "Cutoff time: ${Date(cutoffTime)}")
        var lastValidTimestamp = 0L

        for (child in snapshot.children) {
            try {
                Log.d("HumidityDebug", "Processing child: ${child.key}")

                // Humidity value check
                val humidityObj = child.child("humidity").value
                val humidity = when (humidityObj) {
                    is Double -> humidityObj.toFloat()
                    is Float -> humidityObj
                    is Int -> humidityObj.toFloat()
                    is Long -> humidityObj.toFloat() // Add this line
                    else -> {
                        Log.d("HumidityDebug", "Skipping invalid humidity type")
                        continue
                    }
                }

                // Timestamp handling
                val tsSnapshot = child.child("timestamp")
                val tsValue = tsSnapshot.value
                val timestamp: Long

                if (tsValue is Long) {
                    timestamp = tsValue
                    Log.d("HumidityDebug", "Long timestamp: $timestamp")
                } else if (tsValue is Int) {
                    timestamp = tsValue.toLong()
                    Log.d("HumidityDebug", "Int timestamp: $timestamp")
                } else if (tsValue is String) {
                    timestamp = try {
                        tsValue.toLong()
                    } catch (e: NumberFormatException) {
                        try {
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                .parse(tsValue)?.time ?: continue
                        } catch (e: Exception) {
                            Log.e("HumidityChart", "Error processing ${child.key}: ${e.message}")
                            continue
                        }
                    }
                    Log.d("HumidityDebug", "String timestamp parsed: $timestamp")
                } else {
                    Log.d("HumidityDebug", "Unknown timestamp type")
                    continue
                }

                Log.d("HumidityDebug", "Processed timestamp: ${Date(timestamp)} ($timestamp)")

                if (timestamp > cutoffTime) {
                    val hoursSinceCutoff = ((timestamp - cutoffTime) / (3600 * 1000)).toFloat()
                    entries.add(Entry(hoursSinceCutoff, humidity))
                    Log.d("HumidityDebug", "Added entry: X=$hoursSinceCutoff, Y=$humidity")
                    updateMinMaxHumidity(humidity)

                    // Track last valid data for current humidity
                    if (timestamp > lastValidTimestamp) {
                        lastValidTimestamp = timestamp
                        currentHumidity = humidity
                    }
                } else {
                    Log.d("HumidityDebug", "Skipping old data: ${Date(timestamp)}")
                }
            } catch (e: Exception) {
                Log.e("HumidityChart", "Error processing ${child.key}: ${e.message}")
            }
        }
        entries.clear()

// Dummy humidity data (0 = oldest hour, 11 = current hour)
        entries.add(Entry(0f, 85f))   // 12 hours ago (high morning humidity)
        entries.add(Entry(1f, 82f))   // 11 hours ago
        entries.add(Entry(2f, 80f))   // 10 hours ago
        entries.add(Entry(3f, 78f))   // 9 hours ago
        entries.add(Entry(4f, 75f))   // 8 hours ago
        entries.add(Entry(5f, 72f))   // 7 hours ago
        entries.add(Entry(6f, 68f))   // 6 hours ago
        entries.add(Entry(7f, 65f))   // 5 hours ago (lowest afternoon)
        entries.add(Entry(8f, 70f))   // 4 hours ago
        entries.add(Entry(9f, 75f))   // 3 hours ago
        entries.add(Entry(10f, 78f))  // 2 hours ago
        entries.add(Entry(11f, 80f))  // Current hour (evening rise)
        currentHumidity = 80f
        maxHumidity = 85f
        minHumidity = 65f

        updateChart()
        updateHumidityUI()
    }

    private fun updateMinMaxHumidity(humidity: Float) {
        maxHumidity = maxHumidity?.let { maxOf(it, humidity) } ?: humidity
        minHumidity = minHumidity?.let { minOf(it, humidity) } ?: humidity
    }

    private fun updateChart() {
        val dataSet = if (entries.isEmpty()) {
            // Create transparent dummy dataset
            LineDataSet(listOf(Entry(0f, 0f)), "").apply {
                color = Color.TRANSPARENT
                setDrawCircles(false)
                setDrawValues(false)
            }
        } else {
            LineDataSet(entries, "Humidity").apply {
                color = Color.parseColor("#4CAF50")
                valueTextColor = Color.WHITE
                lineWidth = 2f
                setCircleColor(Color.parseColor("#8BC34A"))
                circleRadius = 4f
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawFilled(true)
                fillColor = Color.parseColor("#C8E6C9")
                fillAlpha = 100
            }
        }

        humidityChart.apply {
            data = LineData(dataSet)

            // Maintain 12-hour window visualization
            xAxis.axisMinimum = 0f
            xAxis.axisMaximum = 12f

            // Force redraw
            animateX(1000)
            invalidate()
        }
    }

    private fun updateHumidityUI() {
        binding.apply {
            currentHumidityText.text = "Current: ${currentHumidity?.format(1) ?: "--"}%"
            maxhumiditytext.text = "Max: ${maxHumidity?.format(1) ?: "--"}%"
            minHumidityText.text = "Min: ${minHumidity?.format(1) ?: "--"}%"
        }
    }

    private fun Float.format(decimals: Int): String = "%.${decimals}f".format(this)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}