package com.okravi.loconotes.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.okravi.loconotes.R
import com.okravi.loconotes.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private var binding : ActivityMainBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding?.root)

    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}