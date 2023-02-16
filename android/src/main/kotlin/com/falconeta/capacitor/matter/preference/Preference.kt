package com.falconeta.capacitor.matter.preference

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences

class Preference(context: Context) {

  private interface PreferencesOperation {
    fun execute(editor: SharedPreferences.Editor?)
  }


  private fun get(key: String?): String? {
    return preferences.getString(key, null)
  }

  private fun set(key: String?, value: String?) {
    class Preference : PreferencesOperation {
      override fun execute(editor: SharedPreferences.Editor?) {
        if (editor == null) {
          return;
        }
        editor.putString(
          key, value
        )
      }

    }
    executeOperation(Preference())
  }

  private fun remove(key: String) {

    class Preference : PreferencesOperation {
      override fun execute(editor: SharedPreferences.Editor?) {
        if (editor == null) {
          return;
        }
        editor.remove(
          key
        )
      }

    }

    executeOperation(Preference())
  }

  public fun clear() {
    class Preference : PreferencesOperation {
      override fun execute(editor: SharedPreferences.Editor?) {
        if (editor == null) {
          return;
        }
        editor.clear()
      }
    }
    executeOperation(Preference())
  }

  public fun getVendorIdAndFabricId(): Pair<Int, Long> {
    val vendorId = (get("vendorId") ?: "65521").toInt()
    val fabricId = (get("fabricId") ?: "1").toLong()
    return Pair(vendorId, fabricId)
  }

  public fun setConfiguration(
    deviceControllerKey: String,
    caRootCert: String,
    fabricId: kotlin.Long,
    vendorId: Int
  ) {

    var caRootKey = "AndroidCARootCert" + java.lang.Long.toHexString(fabricId)

    set("AndroidCARootCert", caRootCert);
    set("AndroidDeviceControllerKey", deviceControllerKey);
    set("fabricId", fabricId.toString());
    set("vendorId", vendorId.toString());
  }

  public fun setDeviceIdForCommissioning(deviceId: Long) {

    set("deviceIdForCommissioning", deviceId.toString());
  }

  public fun getDeviceIdForCommissioning(): Long {
    return (get("deviceIdForCommissioning") ?: "1").toLong()
  }

  public fun removeDeviceIdForCommissioning() {
    remove("deviceIdForCommissioning")
  }

  private val preferences: SharedPreferences;

  init {
    preferences = context.getSharedPreferences("chip.platform.KeyValueStore", Activity.MODE_PRIVATE)
  }

  private fun executeOperation(op: PreferencesOperation) {
    val editor = preferences.edit()
    op.execute(editor)
    editor.apply()
  }

}
