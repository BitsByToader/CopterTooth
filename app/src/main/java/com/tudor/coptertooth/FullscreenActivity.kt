package com.tudor.coptertooth

import android.app.AlertDialog
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.beepiz.bluetooth.gattcoroutines.GattConnection
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.*
import java.lang.Exception


//TODO: find a way to connect to bluetooth module
//TODO: send data over bluetooth to helicopter
//TODO: (not important) animate the seek bar when user presses takeoff/land buttons

private const val TAG = "GattServerActivity"
private const val REQUEST_ENABLE_BT = 1
private const val SCAN_PERIOD: Long = 10000

class FullscreenActivity : AppCompatActivity(), SensorEventListener, CoroutineScope {

    //Coroutines stuff for maintaining the life cycle of the GATT Connection
    private var blConnectionJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + blConnectionJob

    //Fullscreen TextView -- used for dev purposes
    private lateinit var fullscreen: TextView

    //Seek bar -- altitude level of helicopter
    private lateinit var altitude: SeekBar

    //Gyroscope
    private lateinit var mSensorManager: SensorManager
    private var mGyro: Sensor? = null

    //Dialog alert
    private lateinit var calibrationDialogBuilder: AlertDialog.Builder
    private lateinit var alert: AlertDialog

    //Calibrated values of gyroscope
    private var calState = -1
    private var calibratedValues = CalibrationData(0.toFloat(),0.toFloat(),0.toFloat(),0.toFloat(),0.toFloat(),0.toFloat())

    private val baseBluetoothUuidPostfix = "0000-1000-8000-00805f9b34fb"

    //MAC Address of MLT-BT05
    private val iBks12MacAddress = "54:4A:16:6F:47:9A"
    private val defaultDeviceMacAddress = iBks12MacAddress

