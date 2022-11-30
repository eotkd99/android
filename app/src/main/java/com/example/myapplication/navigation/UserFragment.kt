package com.example.myapplication.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
//import com.google.firebase.auth.FirebaseAuth
import com.example.myapplication.R
import kotlinx.android.synthetic.main.fragment_alarm.view.*
import java.util.*

class UserFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = LayoutInflater.from(activity).inflate(R.layout.fragment_user, container, false)
        return view;
    }
}
