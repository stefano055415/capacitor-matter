package com.falconeta.capacitor.matter

import android.app.Activity
import android.bluetooth.BluetoothGatt
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import chip.devicecontroller.ChipDeviceController
import chip.devicecontroller.NetworkCredentials
import chip.devicecontroller.model.ChipAttributePath
import chip.devicecontroller.model.ChipPathId
import chip.setuppayload.SetupPayload
import chip.setuppayload.SetupPayloadParser
import com.falconeta.capacitor.matter.chip.ChipClient
import com.falconeta.capacitor.matter.chip.ClustersHelper
import com.falconeta.capacitor.matter.chip.GenericChipDeviceListener
import com.falconeta.capacitor.matter.chip.bluetooth.BluetoothManager
import com.falconeta.capacitor.matter.chip.setuppayloadscanner.CHIPDeviceInfo
import com.falconeta.capacitor.matter.commissioning.AppCommissioningService
import com.falconeta.capacitor.matter.preference.Preference
import com.getcapacitor.Bridge
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import com.google.android.gms.home.matter.Matter
import com.google.android.gms.home.matter.commissioning.CommissioningRequest
import com.google.android.gms.home.matter.commissioning.CommissioningResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.Int
import kotlin.String

class MatterInstance(
  private val context: Context,
  private val bridge: Bridge
) {

  private var _call: PluginCall? = null;

//  private var deviceController: ChipDeviceController = chipClient.  .getDeviceController(context)
//  private val networkCredentialsParcelable: NetworkCredentialsParcelable?
//    get() = arguments?.getParcelable(ARG_NETWORK_CREDENTIALS)

  private val commissioningLauncher = bridge.activity.registerForActivityResult(
  ActivityResultContracts.StartIntentSenderForResult()
  ) { result: ActivityResult ->
    if (result.resultCode == Activity.RESULT_OK) {
      //Timber.d(TAG, "Commissioning succeeded.")
      Log.i(ContentValues.TAG, "Commissioning succeeded.");
      commissionDeviceSucceeded(result)
    } else {
      Log.i(ContentValues.TAG, "Commissioning failed. " + result.resultCode);
      if (_call != null) {
        _call!!.reject("-1")
        _call = null;
      }

    }
  }


  private var preference: Preference = Preference(context);
  private lateinit var clustersHelper: ClustersHelper;
  private lateinit var chipClient: ChipClient;

  private val matterInstanceJob = Job()
  private val matterInstancePairingJob = Job()
  private val matterInstanceScope = CoroutineScope(Dispatchers.Main + matterInstanceJob)
  private val matterPairingScope = CoroutineScope(Dispatchers.Main + matterInstancePairingJob)

  private var configured = false;

  fun configure(
    deviceControllerKey: String?,
    caRootCert: String?,
    fabricId: Long,
    vendorId: Int
  ) {
    Log.i(
      "configure",
      "deviceControllerKey $deviceControllerKey, caRootCert $caRootCert, fabricId $fabricId, vendorId: $vendorId, "
    )
    preference.setConfiguration(deviceControllerKey, caRootCert, fabricId, vendorId);
    clustersHelper = ClustersHelper(context);
    chipClient = ChipClient(context);
    configured = true;
  }

  fun clear() {
    preference.clear()
  }

  fun getCerts(): Pair<String, String> {
    return preference.getCerts()
  }

  fun manualCodeCommissioning(deviceId: Long, manualCode: String, ssid: String, ssidPassword: String, call: PluginCall) {
    preference.setDeviceIdForCommissioning(deviceId);
    _call = call;
    handleInputQrCodeOrManualCode(deviceId, manualCode, ssid, ssidPassword)
  }


  fun qrCodeCommissioning(deviceId: Long, qrCode: String, ssid: String, ssidPassword: String, call: PluginCall) {
    preference.setDeviceIdForCommissioning(deviceId);
    _call = call;
    handleInputQrCodeOrManualCode(deviceId, qrCode, ssid, ssidPassword)
  }

  private fun handleInputQrCodeOrManualCode(deviceId: Long, qrCode: String, ssid: String, ssidPassword: String) {
    lateinit var payload: SetupPayload
    var isShortDiscriminator = false
    try {
      payload = SetupPayloadParser().parseQrCode(qrCode)
    } catch (ex: SetupPayloadParser.SetupPayloadException) {
      try {
        payload = SetupPayloadParser().parseManualEntryCode(qrCode)
        isShortDiscriminator = true
      } catch (ex: Exception) {
        showError("-5")
        Log.e("TAG", "Unrecognized Manual Pairing Code", ex)
      }
    } catch (ex: SetupPayloadParser.UnrecognizedQrCodeException) {
      showError("-5")
      Log.e("TAG", "Unrecognized QR Code", ex)
    }

    val deviceInfo = CHIPDeviceInfo.fromSetupPayload(payload, isShortDiscriminator)
    Log.i("TAG", deviceInfo.setupPinCode.toString())
    startConnectingToDevice(deviceId, deviceInfo, ssid, ssidPassword )
  }

  private fun showError(error: String) {
    bridge.activity.runOnUiThread {
      _call?.reject(error)
      _call = null
    }
  }
  private fun startConnectingToDevice(deviceId: Long, deviceInfo: CHIPDeviceInfo, ssid: String, ssidPassword: String) {
//    if (gatt != null) {
//      return
//    }

    matterPairingScope.launch {
      val bluetoothManager = BluetoothManager()

      val device = bluetoothManager.getBluetoothDevice(context, deviceInfo.discriminator, deviceInfo.isShortDiscriminator) ?: run {
        showError("-3")
        return@launch
      }

      val gatt = bluetoothManager.connect(context, device, chipClient)

      chipClient.setCompletionListener(ConnectionCallback())

      val connId = bluetoothManager.connectionId
      var network: NetworkCredentials? = null

      network = NetworkCredentials.forWiFi(NetworkCredentials.WiFiCredentials(ssid, ssidPassword))

//      val thread = networkParcelable.threadCredentials
//      if (thread != null) {
//        network = NetworkCredentials.forThread(NetworkCredentials.ThreadCredentials(thread.operationalDataset))
//      }

      setAttestationDelegate()

      chipClient.pairDevice(gatt, connId, deviceId, deviceInfo.setupPinCode, network)
    }
  }

  private fun setAttestationDelegate() {
    chipClient.setDeviceAttestationDelegate(DEVICE_ATTESTATION_FAILED_TIMEOUT
    ) { devicePtr, _, errorCode ->
      Log.i(TAG, "Device attestation errorCode: $errorCode, " +
        "Look at 'src/credentials/attestation_verifier/DeviceAttestationVerifier.h' " +
        "AttestationVerificationResult enum to understand the errors")


      if (errorCode == STATUS_PAIRING_SUCCESS || errorCode == STATUS_PAIRING_DEVICE_ATTTESTATION_FAILED) {
        bridge.activity.runOnUiThread(Runnable {
          chipClient.continueCommissioning(devicePtr, true)
        })

        return@setDeviceAttestationDelegate
      } else {
        chipClient.close()
        showError("-7")
      }

//      activity.runOnUiThread(Runnable {
//        if (dialog != null && dialog?.isShowing == true) {
//          Log.d(TAG, "dialog is already showing")
//          return@Runnable
//        }
//        dialog = AlertDialog.Builder(activity)
//          .setPositiveButton("Continue",
//            DialogInterface.OnClickListener { dialog, id ->
//              deviceController.continueCommissioning(devicePtr, true)
//            })
//          .setNegativeButton("No",
//            DialogInterface.OnClickListener { dialog, id ->
//              deviceController.continueCommissioning(devicePtr, false)
//            })
//          .setTitle("Device Attestation")
//          .setMessage("Device Attestation failed for device under commissioning. Do you wish to continue pairing?")
//          .show()
//      })
    }
  }

  fun commandOnOff(deviceId: Long, endpointId: Int, value: Boolean) {
    this.matterInstanceScope.launch {
      clustersHelper.setOnOffDeviceStateOnOffCluster(deviceId, value, endpointId)
    }
  }

  fun readAttribute(deviceId: Long, endpointId: Int, clusterId: Int, attributeId: Int, call: PluginCall) {
    if (!configured) {
      throw java.lang.Error("plugin must be configured first...");
    }
    this.matterInstanceScope.launch {
      val chipEndpointId = getChipPathIdForText(endpointId.toString())
      val chipClusterId = getChipPathIdForText(clusterId.toString())
      val chipAttributeId = getChipPathIdForText(attributeId.toString())
//      val eventId = getChipPathIdForText(eventIdEd.text.toString())
      val attributePath =
        ChipAttributePath.newInstance(chipEndpointId, chipClusterId, chipAttributeId)

      chipClient.readAttribute(deviceId, attributePath, call);
    }
  }

  private fun getChipPathIdForText(text: String): ChipPathId {
    return if (text.isEmpty()) ChipPathId.forWildcard() else ChipPathId.forId(text.toLong())
  }

  private fun commissionDevice(call: PluginCall) {

    if (!configured) {
      throw java.lang.Error("plugin must be configured first...");
    }

//    val commissionDeviceRequest = CommissioningRequest.builder()
//      .setCommissioningService(ComponentName(context, AppCommissioningService::class.java)).build()
//
//    // The call to commissionDevice() creates the IntentSender that will eventually be launched
//    // in the fragment to trigger the commissioning activity in GPS.
//    Matter.getCommissioningClient(context).commissionDevice(commissionDeviceRequest)
//      .addOnSuccessListener { result ->
//        // Log.i("TEST", "ShareDevice: Success getting the IntentSender: result [${result}]")
//        _call = call;
//        commissioningLauncher.launch(IntentSenderRequest.Builder(result).build())
//
//
//      }.addOnFailureListener { error ->
//        call.reject("reject")
////        Timber.e(error)
////        _commissionDeviceStatus.postValue(
////          TaskStatus.Failed("Setting up the IntentSender failed", error))
//      }
  }

  fun commissionDeviceSucceeded(activityResult: ActivityResult) {
    val result =
      CommissioningResult.fromIntentSenderResult(activityResult.resultCode, activityResult.data)

    val data = JSObject()
    data.put("deviceType", result.commissionedDeviceDescriptor.deviceType)
    val ret = JSObject()
    ret.put("value", data)

    if (_call != null) {
      _call!!.resolve(ret)
      _call = null;
    }
  }

  inner class ConnectionCallback : GenericChipDeviceListener() {
    override fun onConnectDeviceComplete() {
      Log.d(TAG, "onConnectDeviceComplete")
    }

    override fun onStatusUpdate(status: Int) {
      Log.d(TAG, "Pairing status update: $status")
    }

    override fun onCommissioningComplete(nodeId: Long, errorCode: Int) {
      chipClient.close()
      if (errorCode == STATUS_PAIRING_SUCCESS) {
        _call?.resolve()
        _call = null;
      } else {
        showError("-8")
      }
    }

    override fun onPairingComplete(code: Int) {
      Log.d(TAG, "onPairingComplete: $code")
      chipClient.close()
      if (code != STATUS_PAIRING_SUCCESS) {
        showError("-7")
      }
    }

    override fun onOpCSRGenerationComplete(csr: ByteArray) {
      Log.d(TAG, String(csr))
    }

    override fun onPairingDeleted(code: Int) {
      Log.d(TAG, "onPairingDeleted: $code")
    }

    override fun onCloseBleComplete() {
      Log.d(TAG, "onCloseBleComplete")
    }

    override fun onError(error: Throwable?) {
      Log.d(TAG, "onError: $error")
    }
  }

  companion object {
    private const val TAG = "DeviceProvisioningFragment"
    private const val ARG_DEVICE_INFO = "device_info"
    private const val ARG_NETWORK_CREDENTIALS = "network_credentials"
    private const val STATUS_PAIRING_SUCCESS = 0
    private const val STATUS_PAIRING_DEVICE_ATTTESTATION_FAILED = 101

    /**
     * Set for the fail-safe timer before onDeviceAttestationFailed is invoked.
     *
     * This time depends on the Commissioning timeout of your app.
     */
    private const val DEVICE_ATTESTATION_FAILED_TIMEOUT = 600

    /**
     * Return a new instance of [DeviceProvisioningFragment]. [networkCredentialsParcelable] can be null for
     * IP commissioning.
     */

  }
}
