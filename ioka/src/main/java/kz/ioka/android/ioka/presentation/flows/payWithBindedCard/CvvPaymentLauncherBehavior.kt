package kz.ioka.android.ioka.presentation.flows.payWithBindedCard

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kz.ioka.android.ioka.R
import kz.ioka.android.ioka.di.DependencyInjector
import kz.ioka.android.ioka.domain.errorHandler.ResultWrapper
import kz.ioka.android.ioka.domain.order.OrderRepositoryImpl
import kz.ioka.android.ioka.presentation.flows.common.OrderDvo
import kz.ioka.android.ioka.presentation.launcher.PaymentLauncherBehavior
import kz.ioka.android.ioka.util.ViewAction
import kz.ioka.android.ioka.util.getOrderId
import kz.ioka.android.ioka.util.showErrorToast

@Parcelize
internal class CvvPaymentLauncherBehavior(
    private val customerToken: String,
    private val orderToken: String,
    private val cardId: String,
    private val cardNumber: String,
    private val cardType: String,
) : PaymentLauncherBehavior {

    @IgnoredOnParcel
    private val orderRepository = OrderRepositoryImpl(DependencyInjector.orderApi)

    @IgnoredOnParcel
    private var order: OrderDvo? = null

    @IgnoredOnParcel
    private val progressFlow = MutableStateFlow(true)

    override val titleRes: Int get() = R.string.ioka_common_processing_payment

    override fun observeProgress(): Flow<Boolean> = progressFlow

    override suspend fun doOnLoading() {
        val orderResponse = orderRepository.getOrderById(orderToken.getOrderId())

        if (orderResponse is ResultWrapper.Success)
            order = OrderDvo(
                orderResponse.value.externalId,
                orderResponse.value.amount
            )
    }

    override fun doAfterLoading(): ViewAction {
        return if (order == null) {
            ViewAction {
                it.showErrorToast(it.getString(R.string.ioka_common_server_error))
                it.finish()
            }
        } else {
            progressFlow.value = false

            ViewAction {
                val newFragment: CvvFragment = CvvFragment.newInstance(
                    CvvLauncher(
                        customerToken,
                        orderToken,
                        order!!,
                        cardId,
                        cardNumber,
                        cardType,
                    )
                )
                newFragment.show(
                    it.supportFragmentManager,
                    newFragment::class.simpleName
                )
            }
        }
    }
}