package com.example.soni_innogeek.frags

import android.graphics.Color
import android.os.Bundle
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
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class tempCardFrag : Fragment() {
    private var _binding: FragmentTempCardBinding? = null
    private val binding get() = _binding!!

    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference

    // Chart-related variables
    private lateinit var temperatureChart: LineChart
    private val entries = ArrayList<Entry>()
    private lateinit var dataSet: LineDataSet

    // Temperature variables
    private var maxTemperature: Float? = null
    private var minTemperature: Float? = null
    private var currentTemperature: Float? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTempCardBinding.inflate(inflater, container, false)
        val view = binding.root

        // Initialize Firebase
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.getReference("solar_data")

        // Initialize chart
        temperatureChart = binding.temperatureChart
        setupChart()
        setupChartUpdates()

        // Get temperature data
        getTemperatureData()

        return view
    }

    private fun setupChart() {
        dataSet = LineDataSet(entries, "Temperature (°C)").apply {
            color = ColorTemplate.MATERIAL_COLORS[0]
            valueTextColor = Color.WHITE
            lineWidth = 2f
            setCircleColor(Color.WHITE)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        temperatureChart.apply {
            data = LineData(dataSet)
            description.text = "Live Temperature Data"
            description.textColor = Color.WHITE
            legend.textColor = Color.WHITE

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.WHITE
                granularity = 1f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(value.toLong()))
                    }
                }
            }

            axisLeft.apply {
                textColor = Color.WHITE
                axisMinimum = 0f
            }

            axisRight.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(true)
            animateY(1000)
        }
    }

    private fun setupChartUpdates() {
        databaseReference.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousKey: String?) = processData(snapshot)
            override fun onChildChanged(snapshot: DataSnapshot, previousKey: String?) = processData(snapshot)
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousKey: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun processData(snapshot: DataSnapshot) {
        val time = snapshot.child("datetime").getValue(Long::class.java)?.toFloat() ?: return
        val temp = snapshot.child("temperature").getValue(Float::class.java) ?: return

        entries.add(Entry(time, temp))
        entries.sortBy { it.x }

        dataSet.notifyDataSetChanged()
        temperatureChart.data.notifyDataChanged()
        temperatureChart.invalidate()
    }

    private fun getTemperatureData() {
        FirebaseDatabase.getInstance().getReference("WeatherData")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    currentTemperature = snapshot.child("temperature").getValue(Float::class.java)
                    maxTemperature = snapshot.child("max_temperature").getValue(Float::class.java)
                    minTemperature = snapshot.child("min_temperature").getValue(Float::class.java)
                    updateTemperatureUI()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Failed to load temperature: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateTemperatureUI() {
        binding.apply {
            currentTemp.text = "Current: ${currentTemperature ?: "--"}°C"
            maxtemp.text = "Max: ${maxTemperature ?: "--"}°C"
            minimumTemp.text = "Min: ${minTemperature ?: "--"}°C"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}