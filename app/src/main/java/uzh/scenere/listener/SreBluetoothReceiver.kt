package uzh.scenere.listener

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import uzh.scenere.helpers.CollectionHelper

class SreBluetoothReceiver(private val handleBluetooth: (List<BluetoothDevice>) -> Boolean) : BroadcastReceiver() {

    private val alreadyKnownDevices = HashMap<String, BluetoothDevice>()
    private val newDevices = ArrayList<BluetoothDevice>()

    fun addAlreadyKnownDevices(devices: HashMap<String, BluetoothDevice>){
        alreadyKnownDevices.clear()
        alreadyKnownDevices.putAll(devices)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null && CollectionHelper.oneOf(intent.action, BluetoothDevice.ACTION_FOUND, BluetoothDevice.ACTION_ACL_CONNECTED)) {
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            if(!newDevices.contains(device)){
                if (alreadyKnownDevices.containsKey(device.address)){
                    newDevices.add(alreadyKnownDevices[device.address]!!)
                }else{
                    newDevices.add(device)
                }
            }
        }else if (intent != null && intent.action == BluetoothAdapter.ACTION_DISCOVERY_FINISHED) {
            if(!newDevices.isEmpty()){
                handleBluetooth.invoke(newDevices)
                newDevices.clear()
            }
        }else{
            //NOP
        }
    }
}