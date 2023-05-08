package com.example.com527_michalostenda

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.widget.Button
import android.view.View
import androidx.core.os.bundleOf

class MapChooseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_choose)

        val regular = findViewById<Button>(R.id.btnRegular)
        regular.setOnClickListener {
            sendBackMapChoice(false)
        }
        val opentopomap = findViewById<Button>(R.id.btnOpenTopoMap)
        opentopomap.setOnClickListener {
            sendBackMapChoice(true)
        }
    }

    fun sendBackMapChoice(opentopo: Boolean) {
        val intent = Intent()
        val bundle = bundleOf("com.example.OpenStreetMap" to opentopo)
        intent.putExtras(bundle)
        setResult(RESULT_OK, intent)
        finish()
    }
}
