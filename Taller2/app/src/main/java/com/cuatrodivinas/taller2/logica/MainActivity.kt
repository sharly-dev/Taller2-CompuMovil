package com.cuatrodivinas.taller2.logica

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cuatrodivinas.taller2.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()
        binding.btnContactos.setOnClickListener {
            val intent = Intent(this, ContactosActivity::class.java)
            startActivity(intent)
        }
        binding.btnCamara.setOnClickListener {
            val intent = Intent(this, ImageActivity::class.java)
            startActivity(intent)
        }
        binding.btnMaps.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }
    }
}