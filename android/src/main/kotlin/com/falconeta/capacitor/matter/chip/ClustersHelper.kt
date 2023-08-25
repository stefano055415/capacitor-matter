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

package com.falconeta.capacitor.matter.chip

import android.content.Context
import android.util.Log
import chip.devicecontroller.ChipClusters
import chip.devicecontroller.ChipStructs
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Encapsulates the information of interest when querying a Matter device just after it has been
 * commissioned.
 */
data class DeviceMatterInfo(
  val endpoint: Int,
  val types: List<Long>,
  val serverClusters: List<Any>,
  val clientClusters: List<Any>
)

/** Singleton to facilitate access to Clusters functionality. */
class ClustersHelper(context: Context, chipClient: ChipClient) {
  private val chipClient: ChipClient


  private final var TAG: String = "CLUSTERS HELPER SERVICE"


  init {
    this.chipClient = chipClient;
  }

  // -----------------------------------------------------------------------------------------------
  // Convenience functions

  /** Fetches MatterDeviceInfo for each endpoint supported by the device. */
  suspend fun fetchDeviceMatterInfo(nodeId: Long, endpoint: Int): List<DeviceMatterInfo> {
    Log.d(TAG, "fetchDeviceMatterInfo(): nodeId [${nodeId}]")
    val matterDeviceInfoList = arrayListOf<DeviceMatterInfo>()
    val connectedDevicePtr =
      try {
        chipClient.getConnectedDevicePointer(nodeId)
      } catch (e: IllegalStateException) {
        Log.e(TAG, "Can't get connectedDevicePointer.")
        return emptyList()
      }

    val partsListAttribute = readDescriptorClusterPartsListAttribute(connectedDevicePtr, endpoint)
    Log.d(TAG, "partsListAttribute [${partsListAttribute}]")

    // For each part (endpoint)
    partsListAttribute?.forEach { part ->
      Log.d(TAG, "part [$part] is [${part.javaClass}]")
      val endpointInt =
        when (part) {
          is Int -> part.toInt()
          else -> return@forEach
        }
      Log.d(TAG, "Processing part [$part]")

      // DeviceListAttribute
      val deviceListAttribute =
        readDescriptorClusterDeviceListAttribute(connectedDevicePtr, endpointInt)
      val types = arrayListOf<Long>()
      deviceListAttribute.forEach { types.add(it.deviceType) }

      // ServerListAttribute
      val serverListAttribute =
        readDescriptorClusterServerListAttribute(connectedDevicePtr, endpointInt)
      val serverClusters = arrayListOf<Any>()
      serverListAttribute.forEach { serverClusters.add(it) }

      // ClientListAttribute
      val clientListAttribute =
        readDescriptorClusterClientListAttribute(connectedDevicePtr, endpointInt)
      val clientClusters = arrayListOf<Any>()
      clientListAttribute.forEach { clientClusters.add(it) }

      // Build the DeviceMatterInfo
      val deviceMatterInfo = DeviceMatterInfo(endpointInt, types, serverClusters, clientClusters)
      matterDeviceInfoList.add(deviceMatterInfo)
    }
    return matterDeviceInfoList
  }

  // -----------------------------------------------------------------------------------------------
  // DescriptorCluster functions

  /**
   * PartsListAttribute. These are the endpoints supported.
   *
   * ```
   * For example, on endpoint 0:
   *     sendReadPartsListAttribute part: [1]
   *     sendReadPartsListAttribute part: [2]
   * ```
   */
  suspend fun readDescriptorClusterPartsListAttribute(devicePtr: Long, endpoint: Int): List<Any>? {
    return suspendCoroutine { continuation ->
      getDescriptorClusterForDevice(devicePtr, endpoint)
        .readPartsListAttribute(
          object : ChipClusters.DescriptorCluster.PartsListAttributeCallback {
            override fun onSuccess(values: MutableList<Int>?) {
              continuation.resume(values)
            }

            override fun onError(ex: Exception) {
              continuation.resumeWithException(ex)
            }
          })
    }
  }

