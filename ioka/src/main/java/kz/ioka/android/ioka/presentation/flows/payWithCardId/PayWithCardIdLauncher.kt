package kz.ioka.android.ioka.presentation.flows.payWithCardId

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PayWithCardIdLauncher(
    val customerToken: String,
    val orderToken: String,
    val cardId: String
) : Parcelable