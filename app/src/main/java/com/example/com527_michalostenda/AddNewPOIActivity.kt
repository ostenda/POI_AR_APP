package com.example.com527_michalostenda

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf

class AddNewPOIActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_new_poi)

        val poi_name= findViewById<EditText>(R.id.POI_name);
        val poi_type = findViewById<EditText>(R.id.POI_type);
        val poi_description = findViewById<EditText>(R.id.POI_description);
        val btn_poisave = findViewById<Button>(R.id.btn_save_poi)

        btn_poisave.setOnClickListener {
            val name = poi_name.text.toString();
            val type = poi_type.text.toString();
            val description = poi_description.text.toString();

            sendBackPoi(name,type,description);

        }

    }

    fun sendBackPoi (name:String, type:String, description:String) {
        val intent = Intent()
        val bundle = bundleOf("com.app.new-poi_name" to name)
        val bundle2 = bundleOf("com.app.new-poi_type" to type)
        val bundle3= bundleOf("com.app.new-poi_description" to description)

        intent.putExtras(bundle)
        intent.putExtras(bundle2)
        intent.putExtras(bundle3)

        setResult(RESULT_OK, intent)
        finish()
    }
}