  /**
   * DeviceListAttribute
   *
   * ```
   * For example, on endpoint 0:
   *   device: [long type: 22, int revision: 1] -> maps to Root node (0x0016) (utility device type)
   * on endpoint 1:
   *   device: [long type: 256, int revision: 1] -> maps to On/Off Light (0x0100)
   * ```
   */
  suspend fun readDescriptorClusterDeviceListAttribute(
    devicePtr: Long,
    endpoint: Int
  ): List<ChipStructs.DescriptorClusterDeviceTypeStruct> {
    return suspendCoroutine { continuation ->
      getDescriptorClusterForDevice(devicePtr, endpoint)
        .readDeviceTypeListAttribute(
          object : ChipClusters.DescriptorCluster.DeviceTypeListAttributeCallback {
            override fun onSuccess(
              values: List<ChipStructs.DescriptorClusterDeviceTypeStruct>
            ) {
              continuation.resume(values)
            }

            override fun onError(ex: Exception) {
              continuation.resumeWithException(ex)
            }
          })
    }
  }

  /**
   * ServerListAttribute See
   * https://github.com/project-chip/connectedhomeip/blob/master/zzz_generated/app-common/app-common/zap-generated/ids/Clusters.h
   *
   * ```
   * For example: on endpoint 0
   *     sendReadServerListAttribute: [3]
   *     sendReadServerListAttribute: [4]
   *     sendReadServerListAttribute: [29]
   *     ... and more ...
   * on endpoint 1:
   *     sendReadServerListAttribute: [3]
   *     sendReadServerListAttribute: [4]
   *     sendReadServerListAttribute: [5]
   *     sendReadServerListAttribute: [6]
   *     sendReadServerListAttribute: [7]
   *     ... and more ...
   * on endpoint 2:
   *     sendReadServerListAttribute: [4]
   *     sendReadServerListAttribute: [6]
   *     sendReadServerListAttribute: [29]
   *     sendReadServerListAttribute: [1030]
   *
   * Some mappings:
   *     namespace Groups = 0x00000004 (4)
   *     namespace OnOff = 0x00000006 (6)
   *     namespace Descriptor = 0x0000001D (29)
   *     namespace OccupancySensing = 0x00000406 (1030)
   * ```
   */
  suspend fun readDescriptorClusterServerListAttribute(devicePtr: Long, endpoint: Int): List<Long> {
    return suspendCoroutine { continuation ->
      getDescriptorClusterForDevice(devicePtr, endpoint)
        .readServerListAttribute(
          object : ChipClusters.DescriptorCluster.ServerListAttributeCallback {
            override fun onSuccess(values: MutableList<Long>) {
              continuation.resume(values)
            }

            override fun onError(ex: Exception) {
              continuation.resumeWithException(ex)
            }
          })
    }
  }

  /** ClientListAttribute */
  suspend fun readDescriptorClusterClientListAttribute(devicePtr: Long, endpoint: Int): List<Long> {
    return suspendCoroutine { continuation ->
      getDescriptorClusterForDevice(devicePtr, endpoint)
        .readClientListAttribute(
          object : ChipClusters.DescriptorCluster.ClientListAttributeCallback {
            override fun onSuccess(values: MutableList<Long>) {
              continuation.resume(values)
            }

            override fun onError(ex: Exception) {
              continuation.resumeWithException(ex)
            }
          })
    }
  }

  private fun getDescriptorClusterForDevice(
    devicePtr: Long,
    endpoint: Int
  ): ChipClusters.DescriptorCluster {
    return ChipClusters.DescriptorCluster(devicePtr, endpoint)
  }

  // -----------------------------------------------------------------------------------------------
  // ApplicationCluster functions

  suspend fun readApplicationBasicClusterAttributeList(deviceId: Long, endpoint: Int): List<Long> {
    val connectedDevicePtr =
      try {
        chipClient.getConnectedDevicePointer(deviceId)
      } catch (e: IllegalStateException) {
        Log.e(TAG, "Can't get connectedDevicePointer.")
        return emptyList()
      }
    return suspendCoroutine { continuation ->
      getApplicationBasicClusterForDevice(connectedDevicePtr, endpoint)
        .readAttributeListAttribute(
          object : ChipClusters.ApplicationBasicCluster.AttributeListAttributeCallback {
            override fun onSuccess(value: MutableList<Long>) {
              continuation.resume(value)
            }

            override fun onError(ex: Exception) {
              continuation.resumeWithException(ex)
            }
          })
    }
  }

