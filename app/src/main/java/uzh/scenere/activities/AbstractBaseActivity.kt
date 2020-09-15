package uzh.scenere.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.res.Configuration
import android.graphics.Typeface
import android.net.NetworkInfo
import android.net.Uri
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.net.wifi.p2p.*
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.*
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.text.SpannedString
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.Gravity.CENTER
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import kotlinx.android.synthetic.main.sre_toolbar.*
import uzh.scenere.R
import uzh.scenere.const.Constants
import uzh.scenere.const.Constants.Companion.CONNECTION_ESTABLISHED
import uzh.scenere.const.Constants.Companion.EIGHT_KB
import uzh.scenere.const.Constants.Companion.FOLDER_TEMP
import uzh.scenere.const.Constants.Companion.MILLION
import uzh.scenere.const.Constants.Companion.NOTHING
import uzh.scenere.const.Constants.Companion.PHONE_STATE
import uzh.scenere.const.Constants.Companion.SMS_RECEIVED
import uzh.scenere.const.Constants.Companion.TRY_AGAIN
import uzh.scenere.const.Constants.Companion.TWO_SEC_MS
import uzh.scenere.const.Constants.Companion.UNKNOWN_RESPONSE
import uzh.scenere.const.Constants.Companion.WIFI_DIRECT_PORT
import uzh.scenere.const.Constants.Companion.ZERO
import uzh.scenere.helpers.*
import uzh.scenere.listener.SreBluetoothReceiver
import uzh.scenere.listener.SreBluetoothScanCallback
import uzh.scenere.listener.SrePhoneReceiver
import uzh.scenere.listener.SreSmsReceiver
import java.io.*
import java.lang.Thread.sleep
import java.lang.reflect.Proxy
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.random.Random
import kotlin.reflect.KClass


