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

@CapacitorPlugin(name = "Matter")
class MatterPlugin : Plugin() {

  private lateinit var implementation: MatterInstance;
  private lateinit var commissioningLauncher: ActivityResultLauncher<IntentSenderRequest>

  override fun load() {
    super.load();


    commissioningLauncher = bridge.activity.registerForActivityResult(
      ActivityResultContracts.StartIntentSenderForResult()
    ) { result: ActivityResult ->
      if (result.resultCode == Activity.RESULT_OK) {
        //Timber.d(TAG, "Commissioning succeeded.")
        Log.i(ContentValues.TAG, "Commissioning succeeded.");
      } else {
        Log.i(ContentValues.TAG, "Commissioning failed. " + result.resultCode);
      }
    }

    implementation = MatterInstance(commissioningLauncher);

  }

    @PluginMethod
    fun echo(call: PluginCall) {
        val value = call.getString("value")
        val ret = JSObject()
        ret.put("value", implementation.echo(context))
        call.resolve(ret)
    }
}
