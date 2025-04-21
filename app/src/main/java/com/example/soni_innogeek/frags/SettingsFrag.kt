package com.example.soni_innogeek.frags

import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.soni_innogeek.R
import com.example.soni_innogeek.StormAlertService
import com.example.soni_innogeek.databinding.FragmentSettingsBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.Manifest

class SettingsFrag : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var modeReference: DatabaseReference
    private val NOTIFICATION_PERMISSION_CODE = 123
    private var isUpdatingFromFirebase = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.getReference("panelAngle")
        modeReference = firebaseDatabase.getReference("mode")

        setupUI()
        setupListeners()
        fetchInitialData()
    }

    private fun setupUI() {
        updateServiceStatus()
    }

    private fun setupListeners() {
        binding.startServiceButton.setOnClickListener {
            handleServiceToggle()
        }

        binding.materialSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromFirebase) handleModeSwitch(isChecked)
        }

        binding.toggle.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromFirebase) handleCleaningToggle(isChecked)
        }

        binding.changehoriangle.setOnClickListener { showNumberPickerDialog("horizontal") }
        binding.changevertiangle.setOnClickListener { showNumberPickerDialog("vertical") }
    }

    private fun handleServiceToggle() {
        if (isServiceRunning()) {
            stopStormService()
        } else {
            checkNotificationPermission()
        }
        updateServiceStatus()
    }

    private fun isServiceRunning(): Boolean {
        val manager = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Int.MAX_VALUE).any {
            it.service.className == StormAlertService::class.java.name
        }
    }

    private fun updateServiceStatus() {
        val isRunning = isServiceRunning()
        binding.statusTextView.text = "Storm Alert Service Status: ${if (isRunning) "Running" else "Not Running"}"
        binding.startServiceButton.text = if (isRunning) "Stop Storm Alert" else "Start Storm Alert"
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
                return
            }
        }
        startStormService()
    }

    private fun startStormService() {
        ContextCompat.startForegroundService(
            requireContext(),
            Intent(requireContext(), StormAlertService::class.java)
        )
        updateServiceStatus()
    }

    private fun stopStormService() {
        requireActivity().stopService(
            Intent(requireContext(), StormAlertService::class.java)
        )
        updateServiceStatus()
    }

    private fun handleModeSwitch(isChecked: Boolean) {
        if (isChecked) {
            modeReference.setValue(1)
            binding.toggle.isChecked = false
        } else {
            modeReference.setValue(0)
        }
    }

    private fun handleCleaningToggle(isChecked: Boolean) {
        if (isChecked) {
            modeReference.setValue(2)
            binding.materialSwitch.isChecked = false
        } else {
            modeReference.setValue(0)
        }
    }

    private fun fetchInitialData() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot.child("x_axis").getValue(Int::class.java)?.let {
                    binding.horiAngle.text = "$it°"
                }
                dataSnapshot.child("y_axis").getValue(Int::class.java)?.let {
                    binding.vertiAngle.text = "$it°"
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                showToast("Failed to load angles")
            }
        })

        modeReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isUpdatingFromFirebase = true
                when (snapshot.getValue(Int::class.java) ?: 0) {
                    0 -> resetModeSwitches()
                    1 -> setApparatusMode()
                    2 -> setCleaningMode()
                }
                isUpdatingFromFirebase = false
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to load mode status")
            }
        })
    }

    private fun resetModeSwitches() {
        binding.materialSwitch.isChecked = false
        binding.toggle.isChecked = false
    }

    private fun setApparatusMode() {
        binding.materialSwitch.isChecked = true
        binding.toggle.isChecked = false
    }

    private fun setCleaningMode() {
        binding.materialSwitch.isChecked = false
        binding.toggle.isChecked = true
    }

    private fun showNumberPickerDialog(angleType: String) {
        AlertDialog.Builder(requireContext()).apply {
            val dialogView = layoutInflater.inflate(R.layout.dialog_number_picker, null)
            val numberPicker = dialogView.findViewById<NumberPicker>(R.id.numberPicker).apply {
                minValue = 0
                maxValue = 360
            }

            setView(dialogView)
            setPositiveButton("Set") { _, _ ->
                handleAngleSelection(angleType, numberPicker.value)
            }
            setNegativeButton("Cancel", null)
            create().show()
        }
    }

    private fun handleAngleSelection(angleType: String, value: Int) {
        when (angleType) {
            "horizontal" -> {
                databaseReference.child("x_axis").setValue(value)
                binding.horiAngle.text = "$value°"
            }
            "vertical" -> {
                databaseReference.child("y_axis").setValue(value)
                binding.vertiAngle.text = "$value°"
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_CODE && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startStormService()
            } else {
                showToast("Notification permission denied - alerts may not work")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}