    //Bluetooth
    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled
    private lateinit var bluetoothLeHelicopter: BluetoothDevice
    private lateinit var helicopterConnection: GattConnection
    private var bleConnectionState: Boolean = false
    private lateinit var mainCharacteristic: BluetoothGattCharacteristic


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fullscreen)

        // Hide UI first (that shouldn't be there since it's not needed, but I', not bothered enough to delete it
        supportActionBar?.hide()

        //Create a sensor manager and gyroscope instance
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mGyro= mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

        //Register the listener for the gyroscope
        mSensorManager.registerListener(this,mGyro, SensorManager.SENSOR_DELAY_NORMAL)

        //Create the calibration dialog
        calibrationDialogBuilder = AlertDialog.Builder(this)
        calibrationDialogBuilder.setMessage("Acum veti calibra senzorul. Apasati butonul Gata pentru a incepe.")
            .setCancelable(false)
            .setPositiveButton("Gata", null)
        alert = calibrationDialogBuilder.create()
        alert.setTitle("Calibrare")

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        bluetoothAdapter?.takeIf { it.isDisabled }?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        //Find the bluetooth helicopter based on its MAC address and create a GATT instance with it
        bluetoothLeHelicopter = bluetoothAdapter!!.getRemoteDevice(defaultDeviceMacAddress)
        helicopterConnection = GattConnection.invoke(bluetoothLeHelicopter)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {

        super.onPostCreate(savedInstanceState)

        //Retrieve the seek bar
        altitude = findViewById<SeekBar>(R.id.altitude_level)

        //Retrieve the fullscreen text view
        fullscreen = findViewById<TextView>(R.id.fullscreen_content)

        //Find the takeoff button and set a click listener on it
        findViewById<Button>(R.id.takeoff_button).setOnClickListener  {
            if (bleConnectionState) {
                //show toast
                val toast = Toast.makeText(applicationContext, "Incepem decolarea in 3...2...1...", Toast.LENGTH_LONG)
                toast.show()

                //send takeoff message to helicopter
                altitude.progress = 50
            } else {
                Toast.makeText(applicationContext, "Mai intai trebuie sa va conectati la elicopter!", Toast.LENGTH_LONG).show()
            }
        }

        //Find the land button and set a click listener on it
        findViewById<Button>(R.id.land_button).setOnClickListener  {
            if ( bleConnectionState) {
                //show toast
                val toast = Toast.makeText(applicationContext, "Incepem aterizarea in 3...2...1...", Toast.LENGTH_LONG)
                toast.show()

                //send land message to helicopter
                altitude.progress = 0

                launch {
                    var tosend: String = "12346578"
                    mainCharacteristic.value = tosend.toByteArray()
                    helicopterConnection.writeCharacteristic(mainCharacteristic)
                    Log.d(TAG, "Sent with a size of: ${tosend.toByteArray().size}")
                }
            } else {
                Toast.makeText(applicationContext, "Mai intai trebuie sa va conectati la elicopter!", Toast.LENGTH_LONG).show()
            }
        }

        //Find the calibrate button and set a click listener on it
        findViewById<Button>(R.id.calibrate_button).setOnClickListener  {
            alert.show()
        }

        //Find the connect button and set a click listener to it
        findViewById<Button>(R.id.connect_button).setOnClickListener {
            if ( bleConnectionState) {
                helicopterConnection.close()
                findViewById<Button>(R.id.connect_button).text = "Conectare"
                bleConnectionState = false
            } else {
                launch {
                    helicopterConnection.connect()
                    bleConnectionState = true
                    findViewById<Button>(R.id.connect_button).text = "Deconectare"
                    val services = helicopterConnection.discoverServices()
                    //Code bellow is highly wank and innefficient because it searches every charact until it finds the correct one
                    //I am doing this because I haven't found a way (yet) to instantiate a BluetoothGATTCharacteristic with only the UUID
                    services.forEach {
                        it.characteristics.forEach {
                            try {
                                if ( it.uuid == uuidFromShortCode16("FFE1") ) {
                                    mainCharacteristic = it
                                }
                            } catch (e: Exception) {
                                Log.d(TAG, "Couldn't read charast with uuid: ${it.uuid}", e)
                            }
                        }
                    }
                }
            }

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

        //Override the listener for the positive dialog button so it won't dismiss the dialog upon the press
        alert.setOnShowListener {
            alert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                when(calState) {
                    -1 -> { //Uncalibrated, move on to the calibration ~~ getting the base position of the phone
                        alert.setMessage("Tineti telefonul intr-o pozitie comfortabila.")
                    }
                    0 -> alert.setMessage("Miscati telefonul in pozitia de acceleratie maxima.") //Maximum acceleration position
                    1 -> alert.setMessage("Tineti telefonul in pozitia in care elicopterul va frana.") //Braking position
                    2 -> alert.setMessage("Miscati telefonul in pozitia in care elicopterul va face stanga.") //Maximum left position
                    3 -> alert.setMessage("Miscati telefonul in pozitia in care elicopterul va face dreapta.") //Maximum right position
                    4 -> { //Calibrated
                        calState = -2
                        alert.dismiss()
                        fullscreen.setText("Inclinati telefonul pentru a controla elicopterul")
                        alert.setMessage("Acum veti calibra senzorul. Apasati butonul Gata pentru a incepe.")
                    }
                }
                calState++
            }
        }
    }

    override fun onResume() {
        super.onResume()

        mSensorManager.registerListener(this,mGyro, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()

        mSensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        blConnectionJob.cancel()
        helicopterConnection.close()
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do something here if sensor accuracy changes.
        // Won't probably happen tbh
    }

    override fun onSensorChanged(event: SensorEvent) {
        //toBigDecimal().setScale(8, RoundingMode.UP)
        if ( calState != -1 ) {
            when(calState) {
                0 -> {
                    calibratedValues.baseValueY = event.values[1]
                    calibratedValues.baseValueZ = event.values[2]
                }
                1 -> calibratedValues.maxForwardPos = event.values[1]
                2 -> calibratedValues.maxBackwardPos = event.values[1]
                3 -> calibratedValues.maxLeftPos = event.values[2]
                4 -> calibratedValues.maxRightPos = event.values[2]
            }
        }
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

    fun uuidFromShortCode16(shortCode16: String): UUID {
        return UUID.fromString("0000" + shortCode16 + "-" + baseBluetoothUuidPostfix);
    }
}

data class CalibrationData(var baseValueY: Float, var baseValueZ: Float,
                           var maxForwardPos: Float, var maxBackwardPos: Float,
                           var maxLeftPos: Float, var maxRightPos: Float)

/*Log.d("CalibData", "baseValueY = ${calibratedValues.baseValueY}\n")
  Log.d("CalibData", "baseValueZ = ${calibratedValues.baseValueZ}\n")
  Log.d("CalibData", "maxForwardPos = ${calibratedValues.maxForwardPos}\n")
  Log.d("CalibData", "maxBackwardPos = ${calibratedValues.maxBackwardPos}\n")
  Log.d("CalibData", "maxLeftPos = ${calibratedValues.maxLeftPos}\n")
  Log.d("CalibData", "maxRightPos = ${calibratedValues.maxRightPos}\n")*/
