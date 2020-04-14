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

    companion object {
        var m_myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        var m_BluetoothSocket: BluetoothSocket? = null
        lateinit var m_progress: ProgressDialog
        lateinit var m_bluetoothAdapter: BluetoothAdapter

        var m_isConnected: Boolean = false
        var m_isEnabled: Boolean = false
        lateinit var m_address: String

        var m_areGlassesOn: Boolean = false
        lateinit var container_ref: ConstraintLayout
        lateinit var notificationManager: NotificationManager


        private var recordedDataInputStr: StringBuilder = StringBuilder()
        var m_bluetoothInHandler: Handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                if (msg.what == HANDLER_STATE) {                                        //if message is what we want
                    val strIn =
                        msg.obj as String // msg.arg1 = bytes from connect thread
                    recordedDataInputStr.append(strIn) //keep appending to string until ~
                    val startOfLineIndex: Int = recordedDataInputStr.indexOf('<')
                    val endOfLineIndex: Int = recordedDataInputStr.indexOf('>') // determine the end-of-line
                    Log.i("BT_HANDLER","String start-end: $startOfLineIndex / $endOfLineIndex")
                    if (startOfLineIndex >= 0 && endOfLineIndex > 0) {
                        Log.i("BT_HANDLER","Data Received = $recordedDataInputStr")
                        // make sure there data before ~
                        var dataInPrint: String = recordedDataInputStr.substring(startOfLineIndex, endOfLineIndex+1) // extract string
                        Log.i("BT_HANDLER","Parsed Data = $dataInPrint")
                        val commandStr: String = recordedDataInputStr.substring(
                            startOfLineIndex+1,
                            endOfLineIndex
                        ) //get value between delimiters
                        Log.i("BT_HANDLER","Command = $commandStr")
                        val command : Int = commandStr.toInt()
                        onGlassesStateChange(command == 1)

                        recordedDataInputStr.delete(0, recordedDataInputStr.length) //clear all string data
                        dataInPrint = " "
                    }
                }
            }
        }
        const val HANDLER_STATE = 0

        fun onGlassesStateChange(glassesOn: Boolean) {
            m_areGlassesOn = glassesOn
            if (m_areGlassesOn ) {
                notificationManager.onDOD()
                container_ref.setBackground()
            }
            else {
                notificationManager.offDOD()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_communication)
        m_address = intent.getStringExtra(SelectDeviceActivity.EXTRA_ADDRESS)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        container_ref = container

        var connection = ConnectToDevice(this).execute()

        m_isEnabled = enable_updates_toggle.isChecked
        enable_updates_toggle.setOnClickListener {
            m_isEnabled = enable_updates_toggle.isChecked
            sendCommand(m_isEnabled)
        }
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
        back_button.setOnClickListener { disconnect() }
    }

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

    private fun sendCommand(isConnecting: Boolean) {
        if(m_BluetoothSocket != null) {
            var command: Int = if (isConnecting) 1 else 0;
            var sendStr = "<$command>";
            Log.i("MessageOut", ""+sendStr)
            for(attempt in 0..2) {
                try {
                    m_BluetoothSocket!!.outputStream.write(sendStr.toByteArray())
                    break
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

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
            BluetoothListener().start()
            m_progress.dismiss()
        }
    }

    private class BluetoothListener(): Thread() {

        private lateinit var m_InStream: InputStream

        init {
            if(m_BluetoothSocket != null) {
                m_InStream = m_BluetoothSocket!!.inputStream
            }
            else {
                Log.i("BT_Listener", "Error initializing bluetooth listener")
            }
        }

        override fun run() {
            val buffer = ByteArray(256)
            var bytes: Int

            while(m_isConnected) {
                if(m_isEnabled) {
                    try {
                        bytes = m_InStream.read(buffer)
                        val readMessage = String(buffer, 0, bytes)
                        m_bluetoothInHandler.obtainMessage(HANDLER_STATE, bytes, -1, readMessage).sendToTarget()
                    } catch (e: IOException) {
                        break
                    }
                }
            }
        }
    }
}
