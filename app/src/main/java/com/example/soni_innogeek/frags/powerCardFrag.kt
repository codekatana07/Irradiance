package com.example.soni_innogeek.frags

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.soni_innogeek.databinding.FragmentPowerCardBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.util.Calendar

class powerCardFrag : Fragment() {

    private var _binding: FragmentPowerCardBinding? = null
    private val binding get() = _binding!!
    private lateinit var powerChart: LineChart
    private val entries = ArrayList<Entry>()

    // Power values
    private var currentPower: Int = 55
    private var maxPower: Int = 100
    private var minPower: Int = 50

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPowerCardBinding.inflate(inflater, container, false)
        setupChart()
        loadDummyData()
        return binding.root
    }

    private fun setupChart() {
        powerChart = binding.powerChart.apply {
            setBackgroundColor(Color.parseColor("#212121"))
            description.text = "Power Output (W)"
            description.textColor = Color.WHITE
            legend.isEnabled = false

            // Configure X-Axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.WHITE
                granularity = 1f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                        val displayHour = (currentHour - 11 + value.toInt()).let {
                            if (it < 0) it + 24 else it % 24
                        }
                        return String.format("%02d:00", displayHour)
                    }
                }
                axisMinimum = 0f
                axisMaximum = 11f
            }

            // Configure Y-Axis
            axisLeft.apply {
                textColor = Color.WHITE
                axisMinimum = 0f
                axisMaximum = 120f
                setLabelCount(6, true)
                setDrawGridLines(false)
            }

            axisRight.isEnabled = false
        }
    }

    private fun loadDummyData() {
        entries.clear()

        // Dummy power data for 12-hour window
        val powerValues = listOf(50, 55, 65, 75, 85, 95, 100, 95, 85, 75, 65, 55)

        powerValues.forEachIndexed { index, value ->
            entries.add(Entry(index.toFloat(), value.toFloat()))
        }

        updateChart()
        updatePowerUI()
    }

    private fun updatePowerUI() {
        binding.apply {
            currentpower.text = "Current: ${currentPower}W"
            maxpow.text = "Max: ${maxPower}W"
            minpow.text = "Min: ${minPower}W"
        }
    }

    private fun updateChart() {
        val dataSet = LineDataSet(entries, "Power").apply {
            color = Color.parseColor("#FFC107")  // Amber color
            valueTextColor = Color.WHITE
            lineWidth = 2f
            setCircleColor(Color.WHITE)
            circleRadius = 4f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = Color.parseColor("#33FFC107")  // 20% opacity
            fillAlpha = 255
        }

        powerChart.apply {
            data = LineData(dataSet)
            setVisibleXRangeMaximum(12f)
            moveViewToX(11f)
            animateY(1000)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}