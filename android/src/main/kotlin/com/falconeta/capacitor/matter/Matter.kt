package com.falconeta.capacitor.matter

import android.util.Log

class Matter {
    fun echo(value: String): String {
        Log.i("Echo", value)
        return value
    }
}