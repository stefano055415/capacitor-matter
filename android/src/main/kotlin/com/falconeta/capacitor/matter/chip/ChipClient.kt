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
import chip.setuppayload.SetupPayload
import com.falconeta.capacitor.matter.MatterInstance
import com.falconeta.capacitor.matter.chip.setuppayloadscanner.CHIPDeviceInfo
import com.falconeta.capacitor.matter.preference.Preference
import com.falconeta.capacitor.matter.stripLinkLocalInIpAddress
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

// Iteration
const val ITERATION = 10000L

class ChipClient(context: Context) {
  private val TAG: String = "CHIP CLIENT SERVICE"

  private val preference: Preference
  private val vendorId: Int
  private val fabricId: Long
  private val deviceControllerKey: String
  private val caRoot: String
  private val controllerParams: ControllerParams;
  private lateinit var androidPlatform: AndroidChipPlatform

  init {
    preference = Preference(context)
    val (vid, fid) = preference.getVendorIdAndFabricId()
    val (deviceControllerKey, caRoot) = preference.getCerts()
    this.deviceControllerKey = deviceControllerKey;
    this.caRoot = caRoot;
    vendorId = vid;
    fabricId = fid;
    controllerParams = ControllerParams.newBuilder()
    .setUdpListenPort(0)
      .setRootCertificate(caRoot.toByteArray())
      .setControllerVendorId(vendorId)
      .setFabricId(fabricId)
      .setOperationalCertificate(deviceControllerKey.toByteArray())
      .build();
    getAndroidChipPlatform(context)

  }


  // Lazily instantiate [ChipDeviceController] and hold a reference to it.

  private val chipDeviceController: ChipDeviceController by lazy {
    ChipDeviceController(
      controllerParams)
  }

  fun getAndroidChipPlatform(context: Context?): AndroidChipPlatform {
    if (!this::androidPlatform.isInitialized && context != null) {
      //force ChipDeviceController load jni
      ChipDeviceController.loadJni()
      androidPlatform = AndroidChipPlatform(AndroidBleManager(), PreferencesKeyValueStoreManager(context), PreferencesConfigurationManager(context), NsdManagerServiceResolver(context), NsdManagerServiceBrowser(context), ChipMdnsCallbackImpl(), DiagnosticDataProviderImpl(context))
    }

    return androidPlatform
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

  suspend fun readAttribute(deviceId: Long, attributePath: ChipAttributePath, call: PluginCall) {
    val callback: ReportCallback =
      object : ReportCallback {
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
          val data = JSObject()
          data.put("value", debugString)
          call.resolve(data)
        }

        override fun onDone() {
          Log.i(TAG, "wildcard report Done")
        }
      }

      val pointerId = getConnectedDevicePointer(deviceId)
      chipDeviceController.readAttributePath(callback,
        pointerId,
        listOf(attributePath), 0)
  }

  suspend fun openCommissioningWindow(deviceId: Long, discriminator: Int, duration: Int, setupPIN: Int, call: PluginCall) {
    val callback: OpenCommissioningCallback =
      object : OpenCommissioningCallback {
        override fun onError(status: Int, deviceId: Long) {
          call.reject("-12")
        }

        override fun onSuccess(deviceId: Long, manualPairingCode: String?, qrCode: String?) {
          val data = JSObject()
          data.put("manualCode", manualPairingCode)
          call.resolve(data)
        }
      }

    try {
      val pointerId = getConnectedDevicePointer(deviceId)
      var result = chipDeviceController.openPairingWindowWithPINCallback(pointerId, duration, ITERATION, discriminator,  setupPIN.toLong(), callback)
      if(!result){
        call.reject("-12")
      }
    } catch (ex: Exception){
      call.reject("-11")
    }
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


  fun discoverCommissionableNodes(){
    chipDeviceController.discoverCommissionableNodes()
  }

  fun pairDeviceWithIpAddress(deviceId: Long, discriminator: Int, pinCode: Long){
    for(i in 0..10) {
      val device = chipDeviceController.getDiscoveredDevice(i) ?: break

      if (device.discriminator.toInt() == discriminator) {  // TODO: WORK IN PROGRESS
        this.chipDeviceController.pairDeviceWithAddress(deviceId, device.ipAddress, 5540, discriminator, pinCode, null)
      }
      Log.d(TAG,
        "ip address [${device.ipAddress}]")
    }
  }

  fun close() {
    return chipDeviceController.close()
  }

  fun setCompletionListener(listener: ChipDeviceController.CompletionListener){
    return chipDeviceController.setCompletionListener(listener)
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
