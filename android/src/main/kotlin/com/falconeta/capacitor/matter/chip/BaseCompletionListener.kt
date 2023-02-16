package com.falconeta.capacitor.matter.chip

import android.util.Log
import chip.devicecontroller.ChipDeviceController

abstract class BaseCompletionListener : ChipDeviceController.CompletionListener {
  private final var TAG: String = "BASE COMPLETION LISTENER"
  override fun onConnectDeviceComplete() {
    Log.d(TAG, "BaseCompletionListener onConnectDeviceComplete()")
  }

  override fun onStatusUpdate(status: Int) {
    Log.d(TAG, "BaseCompletionListener onStatusUpdate(): status [${status}]")
  }

  override fun onPairingComplete(code: Int) {
    Log.d(TAG, "BaseCompletionListener onPairingComplete(): code [${code}]")
  }

  override fun onPairingDeleted(code: Int) {
    Log.d(TAG, "BaseCompletionListener onPairingDeleted(): code [${code}]")
  }

  override fun onCommissioningComplete(nodeId: Long, errorCode: Int) {
    Log.d(
      TAG,
      "BaseCompletionListener onCommissioningComplete(): nodeId [${nodeId}] errorCode [${errorCode}]"
    )
  }

  override fun onNotifyChipConnectionClosed() {
    Log.d(TAG, "BaseCompletionListener onNotifyChipConnectionClosed()")
  }

  override fun onCloseBleComplete() {
    Log.d(TAG, "BaseCompletionListener onCloseBleComplete()")
  }

  override fun onError(error: Throwable) {
    Log.e(TAG, "BaseCompletionListener onError() $error")
  }

  override fun onOpCSRGenerationComplete(csr: ByteArray) {
    Log.d(TAG, "BaseCompletionListener onOpCSRGenerationComplete() csr [${csr}]")
  }

  override fun onReadCommissioningInfo(
    vendorId: Int,
    productId: Int,
    wifiEndpointId: Int,
    threadEndpointId: Int
  ) {
    Log.d(
      TAG,
      "onReadCommissioningInfo: vendorId [${vendorId}]  productId [${productId}]  wifiEndpointId [${wifiEndpointId}] threadEndpointId [${threadEndpointId}]"
    )
  }

  override fun onCommissioningStatusUpdate(nodeId: Long, stage: String?, errorCode: Int) {
    Log.d(
      TAG,
      "onCommissioningStatusUpdate nodeId [${nodeId}]  stage [${stage}]  errorCode [${errorCode}]"
    )
  }
}
