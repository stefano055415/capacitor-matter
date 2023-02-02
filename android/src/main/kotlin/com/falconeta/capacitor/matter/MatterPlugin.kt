package com.falconeta.capacitor.matter

import android.app.Activity
import android.content.ContentValues
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.PluginMethod
import com.getcapacitor.PluginCall
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.google.android.gms.home.matter.commissioning.CommissioningResult

@CapacitorPlugin(name = "Matter")
class MatterPlugin : Plugin() {

  private lateinit var implementation: MatterInstance;
  private lateinit var commissioningLauncher: ActivityResultLauncher<IntentSenderRequest>

  private var _call: PluginCall? = null;

  override fun load() {
    super.load();


    commissioningLauncher = bridge.activity.registerForActivityResult(
      ActivityResultContracts.StartIntentSenderForResult()
    ) { result: ActivityResult ->
      if (result.resultCode == Activity.RESULT_OK) {
        //Timber.d(TAG, "Commissioning succeeded.")
        Log.i(ContentValues.TAG, "Commissioning succeeded.");
        commissionDeviceSucceeded(result)
      } else {
        Log.i(ContentValues.TAG, "Commissioning failed. " + result.resultCode);
      }
    }

    implementation = MatterInstance(commissioningLauncher);
  }

  fun commissionDeviceSucceeded(activityResult: ActivityResult) {
    val result =
      CommissioningResult.fromIntentSenderResult(activityResult.resultCode, activityResult.data)
//    Timber.i("Device commissioned successfully! deviceName [${result.deviceName}]")
//    Timber.i("Device commissioned successfully! room [${result.room}]")
//    Timber.i(
//      "Device commissioned successfully! DeviceDescriptor of device:\n" +
//        "deviceType [${result.commissionedDeviceDescriptor.deviceType}]\n" +
//        "productId [${result.commissionedDeviceDescriptor.productId}]\n" +
//        "vendorId [${result.commissionedDeviceDescriptor.vendorId}]\n" +
//        "hashCode [${result.commissionedDeviceDescriptor.hashCode()}]")

    // Add the device to the devices repository.
//    viewModelScope.launch {
//      val deviceId = result.token?.toLong()!!
//      try {
//        Timber.d("Commissioning: Adding device to repository")
//        devicesRepository.addDevice(
//          Device.newBuilder()
//            .setName(deviceName) // default name that can be overridden by user in next step
//            .setDeviceId(deviceId)
//            .setDateCommissioned(getTimestampForNow())
//            .setVendorId(result.commissionedDeviceDescriptor.vendorId.toString())
//            .setProductId(result.commissionedDeviceDescriptor.productId.toString())
//            // Note that deviceType is now deprecated. Need to get it by introspecting
//            // the device information. This is done below.
//            .setDeviceType(
//              convertToAppDeviceType(result.commissionedDeviceDescriptor.deviceType.toLong()))
//            .build())
//        Timber.d("Commissioning: Adding device state to repository: isOnline:true isOn:false")
//        devicesStateRepository.addDeviceState(deviceId, isOnline = true, isOn = false)
//        _commissionDeviceStatus.postValue(
//          TaskStatus.Completed("Device added: [${deviceId}] [${deviceName}]"))
//      } catch (e: Exception) {
//        Timber.e("Adding device [${deviceId}] [${deviceName}] to app's repository failed", e)
//        _commissionDeviceStatus.postValue(
//          TaskStatus.Failed(
//            "Adding device [${deviceId}] [${deviceName}] to app's repository failed", e))
//      }
//
//      // Introspect the device and update its deviceType.
//      val deviceMatterInfoList = clustersHelper.fetchDeviceMatterInfo(deviceId, 0)
//      Timber.d("*** MATTER DEVICE INFO ***")
//      deviceMatterInfoList.forEachIndexed { index, deviceMatterInfo ->
//        Timber.d("Processing [[${index}] ${deviceMatterInfo}]")
//        if (index == 0) {
//          if (deviceMatterInfo.types.size > 1) {
//            // TODO: Handle this properly
//            Timber.w("The device has more than one type. We're simply using the first one.")
//          }
//          devicesRepository.updateDeviceType(
//            deviceId, convertToAppDeviceType(deviceMatterInfo.types.first()))
//        }
//      }
//    }

    val data = JSObject()
//    val deviceId = result.token?.toLong()!!

//    data.put("deviceId",deviceId.toString())
    data.put("deviceType", result.commissionedDeviceDescriptor.deviceType)
    data.put("hashCode", result.commissionedDeviceDescriptor.hashCode())
    data.put("vendorId",  result.commissionedDeviceDescriptor.vendorId)
    data.put("productId", result.commissionedDeviceDescriptor.productId)
    val ret = JSObject()
    ret.put("value", data)

    if(_call != null){
      _call!!.resolve(ret)
      _call = null;
    }
  }

  @PluginMethod
    fun echo(call: PluginCall) {

//        val value = call.getString("value")
        val ret = JSObject()
        ret.put("value", implementation.echo(context))
        _call = call;
//        call.resolve(ret)
    }
}
