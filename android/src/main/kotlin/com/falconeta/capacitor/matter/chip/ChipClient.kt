package com.falconeta.capacitor.matter.chip

import android.bluetooth.BluetoothGatt
import android.content.Context
import android.util.Log
import androidx.core.content.ContentProviderCompat.requireContext
import chip.devicecontroller.*
import chip.devicecontroller.GetConnectedDeviceCallbackJni.GetConnectedDeviceCallback
import chip.devicecontroller.model.ChipAttributePath
import chip.devicecontroller.model.ChipEventPath
import chip.devicecontroller.model.NodeState
import chip.platform.AndroidBleManager
import chip.platform.AndroidChipPlatform
import chip.platform.ChipMdnsCallbackImpl
import chip.platform.DiagnosticDataProviderImpl
import chip.platform.NsdManagerServiceBrowser
import chip.platform.NsdManagerServiceResolver
import chip.platform.PreferencesConfigurationManager
import chip.platform.PreferencesKeyValueStoreManager
import com.falconeta.capacitor.matter.preference.Preference
import com.falconeta.capacitor.matter.stripLinkLocalInIpAddress
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ChipClient(context: Context) {
  private val TAG: String = "CHIP CLIENT SERVICE"

  private val preference: Preference
  private val vendorId: Int
  private val fabricId: Long
  private var _call: PluginCall? = null;
  private lateinit var androidPlatform: AndroidChipPlatform

  init {
    preference = Preference(context)
    val (vid, fid) = preference.getVendorIdAndFabricId()
    vendorId = vid;
    fabricId = fid;
  }

  fun getAndroidChipPlatform(context: Context?): AndroidChipPlatform {
    if (!this::androidPlatform.isInitialized && context != null) {
      //force ChipDeviceController load jni
      ChipDeviceController.loadJni()
      androidPlatform = AndroidChipPlatform(AndroidBleManager(), PreferencesKeyValueStoreManager(context), PreferencesConfigurationManager(context), NsdManagerServiceResolver(context), NsdManagerServiceBrowser(context), ChipMdnsCallbackImpl(), DiagnosticDataProviderImpl(context))
    }

    return androidPlatform
  }

  private val reportCallback = object : ReportCallback {
    override fun onError(attributePath: ChipAttributePath?, eventPath: ChipEventPath?, ex: Exception) {
      if (attributePath != null)
      {
        Log.e(TAG, "Report error for $attributePath: $ex")
      }
      if (eventPath != null)
      {
        Log.e(TAG, "Report error for $eventPath: $ex")
      }
    }

    override fun onReport(nodeState: NodeState) {
      Log.i(TAG, "Received wildcard report")
      val debugString = nodeStateToDebugString(nodeState)
      if (_call == null){
        return;
      }
      val data = JSObject()
      data.put("value", debugString)
      _call!!.resolve(data)
    }

    override fun onDone() {
      Log.i(TAG, "wildcard report Done")
    }
  }

  private fun nodeStateToDebugString(nodeState: NodeState): String {
    val stringBuilder = StringBuilder()
    nodeState.endpointStates.forEach { (endpointId, endpointState) ->
      // stringBuilder.append("{")
      endpointState.clusterStates.forEach { (clusterId, clusterState) ->
        // stringBuilder.append("\"${ChipIdLookup.clusterIdToName(clusterId)}Cluster\": {")
        clusterState.attributeStates.forEach { (attributeId, attributeState) ->
//          val attributeName = ChipIdLookup.attributeIdToName(clusterId, attributeId)
          stringBuilder.append("${attributeState.value}")
        }
//        clusterState.eventStates.forEach { (eventId, events) ->
//          for (event in events)
//          {
//            // stringBuilder.append("\"eventNumber\": ${event.eventNumber}")
//            // stringBuilder.append("\"priorityLevel\": ${event.priorityLevel}")
//            // stringBuilder.append("\"systemTimeStamp\": ${event.systemTimeStamp}")
//
//            // val eventName = ChipIdLookup.eventIdToName(clusterId, eventId)
//            stringBuilder.append("${event.value}")
//          }
//        }
        // stringBuilder.append("}")
      }
      // stringBuilder.append("}")
    }
    return stringBuilder.toString()
  }

  private val controllerParams: ControllerParams = ControllerParams.newBuilder()
    .setUdpListenPort(0)
//    .setRootCertificate("MIIBljCCATygAwIBAgIBADAKBggqhkjOPQQDAjAiMSAwHgYKKwYBBAGConwBBAwQMDAwMDAwMDAwMDAwMDAwMDAeFw0yMTA2MTAwMDAwMDBaFw0zMTA2MDgwMDAwMDBaMCIxIDAeBgorBgEEAYKifAEEDBAwMDAwMDAwMDAwMDAwMDAwMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEpcaFHeRGcaHarIfWxgZPDOO/oPX6H2VngD/P1XJqOFI1Kmjh+HrbnNTb92cpZjfs6VEoMnimy+eFMgDMAsemQqNjMGEwDwYDVR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8EBAMCAQYwHQYDVR0OBBYEFIwppQEIGNW95r7dA6kxhhzNQfvaMB8GA1UdIwQYMBaAFIwppQEIGNW95r7dA6kxhhzNQfvaMAoGCCqGSM49BAMCA0gAMEUCIQDDF1F9sVuug55gceG3To7G1WuBUMHi40Cx2uXrYsHkQQIgX+Re3SPGFDH8j07PqdCqaKCeRUhiuuMJJ8ttblhwT68=".toByteArray())
    .setControllerVendorId(vendorId)
    .setFabricId(fabricId)
//    .setOperationalCertificate("BKXGhR3kRnGh2qyH1sYGTwzjv6D1+h9lZ4A/z9VyajhSNSpo4fh625zU2/dnKWY37OlRKDJ4psvnhTIAzALHpkJDO7ViWThhtxxwWtSBqcmGtRckp0QFev2K+jvAn30BJbCW5X8AAABhAAAAAAAAAA==".toByteArray())
    .build();

  // Lazily instantiate [ChipDeviceController] and hold a reference to it.

  private val chipDeviceController: ChipDeviceController by lazy {
    ChipDeviceController.loadJni()
    ChipDeviceController(
      controllerParams)
  }

  // private val chipDeviceController = ChipDeviceController(ControllerParams.newBuilder().setControllerVendorId(vendorId).setFabricId(fabricId).build())

  suspend fun readAttribute(deviceId: Long, attributePath: ChipAttributePath, call: PluginCall) {
      _call = call;
      val pointerId = getConnectedDevicePointer(deviceId)
      chipDeviceController.readAttributePath(reportCallback,
        pointerId,
        listOf(attributePath), 0)
  }

  /**
   * Wrapper around [ChipDeviceController.getConnectedDevicePointer] to return the value directly.
   */
  suspend fun getConnectedDevicePointer(nodeId: Long): Long {
    return suspendCoroutine { continuation ->
      chipDeviceController.getConnectedDevicePointer(
        nodeId,
        object : GetConnectedDeviceCallback {
          override fun onDeviceConnected(devicePointer: Long) {
            Log.d(TAG,"Got connected device pointer")
            continuation.resume(devicePointer)
          }

          override fun onConnectionFailure(nodeId: Long, error: Exception) {
            val errorMessage = "Unable to get connected device with nodeId $nodeId."
            Log.e(TAG,errorMessage, error)
            continuation.resumeWithException(IllegalStateException(errorMessage))
          }
        })
    }
  }

  fun computePaseVerifier(
    devicePtr: Long,
    pinCode: Long,
    iterations: Long,
    salt: ByteArray
  ): PaseVerifierParams {
    Log.d(TAG,
      "computePaseVerifier: devicePtr [${devicePtr}] pinCode [${pinCode}] iterations [${iterations}] salt [${salt}]")
    return chipDeviceController.computePaseVerifier(devicePtr, pinCode, iterations, salt)
  }

  fun setDeviceAttestationDelegate(failsafeTimeout: Int, deviceAttestationDelegate: DeviceAttestationDelegate){
    return chipDeviceController.setDeviceAttestationDelegate(failsafeTimeout,  deviceAttestationDelegate)
  }

  fun continueCommissioning(devicePtr: Long, ignoreAttestationFailure: Boolean){
    return chipDeviceController.continueCommissioning(devicePtr, ignoreAttestationFailure)
  }

  fun pairDevice(bleServer: BluetoothGatt?, connId: Int, deviceId: Long, setupPincode: Long, networkCredentials: NetworkCredentials){
    return chipDeviceController.pairDevice(bleServer, connId, deviceId, setupPincode, networkCredentials)
  }

  fun close() {
    return chipDeviceController.close()
  }

  fun setCompletionListener(listener: ChipDeviceController.CompletionListener){
    return chipDeviceController.setCompletionListener(listener)
  }

  suspend fun awaitEstablishPaseConnection(
    deviceId: Long,
    ipAddress: String,
    port: Int,
    setupPinCode: Long
  ) {
    return suspendCoroutine { continuation ->
      chipDeviceController.setCompletionListener(
        object : BaseCompletionListener() {
          override fun onConnectDeviceComplete() {
            super.onConnectDeviceComplete()
            continuation.resume(Unit)
          }
          // Note that an error in processing is not necessarily communicated via onError().
          // onCommissioningComplete with a "code != 0" also denotes an error in processing.
          override fun onPairingComplete(code: Int) {
            super.onPairingComplete(code)
            if (code != 0) {
              continuation.resumeWithException(
                IllegalStateException("Pairing failed with error code [${code}]"))
            } else {
              continuation.resume(Unit)
            }
          }

          override fun onError(error: Throwable) {
            super.onError(error)
            continuation.resumeWithException(error)
          }

          override fun onReadCommissioningInfo(
            vendorId: Int,
            productId: Int,
            wifiEndpointId: Int,
            threadEndpointId: Int
          ) {
            super.onReadCommissioningInfo(vendorId, productId, wifiEndpointId, threadEndpointId)
            continuation.resume(Unit)
          }

          override fun onCommissioningStatusUpdate(nodeId: Long, stage: String?, errorCode: Int) {
            super.onCommissioningStatusUpdate(nodeId, stage, errorCode)
            continuation.resume(Unit)
          }
        })

      // Temporary workaround to remove interface indexes from ipAddress
      // due to https://github.com/project-chip/connectedhomeip/pull/19394/files
      chipDeviceController.establishPaseConnection(
        deviceId, stripLinkLocalInIpAddress(ipAddress), port, setupPinCode)
    }
  }

  suspend fun awaitCommissionDevice(deviceId: Long, networkCredentials: NetworkCredentials?) {
    class Test : ChipDeviceController.NOCChainIssuer {
      override fun onNOCChainGenerationNeeded(csrInfo: CSRInfo?,
                                              attestationInfo: AttestationInfo?) {
        Log.i("dsdsdsd","dsdsdsd")
        var rootCer=  controllerParams.rootCertificate
        var vendor=  controllerParams.controllerVendorId
        var ipk=  controllerParams.ipk
        var fabricId=  controllerParams.fabricId

        chipDeviceController.onNOCChainGeneration(controllerParams)
      }
    }
//      chipDeviceController.setNOCChainIssuer(Test());

    return suspendCoroutine { continuation ->
      chipDeviceController.setCompletionListener(
        object : BaseCompletionListener() {
          // Note that an error in processing is not necessarily communicated via onError().
          // onCommissioningComplete with an "errorCode != 0" also denotes an error in processing.
          override fun onCommissioningComplete(nodeId: Long, errorCode: Int) {
            super.onCommissioningComplete(nodeId, errorCode)
            if (errorCode != 0) {
              continuation.resumeWithException(
                IllegalStateException("Commissioning failed with error code [${errorCode}]"))
            } else {
              continuation.resume(Unit)
            }
          }
          override fun onError(error: Throwable) {
            super.onError(error)
            continuation.resumeWithException(error)
          }
        })
      chipDeviceController.commissionDevice(deviceId, networkCredentials)
    }
  }

  suspend fun awaitOpenPairingWindowWithPIN(
    connectedDevicePointer: Long,
    duration: Int,
    iteration: Long,
    discriminator: Int,
    setupPinCode: Long
  ) {
    return suspendCoroutine { continuation ->
      Log.d(TAG,"Calling chipDeviceController.openPairingWindowWithPIN")
      val callback: OpenCommissioningCallback =
        object : OpenCommissioningCallback {
          override fun onError(status: Int, deviceId: Long) {
            Log.e(TAG,
              "ShareDevice: awaitOpenPairingWindowWithPIN.onError: status [${status}] device [${deviceId}]")
            continuation.resumeWithException(
              java.lang.IllegalStateException(
                "Failed opening the pairing window with status [${status}]"))
          }
          override fun onSuccess(deviceId: Long, manualPairingCode: String?, qrCode: String?) {
            Log.d(TAG,
              "ShareDevice: awaitOpenPairingWindowWithPIN.onSuccess: deviceId [${deviceId}]")
            continuation.resume(Unit)
          }
        }
      chipDeviceController.openPairingWindowWithPINCallback(
        connectedDevicePointer, duration, iteration, discriminator, setupPinCode, callback)
    }
  }

  /**
   * Wrapper around [ChipDeviceController.getConnectedDevicePointer] to return the value directly.
   */
  suspend fun awaitGetConnectedDevicePointer(nodeId: Long): Long {
    return suspendCoroutine { continuation ->
      chipDeviceController.getConnectedDevicePointer(
        nodeId,
        object : GetConnectedDeviceCallback {
          override fun onDeviceConnected(devicePointer: Long) {
            Log.d(TAG,"Got connected device pointer")
            continuation.resume(devicePointer)
          }

          override fun onConnectionFailure(nodeId: Long, error: Exception) {
            val errorMessage = "Unable to get connected device with nodeId $nodeId"
            Log.e(TAG,errorMessage, error)
            continuation.resumeWithException(IllegalStateException(errorMessage))
          }
        })
    }
  }
}
