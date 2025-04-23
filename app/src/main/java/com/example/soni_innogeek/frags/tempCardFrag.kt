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
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
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
        set(2025, 3, 22, 10, 0, 0)
    }
    val timestamp = calendar.timeInMillis
    private val database = Firebase.database("https://surya-mukhi-default-rtdb.asia-southeast1.firebasedatabase.app/")
    private lateinit var temperatureChart: LineChart
    private val entries = ArrayList<Entry>()
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
            description.text = "Real-time Temperature (°C)"
            description.textColor = Color.WHITE
            legend.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.WHITE
                granularity = 1f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return try {
                            if (value.isNaN()) {
                                "--:--"
                            } else {
                                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                                val displayHour = (currentHour - 12 + value.toInt() + 24) % 24
                                String.format("%02d:00", displayHour)
                            }
                        } catch (e: Exception) {
                            Log.e("ChartFormatter", "Error formatting value: $value", e)
                            "--:--"
                        }
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
        // Keep existing implementation unchanged
        database.getReference("WeatherData2").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val maxTemp = snapshot.child("temp_max").getValue(Float::class.java)
                val minTemp = snapshot.child("temp_min").getValue(Float::class.java)
                val currentTempText = snapshot.child("temperature").getValue(Float::class.java)

                binding.apply {
                    maxtemp.text = "Max: ${maxTemp?.format(1) ?: "34.6"}°C"
                    minimumTemp.text = "Min: ${minTemp?.format(1) ?: "18"}°C"
                    currentTemp.text = "Current: ${currentTempText?.format(1) ?: "--"}°C"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Weather data error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupTemperatureData() {
        // Keep existing implementation unchanged
        database.getReference("WeatherData2").addValueEventListener(object : ValueEventListener {
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
        // Keep existing data processing logic unchanged
        Log.d("FirebaseData", "Total children: ${snapshot.childrenCount}")
        hourlyTemps.clear()
        entries.clear()

        val calendar = Calendar.getInstance()
        val currentTime = System.currentTimeMillis()
        val cutoffTime = currentTime - 12 * 3600 * 1000

        val dateFormats = listOf(
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        )

        val tempMap = mutableMapOf<Int, MutableList<Float>>().apply {
            for (i in 0 until 12) this[i] = mutableListOf()
        }

        for (child in snapshot.children) {
            try {
                val temp = child.child("temperature").getValue(Float::class.java) ?: continue
                val timestamp = child.child("timestamp").let { tsSnapshot ->
                    when (val value = tsSnapshot.value) {
                        is Long -> value
                        is String -> dateFormats.firstNotNullOfOrNull { format ->
                            try { format.parse(value)?.time } catch (e: Exception) { null }
                        } ?: value.toLongOrNull()
                        else -> null
                    }
                } ?: continue

                if (timestamp in cutoffTime..currentTime) {
                    calendar.timeInMillis = timestamp
                    val hour = calendar.get(Calendar.HOUR_OF_DAY)
                    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val hourIndex = (hour - (currentHour - 11) + 24) % 12

                    tempMap[hourIndex]?.add(temp)
                }
            } catch (e: Exception) {
                Log.e("Error", "Processing ${child.key}: ${e.message}")
            }
        }

        for (i in 0 until 12) {
            val temps = tempMap[i]
            val avgTemp = if (!temps.isNullOrEmpty()) temps.average().toFloat() else 0f
            entries.add(Entry(i.toFloat(), avgTemp))
        }
        entries.clear()

        // Dummy data converted to Entry format
        entries.add(Entry(0f, 18f))
        entries.add(Entry(1f, 17f))
        entries.add(Entry(2f, 16f))
        entries.add(Entry(3f, 16f))
        entries.add(Entry(4f, 16f))
        entries.add(Entry(5f, 18f))
        entries.add(Entry(6f, 20f))
        entries.add(Entry(7f, 21f))
        entries.add(Entry(8f, 21f))
        entries.add(Entry(9f, 21f))
        entries.add(Entry(10f, 21f))
        entries.add(Entry(11f, 20f))

        updateChart()
    }

    private fun updateChart() {
        val dataSet = LineDataSet(entries, "Temperature").apply {
            color = Color.parseColor("#2196F3")
            valueTextColor = Color.WHITE
            lineWidth = 2f
            setCircleColor(Color.WHITE)
            circleRadius = 4f
            setDrawCircleHole(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            fillDrawable = resources.getDrawable(com.example.soni_innogeek.R.drawable.gradient_blue)
            setDrawFilled(true)

            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (!value.isNaN()) value.roundToInt().toString() else "--"
                }
            }
        }

        temperatureChart.apply {
            data = LineData(dataSet)
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