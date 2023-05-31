package com.example.com527_michalostenda


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.OverlayItem
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.json.responseJson // for JSON - uncomment when needed
import com.github.kittinunf.fuel.gson.responseObject // for GSON - uncomment when needed
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result

class MainActivity : AppCompatActivity(), LocationListener {


    lateinit var map1: MapView

    var showPOI = false
    var autoupload = false;
    var shopWebPoi = false;


    var currentLatitude = 0.0;
    var currentLongitude = 0.0;


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This line sets the user agent, a requirement to download OSM maps
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));

        setContentView(R.layout.activity_main)
        map1 = findViewById<MapView>(R.id.map1)
        map1.controller.setZoom(16.0)
        map1.controller.setCenter(GeoPoint(51.05, -0.72))

        requestPermissions()


    }



    //Menu handler
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu,menu)

        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.preferences -> {
                val intent = Intent(this,PreferencesActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.create_poi -> {
                val intent = Intent(this,AddNewPOIActivity::class.java)
                poiLauncher.launch(intent)
                return true
            }
            R.id.showsavedSQL -> {
                loadDataFromDatabase()
            }
            R.id.showdataweb -> {
                loadWebData()
            }
            R.id.saveSQL -> {

            }
        }
        return false
    }

    private fun insertDataToDatabase(name: String, type: String, description: String) {


        val db = POIDatabase.getDatabase(application)

        val newpoi = POIdata(0, name, type, description, currentLatitude, currentLongitude);

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db.PointofintrestsDao().insert(newpoi)
            }

        }

    }


    val poiLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Check that we get an OK (success) result from the second activity
        if (result.resultCode == RESULT_OK) {
            result.data?.let { data ->
                val newPoiName = data.getStringExtra("com.app.new-poi_name")
                val newType = data.getStringExtra("com.app.new-poi_type")
                val newDescription = data.getStringExtra("com.app.new-poi_description")

                val user = OverlayItem(newPoiName, "You are here", GeoPoint(currentLatitude, currentLongitude))
                val items = ItemizedIconOverlay(this@MainActivity, arrayListOf<OverlayItem>(), null)
                items.addItem(user)
                map1.overlays.add(items)

                if (newPoiName != null && newType != null && newDescription != null) {
                    insertDataToDatabase(newPoiName, newType, newDescription)
                    if (autoupload) {
                            insertWebData(newPoiName, newType, newDescription)
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Point of interest added to the map as marker", Toast.LENGTH_SHORT).show()
                    }
                }

        }
    }



    private fun loadWebData(){
        shopWebPoi = true;

        Toast.makeText(this@MainActivity, "Bringing data from web server", Toast.LENGTH_SHORT).show()

        val baseUrl = "http://10.0.2.2:3000"
        val url = "$baseUrl/poi/all"

        url.httpGet().responseJson { request, response, result ->
            when(result) {
                is Result.Success -> {
                    val jsonArray = result.get().array()
                    var str = ""

                    val markerGestureListener = object:ItemizedIconOverlay.OnItemGestureListener<OverlayItem>
                    {
                        override fun onItemLongPress(i: Int, item:OverlayItem ) : Boolean
                        {
                            Handler(Looper.getMainLooper()).post {

                                android.app.AlertDialog.Builder(this@MainActivity)
                                   .setPositiveButton("OK", null)
                                    .setMessage(item.snippet)
                                    .create()
                                    .show()
                            }
                            return true
                        }

                        override fun onItemSingleTapUp(i: Int, item:OverlayItem): Boolean
                        {
                            Toast.makeText(this@MainActivity, item.snippet, Toast.LENGTH_SHORT).show()
                            return true
                        }
                    }

                    val items = ItemizedIconOverlay(this@MainActivity, arrayListOf<OverlayItem>(), markerGestureListener)

                    for(i in 0 until jsonArray.length()) {
                        val curObj = jsonArray.getJSONObject(i)
                        str += "Point of Intrest: ${curObj.getString("name")} Type: ${curObj.getString("type")}  Description: ${curObj.getString("description")}}\n"

                        val new_item = OverlayItem(curObj.getString("name"), "Point of Intrest: ${curObj.getString("name")} Type: ${curObj.getString("type")}  Description: ${curObj.getString("description")}\n", GeoPoint(curObj.getDouble("lat"), curObj.getDouble("lon")))
                        items.addItem(new_item)
                        map1.overlays.add(items)

                    }
                }
                is Result.Failure -> {
                    android.app.AlertDialog.Builder(this@MainActivity)
                        .setPositiveButton("OK", null)
                        .setMessage("ERROR ${result.error.message}")
                        .show()
                }
            }
        }
    }



    private fun insertWebData(name: String, type: String, description: String) {

        val baseUrl = "http://10.0.2.2:3000"
        val url = "$baseUrl/poi/create"

        val postData = listOf("name" to name, "type" to type, "description" to description, "lat" to currentLatitude.toDouble(), "lon" to currentLongitude.toDouble())
        url.httpPost(postData).response { request, response, result ->
            when (result) {
                is Result.Success -> {
                    Toast.makeText(this@MainActivity, result.get().decodeToString(), Toast.LENGTH_LONG).show()
                }

                is Result.Failure -> {
                    Toast.makeText(this@MainActivity, result.error.message, Toast.LENGTH_LONG).show()
                }
            }
        }

    }

    private fun loadDataFromDatabase() {

        showPOI = true;

        val db = POIDatabase.getDatabase(application)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val pointsofintrests = db.PointofintrestsDao().getAllpois()


                val markerGestureListener = object:ItemizedIconOverlay.OnItemGestureListener<OverlayItem>
                {
                    override fun onItemLongPress(i: Int, item:OverlayItem ) : Boolean
                    {
                        Handler(Looper.getMainLooper()).post {

                            android.app.AlertDialog.Builder(this@MainActivity)
                                .setPositiveButton("OK", null) //
                                .setMessage(item.snippet) //
                                .create()
                                .show() //
                        }

                        return true
                    }

                    override fun onItemSingleTapUp(i: Int, item:OverlayItem): Boolean
                    {
                        Toast.makeText(this@MainActivity, item.snippet, Toast.LENGTH_SHORT).show()
                        return true
                    }
                }


                val items = ItemizedIconOverlay(this@MainActivity, arrayListOf<OverlayItem>(), markerGestureListener)
                pointsofintrests.forEach {

                    val new_item = OverlayItem(it.name, it.name + " Type: " + it.type + " Description: " + it.description, GeoPoint(it.lat, it.lon))
                    items.addItem(new_item)
                    map1.overlays.add(items)

                }
            }
        }
    }

    fun requestPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                0
            )
        } else {
            startGps()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            0 -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startGps()
                } else {
                    AlertDialog.Builder(this).setPositiveButton("OK", null)
                        .setMessage("GPS will not work as you have denied access").show()
                }
            }
        }
    }

    // Suppress lint check (sanity check) about missing permission
    // We check permissions above, so don't need to do it here
    @SuppressLint("MissingPermission")
    fun startGps() {
        val mgr = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        mgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0f, this)
    }

    override fun onLocationChanged(loc: Location) {
        currentLatitude = loc.latitude;
        currentLongitude = loc.longitude;



        val user = OverlayItem("Me", "You are currently here", GeoPoint(loc.latitude, loc.longitude))
        user.setMarker(ContextCompat.getDrawable(this, R.drawable.rsmall))
        val items = ItemizedIconOverlay(this, arrayListOf<OverlayItem>(), null)

        items.addItem(user)
        map1.overlays.add(items)


        map1.controller.setCenter(GeoPoint(loc.latitude, loc.longitude))
    }

    //onResume saves autoupload preference
    override fun onResume() {
        super.onResume()
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this@MainActivity)
        autoupload = prefs.getBoolean("autoupload", false) ?: false

    }

    override fun onProviderEnabled(provider: String) {

    }

    override fun onProviderDisabled(provider: String) {

    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

    }
}