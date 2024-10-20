package com.example.soni_innogeek.frags

import android.os.Bundle
import android.view.FrameStats
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
    private var horizontalAngle: Double? = null
    private var verticalAngle: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val view = binding.root

        val v=  inflater.inflate(R.layout.fragment_settings, container, false)

        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.getReference("SensorData/panelAngle")

        getanglesvalues()




        return v
    }

    private fun getanglesvalues() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                if(snapshot.exists()) {
                    horizontalAngle = snapshot.child("horizontal").getValue(Double::class.java)
                    verticalAngle = snapshot.child("vertical").getValue(Double::class.java)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle any errors
                Toast.makeText(requireContext(), "Failed to get humidity data.", Toast.LENGTH_SHORT).show()
            }
        })

    }
    private fun updateangleUI() {
        // Display the current, max, and min temperatures in the TextViews
        binding.horiAngle.text = horizontalAngle.toString()
        binding.vertiangle.text = verticalAngle.toString()
    }


}