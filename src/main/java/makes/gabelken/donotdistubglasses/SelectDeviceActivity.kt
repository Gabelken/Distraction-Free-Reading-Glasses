package makes.gabelken.donotdistubglasses

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.select_device_layout.*
import org.jetbrains.anko.toast

class SelectDeviceActivity : AppCompatActivity() {

    // Bluetooth settings
    private var m_bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var m_pairedDevices: Set<BluetoothDevice>
    private val REQUEST_ENABLE_BLUETOOTH = 1

    companion object {
        // Store address of selected devices for subsequent pages to access
        val EXTRA_ADDRESS: String = "Device_Address"
    }

    // Initialize the page
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.select_device_layout)

        // Get system's bluetooth helper
        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        //Make sure device is able to use bluetooth
        if (m_bluetoothAdapter == null) {
            toast("Bluetooth Not Supported on this Device")
            return
        }

        //Ask to enable bluetooth if disabled
        if (!m_bluetoothAdapter!!.isEnabled) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH)
        }

        // Re-retrieve list of available devices each time refresh is clicked
        select_device_refresh.setOnClickListener { pairedDeviceList() }
        // Get initial list of paired devices
        pairedDeviceList()

    }

    /**
     * Ask the phone what bluetooth devices it knows about and parse the list for display.
     */
    private fun pairedDeviceList() {
        m_pairedDevices = m_bluetoothAdapter!!.bondedDevices
        val list: ArrayList<BluetoothDevice> = ArrayList()
        val names: ArrayList<String> = ArrayList()

        if(!m_pairedDevices.isEmpty()) {
            for (device: BluetoothDevice in m_pairedDevices) {
                list.add(device)
                names.add(device.name)
                Log.i("device", ""+device)
            }
        } else {
            toast("No Devices Found")
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, names)
        select_device_list.adapter = adapter
        select_device_list.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val device: BluetoothDevice = list[position]
            val address: String = device.address

            val intent = Intent(this, CommunicationActivity::class.java)
            intent.putExtra(EXTRA_ADDRESS, address)
            startActivity(intent)
        }
    }

    // On successful loading of the page, ask the user to enable bluetooth if  they haven't already
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == Activity.RESULT_OK) {
                if (m_bluetoothAdapter!!.isEnabled) {
                    toast("Bluetooth has been enabled")
                } else {
                    toast("Bluetooth has been disabled")
                }
            } else if(resultCode == Activity.RESULT_CANCELED) {
                toast("Bluetooth enabling has been interrupted")
            }
        }
    }

}