  private fun getApplicationBasicClusterForDevice(
    devicePtr: Long,
    endpoint: Int
  ): ChipClusters.ApplicationBasicCluster {
    return ChipClusters.ApplicationBasicCluster(devicePtr, endpoint)
  }

  // -----------------------------------------------------------------------------------------------
  // BasicCluster functions

  suspend fun readBasicClusterVendorIDAttribute(deviceId: Long, endpoint: Int): Int? {
    val connectedDevicePtr =
      try {
        chipClient.getConnectedDevicePointer(deviceId)
      } catch (e: IllegalStateException) {
        Log.e(TAG, "Can't get connectedDevicePointer.")
        return null
      }
    return suspendCoroutine { continuation ->
      getBasicClusterForDevice(connectedDevicePtr, endpoint)
        .readVendorIDAttribute(
          object : ChipClusters.IntegerAttributeCallback {
            override fun onSuccess(value: Int) {
              continuation.resume(value)
            }

            override fun onError(ex: Exception) {
              continuation.resumeWithException(ex)
            }
          })
    }
  }

  suspend fun readBasicClusterAttributeList(deviceId: Long, endpoint: Int): List<Long> {
    val connectedDevicePtr =
      try {
        chipClient.getConnectedDevicePointer(deviceId)
      } catch (e: IllegalStateException) {
        Log.e(TAG, "Can't get connectedDevicePointer.")
        return emptyList()
      }

    return suspendCoroutine { continuation ->
      getBasicClusterForDevice(connectedDevicePtr, endpoint)
        .readAttributeListAttribute(
          object : ChipClusters.ApplicationBasicCluster.AttributeListAttributeCallback {
            override fun onSuccess(values: MutableList<Long>) {
              continuation.resume(values)
            }

            override fun onError(ex: Exception) {
              continuation.resumeWithException(ex)
            }
          })
    }
  }

  private fun getBasicClusterForDevice(
    devicePtr: Long,
    endpoint: Int
  ): ChipClusters.ApplicationBasicCluster {
    return ChipClusters.ApplicationBasicCluster(devicePtr, endpoint)
  }

  // -----------------------------------------------------------------------------------------------
  // OnOffCluster functions

  // CODELAB FEATURED BEGIN
  suspend fun toggleDeviceStateOnOffCluster(deviceId: Long, endpoint: Int) {
    Log.d(TAG, "toggleDeviceStateOnOffCluster())")
    val connectedDevicePtr =
      try {
        chipClient.getConnectedDevicePointer(deviceId)
      } catch (e: IllegalStateException) {
        Log.e(TAG, "Can't get connectedDevicePointer.")
        return
      }
    return suspendCoroutine { continuation ->
      getOnOffClusterForDevice(connectedDevicePtr, endpoint)
        .toggle(
          object : ChipClusters.DefaultClusterCallback {
            override fun onSuccess() {
              continuation.resume(Unit)
            }

            override fun onError(ex: Exception) {
              Log.e(TAG, ex.toString() + " readOnOffAttribute command failure")
              continuation.resumeWithException(ex)
            }
          })
    }
  }
  // CODELAB FEATURED END

