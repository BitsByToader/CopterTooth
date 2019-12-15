package com.tudor.coptertooth

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


//TODO: find a way to connect to bluetooth module
//TODO: send data over bluetooth to helicopter
//TODO: make a listener for the seekbar

class FullscreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fullscreen)

        // Hide UI first (that shouldn't be there since it's not needed, but I', not bothered enough to delete it
        supportActionBar?.hide()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        //Retrieve the seekbar
        val altitude = findViewById<SeekBar>(R.id.altitude_level)

        super.onPostCreate(savedInstanceState)

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
        /*altitude?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(altitude: SeekBar, progress: Int, fromUser: Boolean) {
                // Write code to perform some action when progress is changed.

            }

            override fun onStartTrackingTouch(altitude: SeekBar) {
                // Write code to perform some action when touch is started.
            }

            override fun onStopTrackingTouch(altitude: SeekBar) {
                // Write code to perform some action when touch is stopped.

            }
        })*/
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
