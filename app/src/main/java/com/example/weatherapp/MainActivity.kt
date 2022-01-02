package com.example.weatherapp

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
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
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.example.weatherapp.modelx.WeatherResponse
import com.example.weatherapp.network.WeatherService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    private lateinit var customDialog: Dialog

    private lateinit var mSharedPreferences: SharedPreferences

    //lateinit the text views that will be attached to the incoming data

    private lateinit var tv_main: TextView

    private lateinit var tv_main_description: TextView

    private lateinit var iv_main: ImageView


    private lateinit var tv_temp: TextView

    private lateinit var tv_humidity: TextView

    private lateinit var tv_sunrise_time: TextView
    private lateinit var tv_sunset_time: TextView

    private lateinit var tv_min: TextView
    private lateinit var tv_max: TextView

    private lateinit var tv_speed: TextView
    private lateinit var tv_speed_unit: TextView

    private lateinit var tv_name: TextView
    private lateinit var tv_country: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Context.MODE_PRIVATE ensures that the data stored can only be visible to this application or all
        // application that share the same user ID
        mSharedPreferences = getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)



        tv_main = findViewById(R.id.tv_main)
        tv_main_description = findViewById(R.id.tv_main_description)
        iv_main = findViewById(R.id.iv_main)
        tv_temp = findViewById(R.id.tv_temp)
        tv_sunrise_time = findViewById(R.id.tv_sunrise_time)
        tv_sunset_time = findViewById(R.id.tv_sunset_time)
        tv_min = findViewById(R.id.tv_min)
        tv_max = findViewById(R.id.tv_max)
        tv_speed = findViewById(R.id.tv_speed)
        tv_speed_unit = findViewById(R.id.tv_speed_unit)
        tv_country = findViewById(R.id.tv_country)
        tv_name = findViewById(R.id.tv_name)
        tv_humidity = findViewById(R.id.tv_humidity)
        setUpUI()

        if (!isLocationEnabled()) {
            Toast.makeText(this, "Check your settings", Toast.LENGTH_SHORT).show()

            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            Dexter.withActivity(this).withPermissions(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report!!.areAllPermissionsGranted()) {
                        requestLocationData()

                    }

                    if (report!!.isAnyPermissionPermanentlyDenied) {
                        Toast.makeText(
                            this@MainActivity, "My Boy", Toast.LENGTH_SHORT
                        ).show()
                    }

                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    showRationaleDialogueForPremissions()
                }
            }).onSameThread()
                .check()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {

        var mLocationRequest = com.google.android.gms.location.LocationRequest()
        mLocationRequest.priority =
            com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallBack,
            Looper.myLooper()
        )
    }

    private val mLocationCallBack = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            super.onLocationResult(p0)
            val mLastLocation: Location = p0.lastLocation
            val latitude = mLastLocation.latitude
            Log.i("Current Latitude", "$latitude")

            val longitude = mLastLocation.longitude
            Log.i("Current Longitude", "$longitude")
            getLocationDetails(latitude, longitude)
        }

    }

    private fun getLocationDetails(latitude: Double, longitude: Double) {
        if (Constants.isNetworkAvailable(this)) {
            Toast.makeText(
                this, "Network is Available",
                Toast.LENGTH_SHORT
            )
                .show()

            val retrofit: Retrofit = Retrofit.Builder().baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()


            var service: WeatherService = retrofit
                .create<WeatherService>(WeatherService::class.java)

            val listCall: Call<WeatherResponse> =
                service.getWeather(latitude, longitude, Constants.METRIC_UNIT, Constants.APP_ID)

            showProgressDialog()
            listCall.enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(response: Response<WeatherResponse>?, retrofit: Retrofit?) {
                    if (response!!.isSuccess) {
                        cancelProgressBar()
                        val weatherList: WeatherResponse = response.body()

                        val weatherResponseJsonString = Gson().toJson(weatherList)

                        val editor = mSharedPreferences.edit()
                        editor.putString(Constants.Weather_Response_Data, weatherResponseJsonString)
                        editor.apply()

                        setUpUI()

                        Log.i("Response", "$weatherList")
                    } else {
                        val rc = response.code()
                        when (rc) {
                            400 -> {
                                Log.i("Error 400", "Bad Connection")
                            }
                            404 -> {
                                Log.i("404", "Not Found")
                            }
                            else -> {
                                Log.i("Error", "Generic")
                            }
                        }
                    }

                }

                override fun onFailure(t: Throwable?) {
                    cancelProgressBar()
                    Log.i("Errorrrr", t!!.message.toString())
                }

            })

        } else {
            Toast.makeText(
                this, "Network is unavailable",
                Toast.LENGTH_SHORT
            )
                .show()
        }

    }

    private fun showRationaleDialogueForPremissions() {
        AlertDialog.Builder(this)
            .setMessage("Check Permissions")
            .setPositiveButton(
                "Go to Settings"
            ) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("Package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("cancel") { dialog,
                                           _ ->
                dialog.dismiss()
            }.show()

    }


    private fun isLocationEnabled(): Boolean {

        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showProgressDialog() {
        customDialog = Dialog(this@MainActivity)
        customDialog.setContentView(R.layout.custom_dialogue)
        customDialog.show()

    }

    private fun cancelProgressBar() {
        customDialog.dismiss()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                requestLocationData()
                true
            }
            else -> return super.onOptionsItemSelected(item)


        }

    }


    /*
    * The function below will be used to setup the UI that will display
    * the all the data that will be called
    * */


    @RequiresApi(Build.VERSION_CODES.N)
    private fun setUpUI() {

        val weatherResponseJsonString =
            mSharedPreferences.getString(Constants.Weather_Response_Data, "")

        if (!weatherResponseJsonString.isNullOrEmpty()) {

            val weatherList = Gson().fromJson(weatherResponseJsonString, WeatherResponse::class.java)


            for (i in weatherList.weather.indices) {
                Log.i("Weather name", weatherList.weather.toString())


                tv_main.text = weatherList.weather[i].main
                tv_main_description.text = weatherList.weather[i].description

                // getUint will be a function that gives us the unit of the users location
                tv_temp.text =
                    weatherList.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())
                tv_humidity.text = weatherList.main.humidity.toString() + "%"


/*
            set the sunrise and sunset text views also call the unixTime function to convert
            the data to human readable format


*/


                tv_sunrise_time.text = unixTime(weatherList.sys.sunrise)
                tv_sunset_time.text = unixTime(weatherList.sys.sunset)


                /*
                * Set up the minimum and max maximum temperatures below
                * */

                tv_min.text = weatherList.main.temp_min.toString() + "min"
                tv_max.text = weatherList.main.temp_max.toString() + "max"


                /*
                * Set up the wind speed and the unit
                * */

                tv_speed.text = weatherList.wind.speed.toString()
                tv_speed_unit.text = weatherList.wind.deg.toString() + "m/h"

                /*
                * set up the name and country of the user
                * */


                tv_name.text = weatherList.name
                tv_country.text = weatherList.sys.country


                /*
                * Use when to update the icon to reflect various weather conditions
                * */

                when (weatherList.weather[i].icon) {
                    "01d" -> iv_main.setImageResource(R.drawable.sunny)
                    "10d" -> iv_main.setImageResource(R.drawable.rain)
                    "13d" -> iv_main.setImageResource(R.drawable.snowflake)
                    "13n" -> iv_main.setImageResource(R.drawable.snowflake)
                    "11d" -> iv_main.setImageResource(R.drawable.storm)
                    "02d" -> iv_main.setImageResource(R.drawable.cloud)
                    "03d" -> iv_main.setImageResource(R.drawable.cloud)
                    "04d" -> iv_main.setImageResource(R.drawable.cloud)
                    "04n" -> iv_main.setImageResource(R.drawable.cloud)
                    "01n" -> iv_main.setImageResource(R.drawable.cloud)
                    "02n" -> iv_main.setImageResource(R.drawable.cloud)
                    "03n" -> iv_main.setImageResource(R.drawable.cloud)
                    "10n" -> iv_main.setImageResource(R.drawable.cloud)
                    "11n" -> iv_main.setImageResource(R.drawable.rain)
                    "50d" -> iv_main.setImageResource(R.drawable.mist)

                }

            }
        }


    }


    private fun getUnit(value: String): String? {
        var value = "°C"

        if ("US" == value || "LR" == value || "MM" == value) {
            value = "°F"
        }
        return value

    }


    /*
    * The function below will convert the sunrise and sunset data to human readable information
    * */

    @RequiresApi(Build.VERSION_CODES.N)
    private fun unixTime(timex: Long): String? {
        val date = Date(timex * 1000L)

        val dateFormat = SimpleDateFormat("HH:mm", Locale.UK)
        dateFormat.timeZone = TimeZone.getDefault()
        return dateFormat.format(date)

    }


}

