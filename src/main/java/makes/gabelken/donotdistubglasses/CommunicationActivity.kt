package makes.gabelken.donotdistubglasses

import android.app.NotificationManager
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.*
import android.provider.Settings
import android.support.constraint.ConstraintLayout
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityCompat.startActivityForResult
import android.support.v4.content.ContextCompat
import android.support.v4.content.ContextCompat.getSystemService
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_communication.*
import java.io.IOException
import java.io.InputStream
import java.util.*

class CommunicationActivity : AppCompatActivity() {

    // Static variables
    companion object {
        // Variables used to initialize and maintain a connection over bluetooth
        var m_myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        var m_BluetoothSocket: BluetoothSocket? = null
        lateinit var m_progress: ProgressDialog
        lateinit var m_bluetoothAdapter: BluetoothAdapter
        lateinit var m_address: String

        // The state of the system
        var m_isConnected: Boolean = false // True if connected successfully to a device
        var m_isEnabled: Boolean = false //  True if accepting messages from an Arduino

        // State of the glasses
        var m_areGlassesOn: Boolean = false

        // References to system objects
        lateinit var container_ref: ConstraintLayout
        lateinit var notificationManager: NotificationManager

        // Variables used to handle incoming messages from the Arduino
        private var recordedDataInputStr: StringBuilder = StringBuilder() //Message storage
        var m_bluetoothInHandler: Handler = object : Handler() {
            override fun handleMessage(msg: Message) { // Attempt to parse incoming strings received over the bluetooth socket
                if (msg.what == HANDLER_STATE) {  //if message is what we want
                    val strIn =
                        msg.obj as String // msg.arg1 = bytes from connect thread
                    recordedDataInputStr.append(strIn) //keep appending to string until part of the message looks like "<#>"
                    val startOfLineIndex: Int = recordedDataInputStr.indexOf('<') // Find index of expected start character if it exists
                    val endOfLineIndex: Int = recordedDataInputStr.indexOf('>') // Find index of expected end character if it exists
                    Log.i("BT_HANDLER","String start-end: $startOfLineIndex / $endOfLineIndex")

                    // If the start and end characters exist, parse out a string that matches
                    if (startOfLineIndex >= 0 && endOfLineIndex > 0) {
                        Log.i("BT_HANDLER","Data Received = $recordedDataInputStr")
                        var dataInPrint: String = recordedDataInputStr.substring(startOfLineIndex, endOfLineIndex+1) // extract string
                        Log.i("BT_HANDLER","Parsed Data = $dataInPrint") //Make sure it looks good in the log

                        // Parse out the command bit between delimiters
                        val commandStr: String = recordedDataInputStr.substring(
                            startOfLineIndex+1,
                            endOfLineIndex
                        )
                        Log.i("BT_HANDLER","Command = $commandStr")
                        val command : Int = commandStr.toInt() // Change the command to an int
                        onGlassesStateChange(command == 1) // Handle the data

                        recordedDataInputStr.delete(0, recordedDataInputStr.length) //clear all string data
                        dataInPrint = " "
                    }
                }
            }
        }
        const val HANDLER_STATE = 0 //Const with expected settings for message handler

        /**
         * When a new command comes from the glasses, update the phone's do not disturb mode accordingly
         */
        fun onGlassesStateChange(glassesOn: Boolean) {
            m_areGlassesOn = glassesOn // Store glasses state in case we need it later
            if (m_areGlassesOn ) {
                notificationManager.onDOD() // If the glasses are on, enable Do Not Disturb
            }
            else {
                notificationManager.offDOD() // If the glasses are off, disable Do Not Disturb
            }
            Log.d("Glasses", "Glasses are on is $m_areGlassesOn")
        }

        // Extension function to turn on do not disturb
        fun NotificationManager.onDOD(){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                this.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            }
        }

        // Extension function to turn off do not disturb
        private fun NotificationManager.offDOD(){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                this.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        }

