package kz.ioka.android.ioka.presentation.flows.payment

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kz.ioka.android.ioka.api.Configuration
import kz.ioka.android.ioka.api.GooglePayConfiguration
import kz.ioka.android.ioka.presentation.flows.common.OrderDvo

@Parcelize
internal data class PayLauncher(
    val orderToken: String,
    val order: OrderDvo,
    val googlePayConfiguration: GooglePayConfiguration? = null,
    val canSaveCard: Boolean,
    val configuration: Configuration? = null
) : Parcelable