  suspend fun setOnOffDeviceStateOnOffCluster(deviceId: Long, isOn: Boolean, endpoint: Int) {
    Log.d(
      TAG,
      "setOnOffDeviceStateOnOffCluster() [${deviceId}] isOn [${isOn}] endpoint [${endpoint}]"
    )
    val connectedDevicePtr =
      try {
        chipClient.getConnectedDevicePointer(deviceId)
      } catch (e: IllegalStateException) {
        Log.e(TAG, "Can't get connectedDevicePointer.")
        return
      }
    if (isOn) {
      // ON
      return suspendCoroutine { continuation ->
        getOnOffClusterForDevice(connectedDevicePtr, endpoint)
          .on(
            object : ChipClusters.DefaultClusterCallback {
              override fun onSuccess() {
                Log.d(TAG, "Success for setOnOffDeviceStateOnOffCluster")
                continuation.resume(Unit)
              }

              override fun onError(ex: Exception) {
                Log.e(TAG, ex.toString() + " Failure for setOnOffDeviceStateOnOffCluster")
                continuation.resumeWithException(ex)
              }
            })
      }
    } else {
      // OFF
      return suspendCoroutine { continuation ->
        getOnOffClusterForDevice(connectedDevicePtr, endpoint)
          .off(
            object : ChipClusters.DefaultClusterCallback {
              override fun onSuccess() {
                Log.d(TAG, "Success for getOnOffDeviceStateOnOffCluster")
                continuation.resume(Unit)
              }

              override fun onError(ex: Exception) {
                Log.e(TAG, ex.toString() + " Failure for getOnOffDeviceStateOnOffCluster")
                continuation.resumeWithException(ex)
              }
            })
      }
    }
  }

  suspend fun getDeviceStateOnOffCluster(deviceId: Long, endpoint: Int): Boolean? {
    Log.d(TAG, "getDeviceStateOnOffCluster())")
    val connectedDevicePtr =
      try {
        chipClient.getConnectedDevicePointer(deviceId)
      } catch (e: IllegalStateException) {
        Log.e(TAG, "Can't get connectedDevicePointer.")
        return null
      }
    return suspendCoroutine { continuation ->
      getOnOffClusterForDevice(connectedDevicePtr, endpoint)
        .readOnOffAttribute(
          object : ChipClusters.BooleanAttributeCallback {
            override fun onSuccess(value: Boolean) {
              continuation.resume(value)
            }

            override fun onError(ex: Exception) {
              Log.e(TAG, ex.toString() + " readOnOffAttribute command failure")
              continuation.resumeWithException(ex)
            }
          })
    }
  }

  private fun getOnOffClusterForDevice(devicePtr: Long, endpoint: Int): ChipClusters.OnOffCluster {
    return ChipClusters.OnOffCluster(devicePtr, endpoint)
  }

  // -----------------------------------------------------------------------------------------------
  // Administrator Commissioning Cluster (11.19)

  suspend fun openCommissioningWindowAdministratorCommissioningCluster(
    deviceId: Long,
    endpoint: Int,
    timeoutSeconds: Int,
    pakeVerifier: ByteArray,
    discriminator: Int,
    iterations: Long,
    salt: ByteArray,
    timedInvokeTimeoutMs: Int
  ) {
    Log.d(TAG, "openCommissioningWindowAdministratorCommissioningCluster())")
    val connectedDevicePtr =
      try {
        chipClient.getConnectedDevicePointer(deviceId)
      } catch (e: IllegalStateException) {
        Log.e(TAG, e.toString() + " Can't get connectedDevicePointer.")
        return
      }

    /*
    ChipClusters.DefaultClusterCallback var1, Integer var2, byte[] var3, Integer var4, Long var5, byte[] var6, int var7
     */
    return suspendCoroutine { continuation ->
      getAdministratorCommissioningClusterForDevice(connectedDevicePtr, endpoint)
        .openCommissioningWindow(
          object : ChipClusters.DefaultClusterCallback {
            override fun onSuccess() {
              continuation.resume(Unit)
            }

            override fun onError(ex: java.lang.Exception?) {
              Log.e(
                TAG,
                ex.toString() +
                  " getAdministratorCommissioningClusterForDevice.openCommissioningWindow command failure"
              )
              continuation.resumeWithException(ex!!)
            }
          },
          timeoutSeconds,
          pakeVerifier,
          discriminator,
          iterations,
          salt,
          timedInvokeTimeoutMs
        )
    }
  }

  private fun getAdministratorCommissioningClusterForDevice(
    devicePtr: Long,
    endpoint: Int
  ): ChipClusters.AdministratorCommissioningCluster {
    return ChipClusters.AdministratorCommissioningCluster(devicePtr, endpoint)
  }
}