        // Extension function to show toast message
        private fun Context.toast(message: String) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }


    // Initialize the page
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_communication)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        container_ref = container

        // Get the address of the device passed in by the selector page and attempt to connect
        m_address = intent.getStringExtra(SelectDeviceActivity.EXTRA_ADDRESS)
        var connection = ConnectToDevice(this).execute()

        // Set enabled state to match app toggle
        m_isEnabled = enable_updates_toggle.isChecked
        // On toggling the enable/disable button, update the stored value and send the update to the arduino
        enable_updates_toggle.setOnClickListener {
            m_isEnabled = enable_updates_toggle.isChecked
            sendCommand(m_isEnabled)
        }

        // Set up notification access needed for do not disturb mode if available. Warn the user if they cannot.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (notificationManager.isNotificationPolicyAccessGranted){
                //toast("Notification policy access granted.")
            }else{
                toast("You need to grant notification policy access.")
                // If notification policy access not granted for this package
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                startActivityForResult(intent, 1)
            }
        }else{
            toast("Device does not support this feature.")
        }

        // On clicking the back button, disconnect from the device
        back_button.setOnClickListener { disconnect() }
    }

    /**
     * On disconnecting from the device, close up the socket, reset static variables, and return to the device selections page.
     */
    private fun disconnect() {
        if(m_BluetoothSocket != null) {
            try {
                sendCommand(false)
                m_BluetoothSocket!!.close()
                m_BluetoothSocket = null
                m_isConnected = false
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        finish()
    }

    /**
     * Try to send an update over the bluetooth socket of the connected device
     */
    private fun sendCommand(isConnecting: Boolean) {
        if(m_BluetoothSocket != null) {
            var command: Int = if (isConnecting) 1 else 0; // Convert the state to a bit
            var sendStr = "<$command>"; // Setup the string to send
            Log.i("MessageOut", ""+sendStr)

            // Attempt to send the message. Retry a couple times if it doesn't appear to succeed
            for(attempt in 0..2) {
                try {
                    m_BluetoothSocket!!.outputStream.write(sendStr.toByteArray()) //Send
                    break
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Helper class to connect to the Arduino in an async task with a progress dialogue
    private class ConnectToDevice(c: Context) : AsyncTask<Void, Void, String>() {
        private var connectSuccess: Boolean = true
        private val context: Context

        init {
            this.context = c
        }

        override fun onPreExecute() {
            super.onPreExecute()
            m_progress = ProgressDialog.show(context, "Connecting...", "Please Wait")
        }

        // Setup the socket
        override fun doInBackground(vararg p0: Void?): String? {
            try {
                if (m_BluetoothSocket == null || !m_isConnected) {
                    m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    val device: BluetoothDevice = m_bluetoothAdapter.getRemoteDevice(m_address)
                    m_BluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(m_myUUID)
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                    m_BluetoothSocket!!.connect()
                }
            } catch(e: IOException) {
                connectSuccess = false
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if(!connectSuccess) {
                Log.w("data", "Couldn't Connect")
            } else {
                m_isConnected = true
            }
            BluetoothListener().start() //Start listening for data over the bluetooth socket
            m_progress.dismiss()
        }
    }

    // Helper class to listen for incoming commands over the bluetooth socket
    private class BluetoothListener(): Thread() {

        private lateinit var m_InStream: InputStream

        init {
            if(m_BluetoothSocket != null) {
                m_InStream = m_BluetoothSocket!!.inputStream // Set the input stream to that of the bluetooth socket
            }
            else {
                Log.i("BT_Listener", "Error initializing bluetooth listener")
            }
        }

        override fun run() {
            val buffer = ByteArray(256)
            var bytes: Int

            // Until the connection is terminated, any time the app is enabled, read incoming commands
            while(m_isConnected) {
                if(m_isEnabled) {
                    try {
                        bytes = m_InStream.read(buffer) // Read into buffer
                        val readMessage = String(buffer, 0, bytes) //Convert for use by string handler
                        m_bluetoothInHandler.obtainMessage(HANDLER_STATE, bytes, -1, readMessage).sendToTarget() // Parse and handle read data
                    } catch (e: IOException) {
                        break
                    }
                }
            }
        }
    }
}
