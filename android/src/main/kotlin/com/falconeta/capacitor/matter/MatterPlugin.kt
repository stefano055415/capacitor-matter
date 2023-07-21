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
import org.xml.sax.ErrorHandler

@CapacitorPlugin(name = "Matter")
class MatterPlugin : Plugin() {

  private lateinit var implementation: MatterInstance;

  override fun load() {
    super.load();
    implementation = MatterInstance(context, bridge);
  }

  @PluginMethod
  fun configure(call: PluginCall) {
    val deviceControllerKey = call.getString("deviceControllerKey")
    val caRootCert = call.getString("caRootCert")
    val fabricStringId = call.getString("fabricId")
    val vendorId = call.getInt("vendorId")
//    if (deviceControllerKey == null || caRootCert == null || fabricStringId == null || vendorId == null) {
//      call.reject("params must be exist!")
//      return;
//    }

    if (fabricStringId == null || vendorId == null) {
      call.reject("params must be exist!")
      return;
    }

    try {
      val fabricId = fabricStringId.toLong()
      implementation.configure(deviceControllerKey, caRootCert, fabricId, vendorId)
      call.resolve()
    } catch (error: NumberFormatException) {
      call.reject("fabricId must be a number and not major of 9223372036854775807")
    }

  }

  @PluginMethod
  fun clear(call: PluginCall) {
    implementation.clear()
    call.resolve()
  }

  @PluginMethod
  fun getCerts(call: PluginCall) {
    var (deviceControllerKey, caRootCert) = implementation.getCerts()
    val data = JSObject()
    data.put("deviceControllerKey", deviceControllerKey)
    data.put("caRootCert", caRootCert)

    call.resolve(data)
  }


  @PluginMethod
  fun startCommissioning(call: PluginCall) {
    val deviceStringId = call.getString("deviceId")
    if (deviceStringId == null) {
      call.reject("deviceId must be exist!")
      return;
    }

    try {
      val deviceId = deviceStringId.toLong()
      implementation.startCommissioning(deviceId, call)

    } catch (error: NumberFormatException) {
      call.reject("deviceId must be a number and not major of 9223372036854775807")
    }

  }

  @PluginMethod
  fun commandOnOff(call: PluginCall) {
    val deviceStringId = call.getString("deviceId")
    val endpointId = call.getInt("endpointId")
    val value = call.getBoolean("value")
    if (deviceStringId == null || value == null || endpointId == null) {
      call.reject("deviceId and value and endpointId must be exist!")
      return;
    }

    try {
      val deviceId = deviceStringId.toLong()
      implementation.commandOnOff(deviceId, endpointId, value)
      call.resolve()
//      _call = call;
    } catch (error: NumberFormatException) {
      call.reject("deviceId must be a number and not major of 9223372036854775807")
    }

  }

  @PluginMethod
  fun readAttribute(call: PluginCall) {
    val deviceStringId = call.getString("deviceId")
    val endpointId = call.getInt("endpointId")
    val clusterId = call.getInt("clusterId")
    val attributeId = call.getInt("attributeId")

    if (deviceStringId == null || endpointId == null || clusterId == null || attributeId == null) {
      call.reject("params must be exist!")
      return;
    }

    try {
      val deviceId = deviceStringId.toLong()
      implementation.readAttribute(deviceId, endpointId, clusterId, attributeId, call)
    } catch (error: NumberFormatException) {
      call.reject("deviceId must be a number and not major of 9223372036854775807")
    }

  }
}
