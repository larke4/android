package com.example.android

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class Gps : LocationListener, AppCompatActivity() {

    private val LOG_TAG: String = "GPS_ACTIVITY"
    private val LOG_FILE_NAME = "location_log.json"

    companion object {
        private const val PERMISSION_REQUEST_ACCESS_LOCATION = 100
    }

    private lateinit var locationManager: LocationManager
    private lateinit var tvLat: TextView
    private lateinit var tvLon: TextView
    private lateinit var tvAlt: TextView
    private lateinit var tvTime: TextView
    private lateinit var btnGetLocation: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gps)

        locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        tvLat = findViewById(R.id.tvLatitude)
        tvLon = findViewById(R.id.tvLongitude)
        tvAlt = findViewById(R.id.tvAltitude)
        tvTime = findViewById(R.id.tvTime)
        btnGetLocation = findViewById(R.id.btnGetLocation)

        btnGetLocation.setOnClickListener {
            updateCurrentLocation()
        }
        findViewById<Button>(R.id.btn_back_gps).setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        updateCurrentLocation()
    }

    override fun onPause() {
        super.onPause()
        locationManager.removeUpdates(this)
    }

    private fun updateCurrentLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {

                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions()
                    return
                }

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 10f, this)
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 10f, this)

                val lastLocationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                val lastLocationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                val lastLocation = when {
                    lastLocationGPS == null -> lastLocationNet
                    lastLocationNet == null -> lastLocationGPS
                    lastLocationGPS.time > lastLocationNet.time -> lastLocationGPS
                    else -> lastLocationNet
                }

                if (lastLocation != null) {
                    onLocationChanged(lastLocation)
                } else {
                    Toast.makeText(this, "поиск спутника", Toast.LENGTH_SHORT).show()
                }

            } else {
                Toast.makeText(applicationContext, "Включите геолокацию в настройках", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PERMISSION_REQUEST_ACCESS_LOCATION
        )
    }

    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_ACCESS_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateCurrentLocation()
            } else {
                Toast.makeText(applicationContext, "разрешения отклонены, геолокация не будет", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    override fun onLocationChanged(location: Location) {
        Log.d(LOG_TAG, "Location received: Lat=${location.latitude}")

        val sdf = SimpleDateFormat("HH:mm:ss dd.MM.yyyy", Locale.getDefault())
        val dateTime = sdf.format(Date(location.time))

        tvLat.text = "Latitude: ${location.latitude}"
        tvLon.text = "Longitude: ${location.longitude}"
        tvAlt.text = "Altitude: ${String.format("%.2f м", location.altitude)}"
        tvTime.text = "Time: $dateTime"

        writeLocationToJson(location)
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}



    private fun writeLocationToJson(location: Location) {
        try {
            val jsonObject = JSONObject().apply {
                put("timestamp", location.time)
                put("datetime", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date(location.time)))
                put("latitude", location.latitude)
                put("longitude", location.longitude)
                put("altitude", location.altitude)
            }

            val jsonString = jsonObject.toString() + "\n"

            val file = File(filesDir, LOG_FILE_NAME)

            file.appendText(jsonString)

        } catch (e: IOException) {
            Log.e(LOG_TAG, "Ошибка записи файла", e)
        }
    }
}