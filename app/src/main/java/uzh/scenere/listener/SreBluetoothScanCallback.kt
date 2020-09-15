package uzh.scenere.listener

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback

class SreBluetoothScanCallback(private val handleBluetooth: (List<BluetoothDevice>) -> Boolean): ScanCallback(){

    private val alreadyKnownDevices = HashMap<String, BluetoothDevice>()
    private val newDevices = ArrayList<BluetoothDevice>()

    fun addAlreadyKnownDevices(devices: HashMap<String, BluetoothDevice>){
        alreadyKnownDevices.clear()
        alreadyKnownDevices.putAll(devices)
    }

    override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult?) {
        if (result != null ){
            val device = result.device
            if (!newDevices.contains(device)) {
                if (alreadyKnownDevices.containsKey(device.address)){
                    newDevices.add(alreadyKnownDevices[device.address]!!)
                }else{
                    newDevices.add(device)
                }
            }else{
                handleBluetooth.invoke(newDevices)
                newDevices.clear()
            }
        }
        super.onScanResult(callbackType, result)
    }
}