package com.falconeta.capacitor.matter

import android.app.Activity.RESULT_OK
import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Context
import android.content.IntentSender
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.falconeta.capacitor.matter.commissioning.AppCommissioningService
import com.google.android.gms.home.matter.Matter
import com.google.android.gms.home.matter.commissioning.CommissioningRequest

class MatterInstance {
  private lateinit var _context: Context;

  private var _commissioningLauncher: ActivityResultLauncher<IntentSenderRequest>

  constructor(commissioningLauncher: ActivityResultLauncher<IntentSenderRequest>){
    _commissioningLauncher = commissioningLauncher
  }

    fun echo(context: Context): String {
        Log.i("Echo", "test")

        commissionDevice(context);
        return "test"
    }

  private fun commissionDevice(context: Context) {

    val commissionDeviceRequest =
      CommissioningRequest.builder()
        .setCommissioningService(ComponentName(context, AppCommissioningService::class.java))
        .build()

    // The call to commissionDevice() creates the IntentSender that will eventually be launched
    // in the fragment to trigger the commissioning activity in GPS.
    Matter.getCommissioningClient(context)
      .commissionDevice(commissionDeviceRequest)
      .addOnSuccessListener { result ->
        Log.i("TEST","ShareDevice: Success getting the IntentSender: result [${result}]")
        // Communication with fragment is via livedata
        _commissioningLauncher.launch(IntentSenderRequest.Builder(result).build())
      }
      .addOnFailureListener { error ->
//        Timber.e(error)
//        _commissionDeviceStatus.postValue(
//          TaskStatus.Failed("Setting up the IntentSender failed", error))
      }
  }
}
