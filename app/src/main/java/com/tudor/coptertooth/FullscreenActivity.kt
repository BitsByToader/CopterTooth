package com.tudor.coptertooth

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.math.RoundingMode


//TODO: find a way to connect to bluetooth module
//TODO: send data over bluetooth to helicopter
//TODO: (not important) animate the seek bar when user presses takeoff/land buttons

class FullscreenActivity : AppCompatActivity(), SensorEventListener {

    //Fullscreen TextView -- used for dev purposes
    private lateinit var fullscreen: TextView

    //Seek bar -- altitude level of helicopter
    private lateinit var altitude: SeekBar

    //Gyroscope
    private lateinit var mSensorManager: SensorManager
    private var mGyro: Sensor? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mGyro= mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

        setContentView(R.layout.activity_fullscreen)

        // Hide UI first (that shouldn't be there since it's not needed, but I', not bothered enough to delete it
        supportActionBar?.hide()

        mSensorManager.registerListener(this,mGyro, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {

        super.onPostCreate(savedInstanceState)

        //Retrieve the seek bar
        altitude = findViewById<SeekBar>(R.id.altitude_level)

        //Retrieve the fullscreen text view
        fullscreen = findViewById<TextView>(R.id.fullscreen_content)

        //Find the takeoff button and set a click listener on it
        findViewById<Button>(R.id.takeoff_button).setOnClickListener  {
            //show toast
            val toast = Toast.makeText(applicationContext, "Incepem decolarea in 3...2...1...", Toast.LENGTH_LONG)
            toast.show()

            //send takeoff message to helicopter
            altitude.progress = 50
        }

        //Find the land button and set a click listener on it
        findViewById<Button>(R.id.land_button).setOnClickListener  {
            //show toast
            val toast = Toast.makeText(applicationContext, "Incepem aterizarea in 3...2...1...", Toast.LENGTH_LONG)
            toast.show()

            //send land message to helicopter
            altitude.progress = 0
        }

        //Find the calibrate button and set a click listener on it
        findViewById<Button>(R.id.calibrate_button).setOnClickListener  {
            //TODO: make a popup that will direct the user to calibrate the gyroscope
            //TODO: make a method of calibrating the gyro to the user's preferred holding position using the popup
        }

        //Set the listener for the seekbar
        altitude.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onStopTrackingTouch(altitude: SeekBar) {
                //TODO: this method will have to call the function that sends the value over bluetooth
            }

            override fun onStartTrackingTouch(altitude: SeekBar) {
                //Don't think I'll have to use this tbh
            }

            override fun onProgressChanged( altitude: SeekBar, progress: Int, fromUser: Boolean ) {
            }
        })
    }

    override fun onResume() {
        super.onResume()

        mSensorManager.registerListener(this,mGyro, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()

        mSensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do something here if sensor accuracy changes.
        // Won't probably happen tbh
    }

    override fun onSensorChanged(event: SensorEvent) {
        // The light sensor returns a single value.
        // Many sensors return 3 values, one for each axis.
        // Do something with this sensor value.
        var test: Float = event.values[1]
        //toBigDecimal().setScale(8, RoundingMode.UP)
        fullscreen.setText(test.toString())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

}
