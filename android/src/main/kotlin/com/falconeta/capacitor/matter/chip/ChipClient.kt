package com.falconeta.capacitor.matter.chip

import android.content.Context
import android.util.Log
import chip.devicecontroller.*
import chip.devicecontroller.GetConnectedDeviceCallbackJni.GetConnectedDeviceCallback
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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ChipClient(context: Context) {
  private val TAG: String = "CHIP CLIENT SERVICE"

  private val preference: Preference
  private val vendorId: Int
  private val fabricId: Long

  init {
    preference = Preference(context)
    val (vid, fid) = preference.getVendorIdAndFabricId()
    vendorId = vid;
    fabricId = fid;
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
    AndroidChipPlatform(
      AndroidBleManager(),
      PreferencesKeyValueStoreManager(context),
      PreferencesConfigurationManager(context),
      NsdManagerServiceResolver(context),
      NsdManagerServiceBrowser(context),
      ChipMdnsCallbackImpl(),
      DiagnosticDataProviderImpl(context))
    ChipDeviceController(
      controllerParams)
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
