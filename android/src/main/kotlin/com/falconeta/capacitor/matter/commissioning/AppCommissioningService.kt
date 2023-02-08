/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.falconeta.capacitor.matter.commissioning

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.falconeta.capacitor.matter.chip.ChipClient
import com.google.android.gms.home.matter.commissioning.CommissioningCompleteMetadata
import com.google.android.gms.home.matter.commissioning.CommissioningRequestMetadata
import com.google.android.gms.home.matter.commissioning.CommissioningService
//import com.google.homesampleapp.APP_NAME
//import com.google.homesampleapp.R
//import com.google.homesampleapp.chip.ChipClient
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * The CommissioningService that's responsible for commissioning the device on the app's custom
 * fabric. AppCommissioningService is specified when building the
 * [com.google.android.gms.home.matter.commissioning.CommissioningRequest] in
 * [../screens.home.HomeViewModel].
 */
@AndroidEntryPoint
class AppCommissioningService : Service(), CommissioningService.Callback {

//  @Inject internal lateinit var devicesRepository: DevicesRepository
//  @Inject internal lateinit var devicesStateRepository: DevicesStateRepository
  @Inject internal lateinit var chipClient: ChipClient

  private final var TAG: String = "MATTER COMMISSIONING SERVICE"

  private val serviceJob = Job()
  private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

  private lateinit var commissioningServiceDelegate: CommissioningService

  override fun onCreate() {
    super.onCreate()
    // May be invoked without MainActivity being called to initialize APP_NAME.
    // So do it here as well.
    Log.i(TAG,"onCreate()")
    commissioningServiceDelegate = CommissioningService.Builder(this).setCallback(this).build()
  }

  override fun onBind(intent: Intent): IBinder {
    Log.i(TAG,"onBind(): intent [${intent}]")
    return commissioningServiceDelegate.asBinder()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Log.i(TAG,"onStartCommand(): intent [${intent}] flags [${flags}] startId [${startId}]")
    return super.onStartCommand(intent, flags, startId)
  }

  override fun onDestroy() {
    super.onDestroy()
    Log.i(TAG,"onDestroy()")
//    serviceJob.cancel()
  }

  override fun onCommissioningRequested(metadata: CommissioningRequestMetadata) {
    Log.i(TAG,
        "*** onCommissioningRequested ***:\n" +
            "\tdeviceDescriptor: " +
            "deviceType [${metadata.deviceDescriptor.deviceType}] " +
            "vendorId [${metadata.deviceDescriptor.vendorId}] " +
            "productId [${metadata.deviceDescriptor.productId}]\n" +
            "\tnetworkLocation: " +
            "IP address toString() [${metadata.networkLocation.ipAddress}] " +
            "IP address hostAddress [${metadata.networkLocation.ipAddress.hostAddress}] " +
            "port [${metadata.networkLocation.port}]\n" +
            "\tpassCode [${metadata.passcode}]")

    // CODELAB: onCommissioningRequested()
    // Perform commissioning on custom fabric for the sample app.

//    commissioningServiceDelegate
//      .sendCommissioningComplete(
//        CommissioningCompleteMetadata.builder().build())
//      .addOnSuccessListener {
//        Log.i(TAG,
//          "Commissioning: OnSuccess for commissioningServiceDelegate.sendCommissioningComplete()")
//      }
//      .addOnFailureListener { ex ->
//        Log.e(TAG,"Commissioning: Failed to send commissioning complete.", ex)
//      }


    serviceScope.launch {
      val deviceId: Long = 1 // devicesRepository.incrementAndReturnLastDeviceId()
      Log.i(TAG,
          "Commissioning: App fabric -> ChipClient.establishPaseConnection(): deviceId [${deviceId}]")
      chipClient.awaitEstablishPaseConnection(
          deviceId,
          metadata.networkLocation.ipAddress.hostAddress!!,
          metadata.networkLocation.port,
          metadata.passcode)
      Log.i(TAG,"Commissioning: App fabric -> ChipClient.commissionDevice(): deviceId [${deviceId}]")
      chipClient.awaitCommissionDevice(deviceId, null)

      Log.i(TAG,"Commissioning: Calling commissioningServiceDelegate.sendCommissioningComplete()")
      commissioningServiceDelegate
          .sendCommissioningComplete(
              CommissioningCompleteMetadata.builder().setToken(deviceId.toString()).build())
          .addOnSuccessListener {
            Log.i(TAG,
                "Commissioning: OnSuccess for commissioningServiceDelegate.sendCommissioningComplete()")
          }
          .addOnFailureListener { ex ->
            Log.e(TAG, "Commissioning: Failed to send commissioning complete $ex")
          }
    }
  }
}
