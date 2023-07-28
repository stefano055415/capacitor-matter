package com.falconeta.capacitor.matter.chip.setuppayloadscanner

import android.os.Parcelable
import chip.setuppayload.OptionalQRCodeInfo.OptionalQRCodeInfoType
import kotlinx.parcelize.Parcelize

@Parcelize
data class QrCodeInfo(
    val tag: Int,
    val type: OptionalQRCodeInfoType,
    val data: String,
    val intDataValue: Int
) : Parcelable
