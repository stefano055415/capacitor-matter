package com.falconeta.capacitor.matter

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.falconeta.capacitor.matter.chip.ChipClient
import com.falconeta.capacitor.matter.chip.ClustersHelper
import com.falconeta.capacitor.matter.commissioning.AppCommissioningService
import com.falconeta.capacitor.matter.preference.Preference
import com.google.android.gms.home.matter.Matter
import com.google.android.gms.home.matter.commissioning.CommissioningRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.Int
import kotlin.String

class MatterInstance(
  private val context: Context,
  private val commissioningLauncher: ActivityResultLauncher<IntentSenderRequest>
) {

  private var preference: Preference = Preference(context);
  private lateinit var clustersHelper: ClustersHelper;

  private val matterInstanceJob = Job()
  private val matterInstanceScope = CoroutineScope(Dispatchers.Main + matterInstanceJob)

  private var configured = false;

  fun configure(
    deviceControllerKey: String,
    caRootCert: String,
    fabricId: Long,
    vendorId: Int
  ) {
    Log.i(
      "configure",
      "deviceControllerKey $deviceControllerKey, caRootCert $caRootCert, fabricId $fabricId, vendorId: $vendorId, "
    )
    preference.setConfiguration(deviceControllerKey, caRootCert, fabricId, vendorId);
    clustersHelper = ClustersHelper(context);
    configured = true;
  }


  fun startCommissioning(deviceId: Long) {
    preference.setDeviceIdForCommissioning(deviceId);
    commissionDevice();
  }

  fun commandOnOff(deviceId: Long, value: Boolean) {
    this.matterInstanceScope.launch {
      clustersHelper.setOnOffDeviceStateOnOffCluster(deviceId, value, 1)
    }
  }

  private fun commissionDevice() {

    if(!configured){
      throw java.lang.Error("plugin must be configured first...");
    }

    val commissionDeviceRequest = CommissioningRequest.builder()
      .setCommissioningService(ComponentName(context, AppCommissioningService::class.java)).build()

    // The call to commissionDevice() creates the IntentSender that will eventually be launched
    // in the fragment to trigger the commissioning activity in GPS.
    Matter.getCommissioningClient(context).commissionDevice(commissionDeviceRequest)
      .addOnSuccessListener { result ->
        // Log.i("TEST", "ShareDevice: Success getting the IntentSender: result [${result}]")
        commissioningLauncher.launch(IntentSenderRequest.Builder(result).build())
      }.addOnFailureListener { error ->
//        Timber.e(error)
//        _commissionDeviceStatus.postValue(
//          TaskStatus.Failed("Setting up the IntentSender failed", error))
      }
  }
}
