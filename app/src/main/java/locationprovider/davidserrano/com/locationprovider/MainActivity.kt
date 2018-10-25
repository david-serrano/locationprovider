package locationprovider.davidserrano.com.locationprovider

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import locationprovider.davidserrano.com.LocationProvider


class MainActivity : AppCompatActivity() {

    private val bothLocationsRequestCode = 1
    private val SETTINGS_ACTION = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        main_start_updates.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkLocationPermission()) {
                    startLocationUpdates()
                }
            } else {
                startLocationUpdates()
            }
        }
    }

    private fun startLocationUpdates() {
        val callback = object : LocationProvider.LocationCallback {
            override fun locationRequestStopped() {
                printOutput("Stopped requesting location")
                enableLocationButton()
            }

            override fun onNewLocationAvailable(latitude: Float, longitude: Float) {
                printOutput("New location available - Lat: $latitude / Lon: $longitude")
            }

            override fun locationServicesNotEnabled() {
                printOutput("Location services are not enabled - please turn them on and try again")
                enableLocationButton()
            }

            override fun updateLocationInBackground(latitude: Float, longitude: Float) {
                printOutput("Location updated in background - Lat: $latitude / Lon: $longitude")
            }

            override fun networkListenerInitialised() {
                printOutput("Network listener started")
            }
        }

        val lProvider: LocationProvider = LocationProvider.Builder()
                .setContext(this)
                .setListener(callback)
                .create()
        disableLocationButton()

        printOutput("\nStarting location updates...")
        lProvider.requestLocation()
    }

    private fun printOutput(s: String) {
        val appendedText = "${main_log.text} \n $s"
        main_log.text = appendedText
    }

    private fun disableLocationButton() {
        main_start_updates.isClickable = false
        main_start_updates.alpha = 0.5f
        main_start_updates.text = getString(R.string.main_requesting_location)
    }

    private fun enableLocationButton() {
        main_start_updates.isClickable = true
        main_start_updates.alpha = 1f
        main_start_updates.text = getString(R.string.start_updates)
    }

    private fun checkLocationPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                showExplanation()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), bothLocationsRequestCode)
            }
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            bothLocationsRequestCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        startLocationUpdates()
                    }
                } else {
                    showExplanation()
                }
                return
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SETTINGS_ACTION && resultCode == Activity.RESULT_OK) {
            startLocationUpdates()
        }
    }

    private fun showExplanation() {
        AlertDialog.Builder(this)
                .setTitle(getString(R.string.alert_title))
                .setMessage(getString(R.string.alert_message))
                .setPositiveButton(getString(R.string.alert_positive_action)) { dialogInterface, i ->
                    dialogInterface.dismiss()
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivityForResult(intent, SETTINGS_ACTION)
                }
                .setNegativeButton(getString(R.string.alert_negative_action)) { dialogInterface, i ->
                    dialogInterface.dismiss()
                }
                .show()
    }
}