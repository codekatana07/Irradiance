package com.example.soni_innogeek.frags

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.soni_innogeek.databinding.FragmentTempCardBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class TempCardFrag : Fragment() {
    private var _binding: FragmentTempCardBinding? = null
    private val binding get() = _binding!!
    val calendar = Calendar.getInstance().apply {
        set(2025, 3, 22, 10, 0, 0) // Month is 0-based (April = 3)
    }
    val timestamp = calendar.timeInMillis
    private val database = Firebase.database("https://surya-mukhi-default-rtdb.asia-southeast1.firebasedatabase.app/")
    private lateinit var temperatureChart: BarChart
    private val entries = ArrayList<BarEntry>()
    private val hourlyTemps = mutableMapOf<Int, MutableList<Float>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTempCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChart()
        setupWeatherData()
        setupTemperatureData()
    }

    private fun setupChart() {
        temperatureChart = binding.temperatureChart.apply {
            setBackgroundColor(Color.parseColor("#212121"))
            description.text = "Hourly Temperature (°C)"
            description.textColor = Color.WHITE
            legend.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.WHITE
                granularity = 1f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val hour = value.toInt() % 24
                        return String.format("%02d:00", hour)
                    }
                }
            }

            axisLeft.apply {
                textColor = Color.WHITE
                axisMinimum = 0f
                setDrawGridLines(false)
            }

            axisRight.isEnabled = false
        }
    }

    private fun setupWeatherData() {
        database.getReference("WeatherData").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val maxTemp = snapshot.child("max_temperature").getValue(Float::class.java)
                val minTemp = snapshot.child("min_temperature").getValue(Float::class.java)
                val currentTempText = snapshot.child("temperature").getValue(Float::class.java)

                binding.apply {
                    maxtemp.text = "Max: ${maxTemp?.format(1) ?: "--"}°C"
                    minimumTemp.text = "Min: ${minTemp?.format(1) ?: "--"}°C"
                    currentTemp.text = "Current: ${currentTempText?.format(1) ?: "--"}°C"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Weather data error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupTemperatureData() {
        database.getReference("WeatherData1").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                processHourlyData(snapshot)
                updateChart()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Temp data error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun processHourlyData(snapshot: DataSnapshot) {
        Log.d("FirebaseData", "Total children: ${snapshot.childrenCount}")
        hourlyTemps.clear()
        entries.clear()

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val cutoffTime = System.currentTimeMillis() - 12 * 3600 * 1000

        val dateFormats = listOf(
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        )

        for (child in snapshot.children) {
            try {
                val temp = child.child("temperature").getValue(Float::class.java) ?: continue

                Log.d("FirebaseData", "Processing child: ${child.key}")
                Log.d("FirebaseData", "Raw timestamp: ${child.child("timestamp").value}")
                Log.d("FirebaseData", "Parsed timestamp: $timestamp")
                Log.d("FirebaseData", "Temperature: $temp")
                val timestamp = child.child("timestamp").let { tsSnapshot ->
                    when (val value = tsSnapshot.value) {
                        is Long -> value
                        is String -> dateFormats.firstNotNullOfOrNull { format ->
                            try { format.parse(value)?.time } catch (e: Exception) { null }
                        } ?: value.toLongOrNull()
                        else -> null
                    }
                }

                // Handle null temperature with safe cast and default value


                if (timestamp == null) {
                    Log.w("Debug", "Invalid timestamp: ${child.key}")
                    continue  // Fixed the unresolved label issue
                }

                if (timestamp > cutoffTime) {
                    calendar.timeInMillis = timestamp
                    val hour = calendar.get(Calendar.HOUR_OF_DAY)
                    val relativeHour = (hour - currentHour + 24) % 24

                    // Add non-null temp to list
                    hourlyTemps.getOrPut(relativeHour) { mutableListOf() }.add(temp)
                    Log.d("ChartData", "Total entries processed: ${entries.size}")
                    Log.d("ChartData", "First entry: ${entries.firstOrNull()?.y}°C at ${entries.firstOrNull()?.x}")
                }
            } catch (e: Exception) {
                Log.e("Error", "Processing ${child.key}: ${e.message}")
            }
        }

        entries.add(BarEntry(0f, 21f))
        entries.add(BarEntry(1f, 20f))
        entries.add(BarEntry(2f, 18f))
        entries.add(BarEntry(3f, 17f))
        entries.add(BarEntry(4f, 18f))
        entries.add(BarEntry(5f, 19f))
        entries.add(BarEntry(6f, 20f))
        entries.add(BarEntry(7f, 20f))
        updateChart()


        // Create chart entries
        for (i in 0 until 12) {
            val avgTemp = hourlyTemps[i]?.average()?.toFloat() ?: 0f
            entries.add(BarEntry(i.toFloat(), avgTemp))
        }
    }

    private fun updateChart() {
        val dataSet = BarDataSet(entries, "Temperature").apply {
            color = Color.parseColor("#2196F3")
            valueTextColor = Color.WHITE
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.roundToInt().toString()
                }
            }
        }

        temperatureChart.apply {
            data = BarData(dataSet)
            setVisibleXRangeMaximum(12f)
            moveViewToX(entries.lastOrNull()?.x ?: 0f)
            animateY(1000)
            invalidate()
        }
    }

    private fun Float.format(decimals: Int): String = "%.${decimals}f".format(this)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}