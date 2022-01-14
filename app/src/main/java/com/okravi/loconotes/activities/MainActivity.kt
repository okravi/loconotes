package com.okravi.loconotes.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.okravi.loconotes.R
import com.okravi.loconotes.databinding.ActivityMainBinding

private var binding : ActivityMainBinding? = null

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)


    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}