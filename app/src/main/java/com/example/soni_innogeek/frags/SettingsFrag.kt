package com.example.soni_innogeek.frags

import android.app.AlertDialog
import android.os.Bundle
import android.view.FrameStats
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.NumberPicker
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import com.example.soni_innogeek.R
import com.example.soni_innogeek.databinding.FragmentHumidityCardBinding
import com.example.soni_innogeek.databinding.FragmentSettingsBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class SettingsFrag : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //avisoni material switch

        var apparatusMode = 0

        binding.materialSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                apparatusMode = 1
                // The switch is checked.
            } else {
                apparatusMode = 0
                // The switch isn't checked.
            }
        }
        binding.toggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                apparatusMode = 2
                // The switch is checked.
            } else {
                apparatusMode = 0
                // The switch isn't checked.
            }
        }
        //
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val view = binding.root

        // Initialize Firebase database reference
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.getReference("panelAngle")

        // Set initial values from Firebase
        fetchAngleDataFromFirebase()

        // Click listeners for change buttons
        binding.changehoriangle.setOnClickListener {
            showNumberPickerDialog("horizontal")
        }

        binding.changevertiangle.setOnClickListener {
            showNumberPickerDialog("vertical")
        }

        return view
    }

    private fun fetchAngleDataFromFirebase() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val horizontalAngle = dataSnapshot.child("x_axis").getValue(Int::class.java)
                val verticalAngle = dataSnapshot.child("y_axis").getValue(Int::class.java)

                // Update UI with angles from Firebase
                if (horizontalAngle != null) {
                    binding.horiAngle.text = "$horizontalAngle°"
                }
                if (verticalAngle != null) {
                    binding.vertiAngle.text = "$verticalAngle°"
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(context, "Failed to load angles.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showNumberPickerDialog(angleType: String) {
        // Build the dialog
        val builder = AlertDialog.Builder(requireContext())
        val inflater: LayoutInflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_number_picker, null)
        builder.setView(dialogView)

        val numberPicker = dialogView.findViewById<NumberPicker>(R.id.numberPicker)
        numberPicker.minValue = 1
        numberPicker.maxValue = 360

        val btnOk = dialogView.findViewById<Button>(R.id.btnOk)
        val dialog = builder.create()

        btnOk.setOnClickListener {
            val selectedAngle = numberPicker.value

            when (angleType) {
                "horizontal" -> {
                    databaseReference.child("x_axis").setValue(selectedAngle)
                    binding.horiAngle.text = "$selectedAngle°"
                }
                "vertical" -> {
                    databaseReference.child("y_axis").setValue(selectedAngle)
                    binding.vertiAngle.text = "$selectedAngle°"
                }
            }

            dialog.dismiss()
        }

        dialog.show() // Show the dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}