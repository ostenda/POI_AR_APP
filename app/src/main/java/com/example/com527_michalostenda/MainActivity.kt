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

class MainActivity : AppCompatActivity(), LocationListener {

    lateinit var map1: MapView

    var showPOI = false
    var autoupload = false;

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

        val markerGestureListener = object:ItemizedIconOverlay.OnItemGestureListener<OverlayItem>
        {
            override fun onItemLongPress(i: Int, item:OverlayItem ) : Boolean
            {
                Toast.makeText(this@MainActivity, item.snippet, Toast.LENGTH_SHORT).show()
                return true
            }

            override fun onItemSingleTapUp(i: Int, item:OverlayItem): Boolean
            {
                Toast.makeText(this@MainActivity, item.snippet, Toast.LENGTH_SHORT).show()
                return true
            }
        }
        val items = ItemizedIconOverlay(this, arrayListOf<OverlayItem>(), markerGestureListener)
        val fernhurst = OverlayItem("Fernhurst", "Village in West Sussex", GeoPoint(51.05, -0.72))
        val blackdown = OverlayItem("Blackdown", "highest point in West Sussex", GeoPoint(51.0581, -0.6897))
        items.addItem(fernhurst)
        items.addItem(blackdown)
        map1.overlays.add(items)
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

         /*   R.id.choosemap -> {
                val intent = Intent(this,MapChooseActivity::class.java)
                startActivity(intent)
                return true
            }
*/
            R.id.create_poi -> {
                val intent = Intent(this,AddNewPOIActivity::class.java)
                PoiLauncher.launch(intent)
                return true
            }
            R.id.showsavedSQL -> {
                loadDataFromDatabase()
            }

        }
        return false
    }


    val PoiLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // The lambda function starts here
        // Check that we get an OK (success) result from the second activity
        if (it.resultCode == RESULT_OK) {
            it.data?.apply {
                val new_poi_name = this.getStringExtra("com.app.new-poi_name")
                val new_type = this.getStringExtra("com.app.new-poi_type")
                val new_description = this.getStringExtra("com.app.new-poi_description")



                val user = OverlayItem(new_poi_name, "You are currently here", GeoPoint(currentLatitude, currentLongitude))
                val items = ItemizedIconOverlay(this@MainActivity, arrayListOf<OverlayItem>(), null)
                items.addItem(user)
                map1.overlays.add(items)


                if (new_poi_name != null) {
                    if(new_type != null){
                        if(new_description !=null){
                            insertDataToDatabase(new_poi_name,new_type,new_description)
                            if(autoupload) {
                                insertWebData(new_poi_name,new_type,new_description)
                            }
                        } else{
                            Toast.makeText(this@MainActivity,"Point of interest added to the map as marker", Toast.LENGTH_SHORT).show()
                        }
                    }
                }


            }
        }
        // The lambda function ends here
    }

    fun insertWebData(new_poi_name: String, new_type: String, new_description: String){

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
                                .setPositiveButton("OK", null) // add an OK button with an optional event handler
                                .setMessage(item.snippet) // set the message
                                .show() // show the dialog
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