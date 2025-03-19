package com.example.fragments_test

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment

class SecondFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout
        val view = inflater.inflate(R.layout.fragment_second, container, false)

        // Get references to UI elements
        val etNumber1 = view.findViewById<EditText>(R.id.etNumber1)
        val etNumber2 = view.findViewById<EditText>(R.id.etNumber2)
        val btnAdd = view.findViewById<Button>(R.id.btnSub)
        val tvResult = view.findViewById<TextView>(R.id.tvResult)

        // Set click listener for the button
        btnAdd.setOnClickListener {
            val num1 = etNumber1.text.toString().toDoubleOrNull()
            val num2 = etNumber2.text.toString().toDoubleOrNull()

            if (num1 != null && num2 != null) {
                val result = num1 - num2;
                tvResult.text = "Result: $result"
            } else {
                tvResult.text = "Please enter valid numbers"
            }
        }

        return view
    }
}
