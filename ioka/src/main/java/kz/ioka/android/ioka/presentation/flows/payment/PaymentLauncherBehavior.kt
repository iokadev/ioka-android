package kz.ioka.android.ioka.presentation.flows.payment

import android.content.Intent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kz.ioka.android.ioka.R
import kz.ioka.android.ioka.api.Configuration
import kz.ioka.android.ioka.api.GooglePayConfiguration
import kz.ioka.android.ioka.di.DependencyInjector
import kz.ioka.android.ioka.domain.errorHandler.ResultWrapper
import kz.ioka.android.ioka.domain.order.OrderRepositoryImpl
import kz.ioka.android.ioka.presentation.flows.common.OrderDvo
import kz.ioka.android.ioka.presentation.launcher.PaymentLauncherBehavior
import kz.ioka.android.ioka.util.ViewAction
import kz.ioka.android.ioka.util.getOrderId
import kz.ioka.android.ioka.util.showErrorToast
import kz.ioka.android.ioka.viewBase.BaseActivity

@Parcelize
internal class PaymentLauncherBehavior(
    private val orderToken: String,
    private val googlePayConfiguration: GooglePayConfiguration? = null,
    private val configuration: Configuration? = null
) : PaymentLauncherBehavior {

    @IgnoredOnParcel
    private val orderRepository = OrderRepositoryImpl(DependencyInjector.orderApi)

    @IgnoredOnParcel
    private var order: OrderDvo? = null

    @IgnoredOnParcel
    private var customerId: String? = null

    override val titleRes: Int get() = R.string.ioka_common_processing_payment

    override fun observeProgress(): Flow<Boolean> = flowOf(true)

    override suspend fun doOnLoading() {
        val orderResponse = orderRepository.getOrderById(orderToken.getOrderId())

        if (orderResponse is ResultWrapper.Success) {
            order = OrderDvo(
                orderResponse.value.externalId,
                orderResponse.value.amount,
            )
            customerId = orderResponse.value.customerId
        }
    }

    override fun doAfterLoading(): ViewAction {
        return if (order == null) {
            ViewAction {
                it.showErrorToast(it.getString(R.string.ioka_common_server_error))
                it.finish()
            }
        } else {
            ViewAction {
                val intent = Intent(it, PayActivity::class.java)
                intent.putExtra(
                    BaseActivity.LAUNCHER,
                    PayLauncher(
                        orderToken,
                        order!!,
                        googlePayConfiguration,
                        customerId != null,
                        configuration
                    )
                )
                it.startActivity(intent)
                it.finish()
            }
        }
    }
}