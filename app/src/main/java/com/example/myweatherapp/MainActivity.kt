package com.example.myweatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.getSystemService
import com.example.myweatherapp.models.WeatherResponse
import com.example.myweatherapp.network.WeatherService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.gson.annotations.SerializedName
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit.*
import java.text.SimpleDateFormat
import java.util.*


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private var mProgressDialog: Dialog? = null


    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var mLatitude: Double = 0.0
    // A global variable for Current Longitude
    private var mLongitude: Double = 0.0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this@MainActivity)


        if (!isLocationEnabled()) {
            Toast.makeText(
                this,
                "Your location Provider(GPS) is turned off. Please turn it on",
                Toast.LENGTH_SHORT
            )
                .show()

            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            Dexter.withContext(this@MainActivity).withPermissions(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report!!.areAllPermissionsGranted()) {
                            requestLocationData()
                        }
                        if (report!!.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(
                                this@MainActivity,
                                "Permission for location denied",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogForPermissions()
                    }
                }).onSameThread().check()
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun getLocationWeatherDetails() {
        if (Constants.isNetworkAvailable(this)) {
            val retrofit: Retrofit = Retrofit.Builder().baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build()

            val service: WeatherService =
                retrofit.create<WeatherService>(WeatherService::class.java)
            val listCall: Call<WeatherResponse> = service.getWeather(
                mLatitude, mLongitude, Constants.METRIC_UNIT, Constants.APP_ID
            )

            showCustomProgressDialog()

            listCall.enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(response: Response<WeatherResponse>?, retrofit: Retrofit?) {
                    if (response!!.isSuccess) {
                        hideProgressDialog()
                        val weatherList: WeatherResponse = response.body()
                        Log.i("Response Result", "$weatherList")
                        setupUI(weatherList)
                    } else {
                        val rc = response.code()
                        when (rc) {
                            400 -> {
                                Log.e("Error 400", "Bad connection")
                            }

                            404 -> {
                                Log.e("Error 404", "Not found")
                            }

                            else -> {
                                Log.e("Error", "Generic error")
                            }
                        }
                    }
                }

                override fun onFailure(t: Throwable?) {
                    Log.e("Error!!!!!!!!", t!!.message.toString())
                    hideProgressDialog()
                }

            })
        } else {
            Toast.makeText(this@MainActivity, "No internet connection", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton(
                "GO TO SETTINGS"
            ) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog,
                                           _ ->
                dialog.dismiss()
            }.show()
    }


    private fun showCustomProgressDialog() {
        mProgressDialog = Dialog(this)

        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)

        mProgressDialog!!.show()
    }

    private fun hideProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog!!.dismiss()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {

        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY



        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            val mLastLocation: Location = locationResult!!.lastLocation
             mLatitude = mLastLocation.latitude
            Log.i("Current Latitude", "$mLatitude")

            mLongitude = mLastLocation.longitude
            Log.i("Current Longitude", "$mLongitude")
            getLocationWeatherDetails()

            mFusedLocationClient.removeLocationUpdates(this)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // TODO (STEP 7: Now finally, make an api call on item selection.)
            // START
            R.id.action_refresh -> {
                getLocationWeatherDetails()
                true
            }
            else -> super.onOptionsItemSelected(item)
            // END
        }
    }

    private fun getUnit(value: String): String? {
        Log.i("unitttttt", value)
        var value = "°C"
        if ("US" == value || "LR" == value || "MM" == value) {
            value = "°F"
        }
        return value
    }

    private fun unixTime(timex: Long): String? {
        val date = Date(timex * 1000L)
        @SuppressLint("SimpleDateFormat") val sdf =
            SimpleDateFormat("HH:mm:ss")
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }

    private fun setupUI(weatherList: WeatherResponse) {
        // For loop to get the required data. And all are populated in the UI.
        for (z in weatherList.weather.indices) {
            Log.i("NAME", weatherList.weather[z].main)

            val tv_main = findViewById<TextView>(R.id.tv_main)
            val tv_main_description = findViewById<TextView>(R.id.tv_main_description)
            val tv_temp = findViewById<TextView>(R.id.tv_temp)
            val tv_humidity = findViewById<TextView>(R.id.tv_humidity)
            val tv_min = findViewById<TextView>(R.id.tv_min)
            val tv_max = findViewById<TextView>(R.id.tv_max)
            val tv_speed = findViewById<TextView>(R.id.tv_speed)
            val tv_name = findViewById<TextView>(R.id.tv_name)
            val tv_country = findViewById<TextView>(R.id.tv_country)
            val tv_sunrise_time = findViewById<TextView>(R.id.tv_sunrise_time)
            val tv_sunset_time = findViewById<TextView>(R.id.tv_sunset_time)
            val iv_main : ImageView = findViewById(R.id.iv_main)



            tv_main.text = weatherList.weather[z].main
            tv_main_description.text = weatherList.weather[z].description
            tv_temp.text = weatherList.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())
            // Here we update the main icon
            tv_humidity.text = weatherList.main.humidity.toString() + " per cent"
            tv_min.text = weatherList.main.tempMin.toString() + " min"
            tv_max.text = weatherList.main.tempMax.toString() + " max"
            tv_speed.text = weatherList.wind.speed.toString()
            tv_name.text = weatherList.name
            tv_country.text = weatherList.sys.country
            tv_sunrise_time.text = unixTime(weatherList.sys.sunrise.toLong())
            tv_sunset_time.text = unixTime(weatherList.sys.sunset.toLong())

            // Here we update the main icon
            when (weatherList.weather[z].icon) {
                "01d" -> iv_main.setImageResource(R.drawable.sunny)
                "02d" -> iv_main.setImageResource(R.drawable.cloud)
                "03d" -> iv_main.setImageResource(R.drawable.cloud)
                "04d" -> iv_main.setImageResource(R.drawable.cloud)
                "04n" -> iv_main.setImageResource(R.drawable.cloud)
                "10d" -> iv_main.setImageResource(R.drawable.rain)
                "11d" -> iv_main.setImageResource(R.drawable.storm)
                "13d" -> iv_main.setImageResource(R.drawable.snowflake)
                "01n" -> iv_main.setImageResource(R.drawable.cloud)
                "02n" -> iv_main.setImageResource(R.drawable.cloud)
                "03n" -> iv_main.setImageResource(R.drawable.cloud)
                "10n" -> iv_main.setImageResource(R.drawable.cloud)
                "11n" -> iv_main.setImageResource(R.drawable.rain)
                "13n" -> iv_main.setImageResource(R.drawable.snowflake)
            }
        }
    }
}







