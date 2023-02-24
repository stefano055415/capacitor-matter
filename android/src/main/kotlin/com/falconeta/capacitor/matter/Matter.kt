package com.falconeta.capacitor.matter

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import chip.devicecontroller.model.ChipAttributePath
import chip.devicecontroller.model.ChipPathId
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

  fun startCommissioning(deviceId: Long) {
    preference.setDeviceIdForCommissioning(deviceId);
    commissionDevice();
  }

  fun commandOnOff(deviceId: Long, value: Boolean) {
    this.matterInstanceScope.launch {
      clustersHelper.setOnOffDeviceStateOnOffCluster(deviceId, value, 1)
    }
  }

  fun readAttribute(deviceId: Long, endpointId: Int, clusterId: Int, attributeId: Int) {
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

      chipClient.readAttribute(deviceId, attributePath);

//      deviceController.readPath(reportCallback,
//        ChipClient.getConnectedDevicePointer(requireContext(),
//          addressUpdateFragment.deviceId),
//        listOf(attributePath),
//        null,
//        isFabricFiltered)
    }
  }

  private fun getChipPathIdForText(text: String): ChipPathId {
    return if (text.isEmpty()) ChipPathId.forWildcard() else ChipPathId.forId(text.toLong())
  }

  private fun commissionDevice() {

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
        commissioningLauncher.launch(IntentSenderRequest.Builder(result).build())
      }.addOnFailureListener { error ->
//        Timber.e(error)
//        _commissionDeviceStatus.postValue(
//          TaskStatus.Failed("Setting up the IntentSender failed", error))
      }
  }
}
