package com.example.soni_innogeek.frags

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import com.example.soni_innogeek.LoginActivity
import com.example.soni_innogeek.MainActivity
import com.example.soni_innogeek.R
import com.example.soni_innogeek.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth


class ProfileFrag : Fragment() {
    private lateinit var _binding : FragmentProfileBinding
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater,container,false)
        binding.vertSettings.setOnClickListener {
            showPopupMenu(it)
        }

        return binding.root
    }

    // Function to show the popup menu
    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.menu, popupMenu.menu)

        // Handle menu item clicks
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.edit_profile -> {
                    // Replace fragment on edit profile click
                    (activity as MainActivity).replaceFragment(editProfileFrag())
                    true
                }
                R.id.log_out -> {
                    // Log out the user from Firebase and redirect to login screen
                    logoutUser()
                    true
                }
                else -> false
            }
        }

        // Show the popup menu
        popupMenu.show()
    }

    private fun logoutUser() {
        // Sign out from Firebase
        FirebaseAuth.getInstance().signOut()

        // Redirect to LoginActivity
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
    }
}