package com.falconeta.capacitor.matter

import android.app.Activity
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import chip.devicecontroller.model.ChipAttributePath
import chip.devicecontroller.model.ChipPathId
import com.falconeta.capacitor.matter.chip.ChipClient
import com.falconeta.capacitor.matter.chip.ClustersHelper
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
import kotlinx.coroutines.launch
import kotlin.Int
import kotlin.String

class MatterInstance(
  private val context: Context,
  bridge: Bridge
) {

  private var _call: PluginCall? = null;

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
  private val matterInstanceScope = CoroutineScope(Dispatchers.Main + matterInstanceJob)

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

  fun startCommissioning(deviceId: Long, call: PluginCall) {
    preference.setDeviceIdForCommissioning(deviceId);
    commissionDevice(call);
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

    val commissionDeviceRequest = CommissioningRequest.builder()
      .setCommissioningService(ComponentName(context, AppCommissioningService::class.java)).build()

    // The call to commissionDevice() creates the IntentSender that will eventually be launched
    // in the fragment to trigger the commissioning activity in GPS.
    Matter.getCommissioningClient(context).commissionDevice(commissionDeviceRequest)
      .addOnSuccessListener { result ->
        // Log.i("TEST", "ShareDevice: Success getting the IntentSender: result [${result}]")
        _call = call;
        commissioningLauncher.launch(IntentSenderRequest.Builder(result).build())


      }.addOnFailureListener { error ->
        call.reject("reject")
//        Timber.e(error)
//        _commissionDeviceStatus.postValue(
//          TaskStatus.Failed("Setting up the IntentSender failed", error))
      }
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
}
