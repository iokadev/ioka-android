package kz.ioka.android.ioka.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GooglePayConfiguration(
    val merchantIdentifier: String,
    val countryCode: String
) : Parcelable