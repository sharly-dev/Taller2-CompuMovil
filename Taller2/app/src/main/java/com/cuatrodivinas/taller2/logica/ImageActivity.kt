package com.cuatrodivinas.taller2.logica

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cuatrodivinas.taller2.databinding.ActivityImageBinding

class ImageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}