abstract class AbstractBaseActivity : AppCompatActivity() {
    protected var marginSmall: Int? = null
    protected var marginHuge: Int? = null
    protected var textSize: Float? = null
    protected var fontAwesome: Typeface? = null
    protected var fontNormal: Typeface = Typeface.DEFAULT
    protected var screenWidth = 0
    protected var screenHeight = 0
    protected var tutorialOpen = false
    //NFC
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var nfcReady = false
    //WiFi
    private var wifiManager: WifiManager? = null
    //WiFi direct
    private var wifiP2pManager: WifiP2pManager? = null
    private var ownWifiP2pDevice: WifiP2pDevice? = null
    private var wifiP2pEnabled = false
    private var retryChannel = false
    //Bluetooth
    private var sreBluetoothReceiver: SreBluetoothReceiver? = null
    private var sreBluetoothCallback: SreBluetoothScanCallback? = null
    //Telephone
    private var srePhoneReceiver: SrePhoneReceiver? = null
    //SMS
    private var sreSmsReceiver: SreSmsReceiver? = null
    //Async
    private var asyncTask: SreAsyncTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndCreateFolderStructure()
        setContentView(getConfiguredLayout())
        readVariables()
        initNfc()
        initWifi()
    }

    private fun checkAndCreateFolderStructure(){
        if (PermissionHelper.check(applicationContext,PermissionHelper.Companion.PermissionGroups.STORAGE)){
            FileHelper.checkAndCreateFolderStructure()
        }
    }

    private fun readVariables() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        screenHeight = displayMetrics.heightPixels
        screenWidth = displayMetrics.widthPixels
        marginSmall = DipHelper.get(resources).dip5
        marginHuge = DipHelper.get(resources).dip50
        textSize = DipHelper.get(resources).dip3_5.toFloat()
        fontAwesome = Typeface.createFromAsset(applicationContext.assets, "FontAwesome900.otf")
    }

    //NFC

    open fun isUsingNfc():Boolean{
        return false
    }

    protected fun isNfcReady(): Boolean{
        return nfcReady
    }

    private fun initNfc() {
        if (isUsingNfc()) {
            nfcAdapter = NfcAdapter.getDefaultAdapter(this)
            if (nfcAdapter != null) {
                pendingIntent = PendingIntent.getActivity(this, 0, Intent(this,
                        javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)

            }
            nfcReady = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (nfcAdapter != null && pendingIntent != null){
            nfcAdapter!!.enableForegroundDispatch(this, pendingIntent, null, null)
        }
        resumePhoneCallListener()
        resumeSmsListener()
        resumeBluetoothListener()
        registerWifiP2pReceiver()
        val sreStyle = getSreStyle(this)
        if (sreStyle != SreStyle.NORMAL){
            reStyle(applicationContext,getConfiguredRootLayout())
            window.statusBarColor = StyleHelper.get(this).getStatusBarColor(this,sreStyle)
        }
    }

    override fun onPause() {
        if (nfcAdapter != null) {
            nfcAdapter!!.disableForegroundDispatch(this)
        }
        pausePhoneCallListener()
        pauseSmsListener()
        pauseBluetoothListener()
        unregisterWifiP2pReceiver()
        cancelAsyncTask()
        clearAllActiveNotifications()
        super.onPause()
    }

    override fun onDestroy() {
        unregisterPhoneCallListener()
        unregisterSmsListener()
        unregisterBluetoothListener()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (nfcReady && intent != null){
            getNfcTagInfo(intent)
        }
    }

    private var nfcDataWrite: String? = null
    private var nfcDataRead: String? = null
    private var beamBinaryWrite: ByteArray? = null
    private var beamBinaryRead: ByteArray? = null

    fun setDataToWrite(data: String?){
        nfcDataWrite = data
    }

    fun getDataToRead(): String?{
        return nfcDataRead
    }

    fun setBinaryDataToBeam(data: ByteArray?){
        beamBinaryWrite = data
    }

    fun getBinaryDataFromBeam(): ByteArray?{
        return beamBinaryRead
    }

    open fun execUseNfcData(data: String){
        //NOP
    }

    open fun execNoDataRead(){
        //NOP
    }

    open fun onDataWriteExecuted(returnValues: Pair<Boolean, String>){
        notify(getString(R.string.nfc),returnValues.second)
    }

    private fun getNfcTagInfo(intent: Intent) {
        val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        if (nfcDataWrite != null){
            onDataWriteExecuted(writeDataToTag(tag,nfcDataWrite!!))
            nfcDataWrite = null
        }else{
            nfcDataRead = getDataFromTag(tag,intent)
            if (nfcDataRead != null){
                execUseNfcData(nfcDataRead!!)
            }else{
                execNoDataRead()
            }
        }
    }

    private fun getDataFromTag(tag: Tag, intent: Intent): String? {
        val ndef = Ndef.get(tag)
        try {
            ndef.connect()
            val messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)

            if (messages != null) {
                val ndefMessages = ArrayList<NdefMessage>()
                for (i in 0 until messages.size) {
                    ndefMessages.add(messages[i] as NdefMessage)
                }
                val record = ndefMessages[0].records[0]

                val payload = record.payload
                val text = String(payload)
                ndef.close()
                return text
            }
        } catch (e: Exception) {
            //NOP
        }
        return null
    }

    private fun writeDataToTag(tag: Tag, message: String): Pair<Boolean,String>{
        val nDefTag = Ndef.get(tag)
        val ndefMessage = dataToNdefMessage(message)
        try {

            nDefTag?.let {
                it.connect()
                if (it.maxSize < ndefMessage.toByteArray().size) {
                    return Pair(false,getString(R.string.nfc_too_large))
                }
                return if (it.isWritable) {
                    it.writeNdefMessage(ndefMessage)
                    it.close()
                    Pair(true,getString(R.string.nfc_success))
                } else {
                    Pair(false,getString(R.string.nfc_write_not_supported))
                }
            }
        } catch (e: Exception) {
            //Possible unformatted Tag, try to format
            try{
                val nDefFormatableTag = NdefFormatable.get(tag)

                nDefFormatableTag?.let {
                    return try {
                        it.connect()
                        it.format(ndefMessage)
                        it.close()
                        Pair(true,getString(R.string.nfc_success))
                    } catch (e: IOException) {
                        Pair(false,getString(R.string.nfc_no_init))
                    }
                }
                return Pair(false,getString(R.string.nfc_write_not_supported))
            }catch(e: Exception){
                //NOP
            }
        }
        return Pair(false,getString(R.string.nfc_write_not_supported))
    }

    private fun dataToNdefMessage(data: String): NdefMessage{
        val pathPrefix = Constants.APPLICATION_ID
        val nfcRecord = NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE, pathPrefix.toByteArray(), ByteArray(0), data.toByteArray())
        return NdefMessage(arrayOf(nfcRecord))
    }

    @SuppressLint("SetWorldReadable")
    fun sendDataOverBeam(){
        if (beamBinaryWrite != null && isUsingNfc() && CommunicationHelper.check(this,CommunicationHelper.Companion.Communications.NFC)&&
                CommunicationHelper.supports(this,CommunicationHelper.Companion.Communications.NFC)&&
                CommunicationHelper.requestBeamActivation(this,true)){
            val fileName = getString(R.string.share_export_file_prefix) + DateHelper.getCurrentTimestamp() + Constants.SRE_FILE
            val filePath = FileHelper.writeFile(applicationContext, beamBinaryWrite!!,fileName,FOLDER_TEMP)
            val fileToTransfer = File(filePath)
            fileToTransfer.setReadable(true, false)
            fileToTransfer.deleteOnExit()
            nfcAdapter!!.setBeamPushUris(arrayOf(Uri.fromFile(fileToTransfer)), this)
            nfcAdapter!!.invokeBeam(this)
            beamBinaryWrite = null
        }
    }

    // WiFi

    open fun isUsingWifi(): Boolean{
        return false
    }

    private fun initWifi(){
        if (isUsingWifi()) {
            wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiReceiver = WifiBroadcastReceiver(wifiManager, onWifiDiscoveredExecutable)
            registerReceiver(wifiReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        }
    }

    var scanningWifi: Boolean = false

    fun startWifiScan(){
        scanningWifi = true
        wifiManager?.startScan()
    }

    fun stopWifiScan(){
        scanningWifi = false
    }

    private val onWifiDiscoveredExecutable: (List<ScanResult>?) -> Unit = {
        if (!it.isNullOrEmpty()) {
            for (scanResult in it){
                execUseWifiScanResult(scanResult)
            }
        }
        if (scanningWifi){
            Handler().postDelayed({
                wifiManager?.startScan()
            },2000)
        }
    }

    open fun execUseWifiScanResult(scanResult: ScanResult) {
        //NOP
    }

    private class WifiBroadcastReceiver(val wifiManager: WifiManager?,val onWifiDiscoveredExecutable: (List<ScanResult>?) -> Unit) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                onWifiDiscoveredExecutable.invoke(wifiManager?.scanResults)
            }
        }
    }

    //WiFi P2P
    var channel: WifiP2pManager.Channel? = null
    var sreWifiP2pBroadcastReceiver: SreWifiP2pBroadcastReceiver? = null
    protected fun enableWifiP2p(){
        if (PermissionHelper.check(this,PermissionHelper.Companion.PermissionGroups.WIFI)){
            wifiP2pEnabled = true
            wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
            channel = wifiP2pManager!!.initialize(this, Looper.getMainLooper(),null)
            registerWifiP2pReceiver()
        }
    }

    private fun registerWifiP2pReceiver() {
        if (wifiP2pEnabled && sreWifiP2pBroadcastReceiver == null){
            sreWifiP2pBroadcastReceiver = SreWifiP2pBroadcastReceiver(
                    wifiP2pManager!!,
                    channel!!,
                    handlePeerData,
                    handleOwnData,
                    createWifiP2pSender,
                    createWifiP2pReceiver,
                    updateWifiP2pDeviceList,
                    updateWifiP2pMaster,
                    isWifiP2pMaster,
                    notifyExecutable,
                    onWifiP2pDiscoveryDisabled,
                    {disconnectFromWifiP2pGroup()})
            val intentFilter = IntentFilter()
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            registerReceiver(sreWifiP2pBroadcastReceiver, intentFilter)
        }
    }

    protected fun getWifiP2pManager(): WifiP2pManager? {
        return wifiP2pManager
    }

    protected fun isWifiP2pScanActive(): Boolean{
        return wifiP2pScanActive
    }

    private var wifiP2pScanActive = false

    protected fun startWifiP2pDeviceDiscovery(){
        if (wifiP2pEnabled && wifiP2pManager != null){
            wifiP2pScanActive = true
            wifiP2pManager!!.discoverPeers(channel, object: WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    //NOP
                }

                override fun onFailure(reason: Int) {
                    //NOP
                }


            })
        }
    }

    protected fun disableWifiP2p(){
        unregisterWifiP2pReceiver()
        stopWifiP2pDeviceDiscovery()
        wifiP2pManager = null
        channel = null
        wifiP2pEnabled = false
    }

    protected fun stopWifiP2pDeviceDiscovery() {
        if (wifiP2pManager != null) {
            wifiP2pScanActive = false
            wifiP2pManager?.stopPeerDiscovery(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    //NOP
                }

                override fun onFailure(reason: Int) {
                    //NOP
                }
            })
        }
    }

    private fun unregisterWifiP2pReceiver() {
        if (wifiP2pEnabled && sreWifiP2pBroadcastReceiver != null){
            try{
            unregisterReceiver(sreWifiP2pBroadcastReceiver)
            }catch(e: Exception){
                //NOP
            }
            sreWifiP2pBroadcastReceiver = null
        }
    }

    protected fun resetWifiP2p(disconnectFromGroup: Boolean = true){
        disableWifiP2p()
        enableWifiP2p()
        if (disconnectFromGroup){
            disconnectFromWifiP2pGroup()
        }
        startWifiP2pDeviceDiscovery()
    }

    @Suppress("UNCHECKED_CAST")
    @SuppressLint("PrivateApi")
    protected fun disconnectFromWifiP2pGroup(){
        if (getWifiP2pManager() != null){
            getWifiP2pManager()?.removeGroup(channel,null)
            try {
                val persistentGroupInfoListenerClass = Class.forName("android.net.wifi.p2p.WifiP2pManager\$PersistentGroupInfoListener")
                val requestPersistentGroupInfo = getWifiP2pManager()?.javaClass!!.getDeclaredMethod("requestPersistentGroupInfo", WifiP2pManager.Channel::class.java, persistentGroupInfoListenerClass)
                requestPersistentGroupInfo.isAccessible = true
                val persistentGroupInfoListener = Proxy.newProxyInstance(persistentGroupInfoListenerClass.classLoader, arrayOf(persistentGroupInfoListenerClass)) { _, method, args ->
                    if (method?.name == "onPersistentGroupInfoAvailable" && args != null){
                        for (arg in args){
                            if (arg.className() == "WifiP2pGroupList"){
                                try{
                                    val wifiP2pGroupListClass = Class.forName("android.net.wifi.p2p.WifiP2pGroupList")
                                    val getGroupList = wifiP2pGroupListClass.getMethod("getGroupList")
                                    val getNetworkId = WifiP2pGroup::class.java.getMethod("getNetworkId")
                                    val persistentWifiP2pGroups: Collection<WifiP2pGroup> = getGroupList.invoke(arg) as Collection<WifiP2pGroup>
                                    val deletePersistentGroup = getWifiP2pManager()?.javaClass!!.getDeclaredMethod("deletePersistentGroup", WifiP2pManager.Channel::class.java, Int::class.java, WifiP2pManager.ActionListener::class.java)
                                    deletePersistentGroup.isAccessible = true
                                    for (group in persistentWifiP2pGroups){
                                        val networkId: Int = getNetworkId.invoke(group) as Int
                                        deletePersistentGroup.invoke(getWifiP2pManager()!!, channel, networkId, null)
                                    }
                                }catch (e: Exception){
                                    //NOP
                                }
                            }
                        }
                    }
                    null
                }
                requestPersistentGroupInfo.invoke(getWifiP2pManager(), channel, persistentGroupInfoListener)
            }catch(e: Exception){
                //NOP
            }
        }
    }



    open val handlePeerData: (WifiP2pDeviceList?) -> Unit = {
        //NOP
    }

    open val handleOwnData: (WifiP2pDevice?) -> Unit = {
        //NOP
    }

    open val collectWifiP2pDataToSend: () -> ByteArray = {
        //NOP
        ByteArray(0)
    }

    open val handleWifiP2pData: (ByteArray) -> Unit = {
        //NOP
    }

    open val postWifiP2pTransmission: () -> Boolean = {
        true //Repeat
    }

    open val isWifiP2pMaster: () -> Boolean = {
        false //Repeat
    }

    open val onWifiP2pDiscoveryDisabled: () -> Unit = {
        //NOP
    }


    open val createWifiP2pSender: () -> ServerSocket? = {
        var returnSocket: ServerSocket? = null
        if (!collectWifiP2pDataToSend.invoke().isEmpty()) {
            cancelAsyncTask()
            try{
            returnSocket = ServerSocket(WIFI_DIRECT_PORT)
            }catch(e: Exception){
                //NOP
            }
            if (returnSocket != null) {
                executeAsyncTask({
                    try {
                        val client = returnSocket.accept()
                        val dataOutputStream = DataOutputStream(BufferedOutputStream(client.getOutputStream()))
                        dataOutputStream.write(collectWifiP2pDataToSend.invoke())
                        returnSocket.close()
                    } catch (e: java.lang.Exception) {
                    } finally {
                        returnSocket.close()
                    }
                }, {
                    returnSocket.close()
                }, {
                    returnSocket.close()
                })
            }
        }
        returnSocket
    }

    open val createWifiP2pReceiver: (WifiP2pInfo) -> Socket? = {
        var returnSocket: Socket? = null
        cancelAsyncTask()
        executeAsyncTask({
            val host: String = it.groupOwnerAddress.hostAddress
            val port: Int = WIFI_DIRECT_PORT
            val socket = Socket()
            returnSocket = socket
            try {
                socket.bind(null)
                socket.connect((InetSocketAddress(host, port)), TWO_SEC_MS.toInt())
                val outputStream = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
                outputStream.write("ACK".toByteArray())
                outputStream.flush()
                val inputStream = DataInputStream(BufferedInputStream(socket.getInputStream()))
                var length = EIGHT_KB
                val out = ByteArrayOutputStream(length)
                sleep(TWO_SEC_MS)
                while (true) {
                    val buffer = ByteArray(length)
                    length = inputStream.read(buffer)
                    out.write(buffer, 0, length)
                    if (length < EIGHT_KB)
                        break
                    }
                handleWifiP2pData.invoke(out.toByteArray())
                out.close()
                inputStream.close()
            } catch (e: IOException) {
            } finally {
                socket.close()
            }
        }, {
            val repeat = postWifiP2pTransmission.invoke()
            if (repeat) {
                Handler().postDelayed({
                    if (wifiP2pEnabled) {
                        resetWifiP2p(false)
                    }
                }, TWO_SEC_MS)
            }
        })
        returnSocket
    }

    open val updateWifiP2pDeviceList: (Collection<WifiP2pDevice>) -> Unit = {
        connectedWifiP2pDeviceList.clear()
        connectedWifiP2pDeviceList.addAll(it)
    }

    open val updateWifiP2pMaster: (WifiP2pDevice?) -> Unit = {
        connectedWifiP2pMaster = it
    }

    protected var connectedWifiP2pDeviceList = ArrayList<WifiP2pDevice>()

    protected var connectedWifiP2pMaster: WifiP2pDevice? = null

    protected var scanStartTime = 0L

    class SreWifiP2pBroadcastReceiver(private val wifiP2pManager: WifiP2pManager,
                                      private val channel: WifiP2pManager.Channel,
                                      private val handlePeerData: (WifiP2pDeviceList?) -> Unit,
                                      private val handleOwnData: (WifiP2pDevice?) -> Unit,
                                      private val createWifiP2pSender: () -> ServerSocket?,
                                      private val createWifiP2pReceiver: (WifiP2pInfo) -> Socket?,
                                      private val updateWifiP2pDeviceList: (Collection<WifiP2pDevice>) -> Unit,
                                      private val updateWifiP2pMaster: (WifiP2pDevice?) -> Unit,
                                      private val isWifiP2pMaster: () -> Boolean,
                                      private val notify: (String) -> Unit,
                                      private val onWifiP2pDiscoveryDisabled: () -> Unit,
                                      private val clearWifiP2pGroups: () -> Unit
    ): BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION == intent?.action) {
                wifiP2pManager.requestPeers(channel) { peers -> handlePeerData.invoke(peers) }
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION == intent?.action) {
                handleOwnData.invoke(intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE) as WifiP2pDevice?)
            }else if (WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION == intent?.action){
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, UNKNOWN_RESPONSE)
                if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED){
                    onWifiP2pDiscoveryDisabled.invoke()
                }
            }else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION == intent?.action){
                val networkInfo: NetworkInfo? = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO) as NetworkInfo
                val wifiP2pInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO) as WifiP2pInfo
                val wifiP2pGroupInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP) as WifiP2pGroup

                if (networkInfo?.isConnected == true && wifiP2pInfo.groupFormed) {

                    if (wifiP2pInfo.isGroupOwner){
                        if (!isWifiP2pMaster.invoke()){
                            notify(TRY_AGAIN)
                            clearWifiP2pGroups.invoke()
                        }else{
                            //SERVER
                            createWifiP2pSender.invoke()
                        }
                    }else{
                        if (isWifiP2pMaster.invoke()){
                            notify(TRY_AGAIN)
                            clearWifiP2pGroups.invoke()
                        }else{
                            //CLIENT
                            notify(CONNECTION_ESTABLISHED)
                            createWifiP2pReceiver.invoke(wifiP2pInfo)
                        }
                    }
                }
                updateWifiP2pMaster.invoke(wifiP2pGroupInfo.owner)
                updateWifiP2pDeviceList.invoke(wifiP2pGroupInfo.clientList)
            }
        }
    }

    abstract fun getConfiguredLayout(): Int
    abstract fun getConfiguredRootLayout(): ViewGroup?

    open fun onNavigationButtonClicked(view: View) {
        when (view.id) {
            R.id.startup_button_continue -> startActivity(Intent(this, MainMenuActivity::class.java))
            R.id.projects_button_scenario_management -> startActivity(Intent(this, ScenariosActivity::class.java))
        }
    }

    open fun onToolbarClicked(view: View) {
        when (view.id) {
            R.id.toolbar_action_left -> onToolbarLeftClicked()
            R.id.toolbar_action_center_left -> onToolbarCenterLeftClicked()
            R.id.toolbar_action_center -> onToolbarCenterClicked()
            R.id.toolbar_action_center_right -> onToolbarCenterRightClicked()
            R.id.toolbar_action_right -> onToolbarRightClicked()
        }
    }

    open fun onButtonClicked(view: View) {
        //NOP
    }

    open fun onToolbarLeftClicked() {
        //NOP
    }

    open fun onToolbarCenterLeftClicked() {
        //NOP
    }

    open fun onToolbarCenterClicked() {
        //NOP
    }

    open fun onToolbarCenterRightClicked() {
        //NOP
    }

    open fun onToolbarRightClicked() {
        //NOP
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        onLayoutRendered()
    }

    open fun onLayoutRendered(){
        //NOP
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        execAdaptToOrientationChange()
    }

    //************
    //* CREATION *
    //************
    protected fun createLayoutParams(weight: Float, textView: TextView? = null, crop: Int = 0): LinearLayout.LayoutParams {
        val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                weight
        )
        if (textView != null) {
            val margin = NumberHelper.nvl(this.resources?.getDimension(R.dimen.dpi5), 0).toInt()
            textView.setPadding(0, 0, 0, 0)
            layoutParams.setMargins(margin, margin, margin, margin)
            when (crop) {
                0 -> {
                    textView.setPadding(0, -margin / 2, 0, 0)
                }
                1 -> layoutParams.setMargins(margin, margin, margin, margin / 2)
                2 -> layoutParams.setMargins(margin, margin / 2, margin, margin)
            }
            textView.gravity = Gravity.CENTER
            textView.textAlignment = View.TEXT_ALIGNMENT_CENTER
            textView.layoutParams = layoutParams
        }
        return layoutParams
    }

    protected fun createTitle(title: String, holder: ViewGroup) {
        val titleText = TextView(this)
        val titleParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        titleText.layoutParams = titleParams
        titleText.textAlignment = View.TEXT_ALIGNMENT_CENTER
        titleText.gravity = Gravity.CENTER
        titleText.text = title
        titleText.setTextColor(getColorWithStyle(applicationContext,R.color.srePrimaryDark))
        holder.addView(titleText)
    }

    fun toast(toast: String) {
        Toast.makeText(this, toast, Toast.LENGTH_SHORT).show()
    }

    fun copyToClipboard(str: String){
        val clipboard =  getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(str, str)
        clipboard.primaryClip = clip
    }

    fun getFromClipboard(): String{
        val clipboard =  getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val str = StringHelper.nvl(clipboard.primaryClip?.getItemAt(0)?.text?.toString(),NOTHING)
        return str
    }

    protected val notifyExecutable: (String) -> Unit = {
        notify(it)
    }

    private  fun clearAllActiveNotifications(){
        for (id in notificationIdQueue){
            val notificationManager = NotificationManagerCompat.from(this)
            notificationManager.cancel(id)
        }
    }

    private val notificationQueue = ArrayList<Pair<String?,String?>>()
    private val notificationIdQueue = ArrayList<Int>()
    fun notify(title: String? = null, content: String? = null, clearNotificationId: Int? = null){
        val notificationManager = NotificationManagerCompat.from(this)
        if (clearNotificationId == null){
            notificationQueue.add(Pair(title,content))
            if (notificationQueue.size > 1){
                return // let the notifications in the queue call themselves
            }
        }else{
            notificationQueue.removeFirst()
            notificationManager.cancel(clearNotificationId)
            notificationIdQueue.remove(clearNotificationId)
        }
        if (notificationQueue.isEmpty()){
            return
        }
        val notification = NotificationCompat.Builder(applicationContext, Constants.APPLICATION_ID)
                .setSmallIcon(android.R.drawable.btn_star)
                .setContentTitle(notificationQueue.first().first)
                .setColor(getColorWithStyle(this,R.color.srePrimary))
                .setColorized(true)
                .setDefaults(Notification.DEFAULT_ALL)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notification.priority = NotificationManager.IMPORTANCE_HIGH
        }else{
            notification.priority = Notification.PRIORITY_MAX
        }
        if (notificationQueue.first().second != null){
            notification.setContentText(notificationQueue.first().second)
        }
        val text = StringHelper.nvl(notificationQueue.first().first,NOTHING).plus(StringHelper.nvl(notificationQueue.first().second,NOTHING))
        val id = Random(System.currentTimeMillis()).nextInt(ZERO, MILLION)
        Handler().postDelayed( {
            notificationIdQueue.add(id)
            notificationManager.notify(id, notification.build())
        },100)
        Handler().postDelayed( {
            notify(null,null,id)
        },2000L+(50*text.length))
    }

    protected fun execMinimizeKeyboard(){
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        val focusView = currentFocus ?: View(this)
        inputMethodManager.hideSoftInputFromWindow(focusView.windowToken, 0)
        focusView.clearFocus()
    }

    open fun execAdaptToOrientationChange() {
        //NOP
    }

    //*******
    //* GUI *
    //*******
    protected fun customizeToolbarId(l: Int?, cl: Int?, c: Int?, cr: Int?, r: Int?) {
        toolbar_action_left.text = StringHelper.lookupOrEmpty(l, applicationContext)
        toolbar_action_center_left.text = StringHelper.lookupOrEmpty(cl, applicationContext)
        toolbar_action_center.text = StringHelper.lookupOrEmpty(c, applicationContext)
        toolbar_action_center_right.text = StringHelper.lookupOrEmpty(cr, applicationContext)
        toolbar_action_right.text = StringHelper.lookupOrEmpty(r, applicationContext)
    }

    protected fun adaptToolbarId(l: Int?, cl: Int?, c: Int?, cr: Int?, r: Int?) {
        toolbar_action_left.text = if (l != null) StringHelper.lookupOrEmpty(l, applicationContext) else toolbar_action_left.text
        toolbar_action_center_left.text = if (cl != null)  StringHelper.lookupOrEmpty(cl, applicationContext) else toolbar_action_center_left.text
        toolbar_action_center.text = if (c != null)  StringHelper.lookupOrEmpty(c, applicationContext) else toolbar_action_center.text
        toolbar_action_center_right.text = if (cr != null)  StringHelper.lookupOrEmpty(cr, applicationContext) else toolbar_action_center_right.text
        toolbar_action_right.text = if (r != null)  StringHelper.lookupOrEmpty(r, applicationContext) else toolbar_action_right.text
    }

    protected fun customizeToolbarText(l: String?, cl: String?, c: String?, cr: String?, r: String?) {
        toolbar_action_left.text = l
        toolbar_action_center_left.text = cl
        toolbar_action_center.text = c
        toolbar_action_center_right.text = cr
        toolbar_action_right.text = r
    }

    protected fun adaptToolbarText(l: String?, cl: String?, c: String?, cr: String?, r: String?) {
        toolbar_action_left.text = l ?: toolbar_action_left.text
        toolbar_action_center_left.text = cl ?: toolbar_action_center_left.text
        toolbar_action_center.text = c ?: toolbar_action_center.text
        toolbar_action_center_right.text = cr ?: toolbar_action_center_right.text
        toolbar_action_right.text = r ?: toolbar_action_right.text
    }

    protected fun getSpannedStringFromId(id: Int): SpannedString{
        return getText(id) as SpannedString
    }

    fun executeAsyncTask(asyncFunction: () -> Unit, postExecuteFunction: () -> Unit, cancelExecuteFunction: (() -> Unit)? = null){
        if (asyncTask == null){
            asyncTask = SreAsyncTask(asyncFunction,postExecuteFunction,{cancelAsyncTask()},cancelExecuteFunction)
            asyncTask?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    fun cancelAsyncTask(){
        if (asyncTask != null){
            try {
                asyncTask?.cancel(true)
            }catch (e: Exception){
                //NOP
            }
            asyncTask = null
        }
    }

    protected fun isAsyncTaskRunning(): Boolean{
        if (asyncTask != null){
            return !asyncTask!!.isCancelled
        }
        return false
    }

    class SreAsyncTask(private val asyncFunction: () -> Unit, private val postExecuteFunction: () -> Any?, private val cleanupFunction: () -> Unit, private val cancelExecuteFunction: (() -> Unit)?) : AsyncTask<Void, Void, Void?>() {
        override fun doInBackground(vararg params: Void?): Void? {
            try{
                asyncFunction.invoke()
            }catch(e: Exception){
                //NOP
            }
            return null
        }

        override fun onPreExecute() {
            //NOP
        }

        override fun onPostExecute(result: Void?) {
            try{
                postExecuteFunction.invoke()
            }catch(e: Exception){
                //NOP
            }finally {
                cleanupFunction.invoke()
            }
        }

        override fun onCancelled() {
            try{
                cancelExecuteFunction?.invoke()
            }catch(e: Exception){
                //NOP
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <T: View> searchForLayout(view: View, clazz: KClass<T>): T? {
        if (view is ViewGroup){
            for (v in 0 until view.childCount){
                if (view.getChildAt(v)::class == clazz){
                    return view.getChildAt(v) as T
                }
                val v0 = searchForLayout(view.getChildAt(v),clazz)
                if (v0 != null){
                    return v0
                }
            }
        }
        return null
    }

    protected fun <T: View?> removeExcept(holder: ViewGroup, exception: T) {
        if (exception == null){
            return
        }
        if (holder.childCount == 0)
            return
        if (holder.childCount == 1 && holder.getChildAt(0) == exception)
            return
        if (holder.getChildAt(0) != exception) {
            holder.removeViewAt(0)
        }else{
            holder.removeViewAt(holder.childCount-1)
        }
        removeExcept(holder,exception)
    }

    fun registerPhoneCallListener(){
        srePhoneReceiver = SrePhoneReceiver(handlePhoneCall)
        resumePhoneCallListener()
    }

    fun unregisterPhoneCallListener(){
        pausePhoneCallListener()
        srePhoneReceiver = null
    }

    private fun resumePhoneCallListener(){
        if (srePhoneReceiver != null){
            srePhoneReceiver!!.registerListener(applicationContext)
            val phoneCallFilter = IntentFilter()
            phoneCallFilter.addAction(PHONE_STATE)
            registerReceiver(srePhoneReceiver,phoneCallFilter)
        }
    }

    private fun pausePhoneCallListener(){
        if (srePhoneReceiver != null){
            try{
                val telephonyManager = applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                telephonyManager.listen(null,PhoneStateListener.LISTEN_NONE)
                unregisterReceiver(srePhoneReceiver)
            }catch(e: Exception){
                //NOP
            }
        }
    }

    open fun handlePhoneNumber(phoneNumber: String): Boolean {
        return false
    }

    private val handlePhoneCall: (Context?, String) -> Boolean = { context: Context?, phoneNumber: String ->
        var hangUp = false
        if (handlePhoneNumber(phoneNumber)) {
            try {
                val telephonyManager = context?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

                val methodGetITelephony = telephonyManager.javaClass.getDeclaredMethod("getITelephony")

                methodGetITelephony.isAccessible = true

                val telephonyInterface = methodGetITelephony.invoke(telephonyManager)

                val methodEndCall = telephonyInterface.javaClass.getDeclaredMethod("endCall")

                methodEndCall.invoke(telephonyInterface)
                hangUp = true

            } catch (e: java.lang.Exception) {
                //NOP
            }
        }
        hangUp
    }

    fun registerSmsListener(){
        sreSmsReceiver = SreSmsReceiver(handleSms)
        resumeSmsListener()
    }

    fun unregisterSmsListener(){
        pauseSmsListener()
        sreSmsReceiver = null
    }

    private fun resumeSmsListener(){
        if (sreSmsReceiver != null){
            val smsFilter = IntentFilter()
            smsFilter.addAction(SMS_RECEIVED)
            registerReceiver(sreSmsReceiver,smsFilter)
        }
    }

    private fun pauseSmsListener(){
        if (sreSmsReceiver != null){
            try{
                unregisterReceiver(sreSmsReceiver)
            }catch(e: Exception){
                //NOP
            }
        }
    }

    open fun handleSmsData(phoneNumber: String, message: String): Boolean {
        return false
    }

    private val handleSms: (String, String) -> Boolean = { phoneNumber: String, message: String ->
        if (handleSmsData(phoneNumber,message)) {
            //NOP
        }
        true
    }

    fun registerBluetoothListener(){
        if (CommunicationHelper.check(this,CommunicationHelper.Companion.Communications.BLUETOOTH)){
            // Turn off Bluetooth to let devices reconnect
            CommunicationHelper.disable(this,CommunicationHelper.Companion.Communications.BLUETOOTH)
        }
        //Turn on Bluetooth again
        CommunicationHelper.enable(this,CommunicationHelper.Companion.Communications.BLUETOOTH)
        registerBluetoothListenerDelayed()
    }

    private fun registerBluetoothListenerDelayed() {
        Handler().postDelayed({
            if (CommunicationHelper.check(this, CommunicationHelper.Companion.Communications.BLUETOOTH)) {
                sreBluetoothReceiver = SreBluetoothReceiver(handleBluetooth)
                resumeBluetoothListener()
            }else{
                CommunicationHelper.enable(this,CommunicationHelper.Companion.Communications.BLUETOOTH)
                registerBluetoothListenerDelayed()
            }
        }, 300
        )
    }

    fun unregisterBluetoothListener(){
        pauseBluetoothListener()
        sreBluetoothReceiver = null
    }

    private fun resumeBluetoothListener(){
        if (sreBluetoothReceiver != null){
            val bluetoothFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            bluetoothFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            bluetoothFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            registerReceiver(sreBluetoothReceiver,bluetoothFilter)
            execStartBluetoothDiscovery()
        }
    }

    private fun pauseBluetoothListener(){
        if (sreBluetoothReceiver != null){
            try{
                execStopBluetoothDiscovery()
                unregisterReceiver(sreBluetoothReceiver)
            }catch(e: Exception){
                //NOP
            }
        }
    }

    fun execStartBluetoothDiscovery() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothAdapter.startDiscovery()
        if (sreBluetoothCallback == null){
            val alreadyKnownDevices = HashMap<String,BluetoothDevice>()
            if (!bluetoothAdapter.bondedDevices.isEmpty()){
                for (device in bluetoothAdapter.bondedDevices){
                    alreadyKnownDevices[device.address] = device
                }
            }
            sreBluetoothReceiver?.addAlreadyKnownDevices(alreadyKnownDevices)
            sreBluetoothCallback = SreBluetoothScanCallback(handleBluetooth)
            sreBluetoothCallback?.addAlreadyKnownDevices(alreadyKnownDevices)
            bluetoothAdapter.bluetoothLeScanner.startScan(sreBluetoothCallback)
        }
    }

    private fun execStopBluetoothDiscovery() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothAdapter.cancelDiscovery()
        if (sreBluetoothCallback != null){
            bluetoothAdapter.bluetoothLeScanner.stopScan(sreBluetoothCallback)
            bluetoothAdapter.bluetoothLeScanner.flushPendingScanResults(sreBluetoothCallback)
            sreBluetoothCallback = null
        }
    }

    open fun handleBluetoothData(devices: List<BluetoothDevice>): Boolean {
        return false
    }

    private val handleBluetooth: (List<BluetoothDevice>) -> Boolean = { devices: List<BluetoothDevice> ->
        if (handleBluetoothData(devices)) {
            //NOP
        }
        true
    }

    protected fun createLoadingCircle(): RelativeLayout {
        val progressBar = ProgressBar(applicationContext)
        val progressLayout = RelativeLayout(applicationContext)
        val layoutParams = RelativeLayout.LayoutParams(screenWidth,screenHeight)
        progressLayout.layoutParams = layoutParams
        progressLayout.gravity = CENTER
        val progressBarParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        progressBarParams.addRule(RelativeLayout.CENTER_IN_PARENT)
        progressBar.layoutParams = progressBarParams
        progressLayout.addView(progressBar,progressBarParams)
        return progressLayout
    }
}
