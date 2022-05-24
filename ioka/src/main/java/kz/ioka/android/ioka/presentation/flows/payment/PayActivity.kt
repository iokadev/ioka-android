package kz.ioka.android.ioka.presentation.flows.payment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.*
import kotlinx.coroutines.flow.launchIn
import kz.ioka.android.ioka.R
import kz.ioka.android.ioka.di.DependencyInjector
import kz.ioka.android.ioka.domain.cardInfo.CardInfoRepositoryImpl
import kz.ioka.android.ioka.domain.payment.PaymentRepositoryImpl
import kz.ioka.android.ioka.googlePay.GooglePayUtil
import kz.ioka.android.ioka.presentation.flows.common.CardInfoViewModel
import kz.ioka.android.ioka.presentation.flows.common.CardInfoViewModelFactory
import kz.ioka.android.ioka.presentation.flows.common.PaymentState
import kz.ioka.android.ioka.presentation.result.ErrorResultLauncher
import kz.ioka.android.ioka.presentation.result.ResultActivity
import kz.ioka.android.ioka.presentation.result.SuccessResultLauncher
import kz.ioka.android.ioka.presentation.webView.PaymentConfirmationBehavior
import kz.ioka.android.ioka.uikit.ButtonState
import kz.ioka.android.ioka.uikit.CardNumberEditText
import kz.ioka.android.ioka.uikit.CvvEditText
import kz.ioka.android.ioka.uikit.IokaStateButton
import kz.ioka.android.ioka.util.showErrorToast
import kz.ioka.android.ioka.util.toAmountFormat
import kz.ioka.android.ioka.viewBase.BaseActivity
import kz.ioka.android.ioka.viewBase.Scannable
import kz.ioka.android.ioka.viewBase.ThreeDSecurable
import org.json.JSONException
import org.json.JSONObject

internal class PayActivity : BaseActivity(), Scannable, ThreeDSecurable {

    companion object {
        const val LOAD_PAYMENT_DATA_REQUEST_CODE = 123344
    }

    private val cardInfoViewModel: CardInfoViewModel by viewModels {
        CardInfoViewModelFactory(
            CardInfoRepositoryImpl(DependencyInjector.cardInfoApi)
        )
    }

    private val viewModel: PayWithCardViewModel by viewModels {
        PayWithCardViewModelFactory(
            launcher()!!,
            PaymentRepositoryImpl(DependencyInjector.paymentApi)
        )
    }

    private lateinit var vRoot: ConstraintLayout
    private lateinit var vToolbar: Toolbar
    private lateinit var groupGooglePay: Group
    private lateinit var btnGooglePay: AppCompatImageButton
    private lateinit var etCardNumber: CardNumberEditText
    private lateinit var etExpireDate: AppCompatEditText
    private lateinit var vCvvInput: CvvEditText
    private lateinit var switchSaveCard: SwitchCompat
    private lateinit var btnPay: IokaStateButton

    private lateinit var paymentsClient: PaymentsClient

    override val activityResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(), activityResultCallback()
    )

    override fun onCardScanned(cardNumber: String) {
        etCardNumber.setCardNumber(cardNumber)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ioka_activity_pay)

        paymentsClient = GooglePayUtil.createPaymentsClient(this)

        bindViews()
        setConfiguration()
        setupListeners()
        observeData()
    }

    private fun bindViews() {
        vRoot = findViewById(R.id.vRoot)
        vToolbar = findViewById(R.id.vToolbar)
        groupGooglePay = findViewById(R.id.groupGooglePay)
        btnGooglePay = findViewById(R.id.btnGooglePay)
        etCardNumber = findViewById(R.id.vCardNumberInput)
        etExpireDate = findViewById(R.id.etExpireDate)
        vCvvInput = findViewById(R.id.vCvvInput)
        switchSaveCard = findViewById(R.id.vSaveCardSwitch)
        btnPay = findViewById(R.id.btnPay)
    }

    private fun setConfiguration() {
        launcher<PayLauncher>()?.configuration?.apply {
            vRoot.setBackgroundColor(
                ContextCompat.getColor(this@PayActivity, backgroundColor)
            )

            etCardNumber.setIconColor(iconColor)
            vCvvInput.setIconColor(iconColor)

            buttonText?.let { btnPay.setText(buttonText) }

            fieldBackground?.let {
                etCardNumber.background = ContextCompat.getDrawable(this@PayActivity, it)
                etExpireDate.background = ContextCompat.getDrawable(this@PayActivity, it)
                vCvvInput.background = ContextCompat.getDrawable(this@PayActivity, it)
            }
            buttonBackground?.let {
                btnPay.background = ContextCompat.getDrawable(this@PayActivity, it)
            }
        }

        if (launcher<PayLauncher>()?.googlePayConfiguration != null) {
            possiblyShowGooglePayButton()
        } else {
            groupGooglePay.isVisible = false
        }
    }

    private fun possiblyShowGooglePayButton() {
        val isReadyToPayJson = GooglePayUtil.isReadyToPayRequest() ?: return
        val request = IsReadyToPayRequest.fromJson(isReadyToPayJson.toString())

        val task = paymentsClient.isReadyToPay(request)
        task.addOnCompleteListener { completedTask ->
            try {
                completedTask.getResult(ApiException::class.java)?.let {
                    groupGooglePay.isVisible = it
                }
            } catch (exception: ApiException) {
                Log.w("isReadyToPay failed", exception)
            }
        }
    }

    private fun requestPayment() {
        btnGooglePay.isClickable = false

        val paymentDataRequestJson =
            GooglePayUtil.getPaymentDataRequest(
                launcher<PayLauncher>()?.order?.amount?.amount.toString(),
                launcher<PayLauncher>()?.order?.amount?.currency?.code!!,
                launcher<PayLauncher>()?.googlePayConfiguration?.merchantIdentifier!!
            )

        if (paymentDataRequestJson == null) {
            Log.e("RequestPayment", "Can't fetch payment data request")
            return
        }
        val request = PaymentDataRequest.fromJson(paymentDataRequestJson.toString())

        AutoResolveHelper.resolveTask(
            paymentsClient.loadPaymentData(request), this, LOAD_PAYMENT_DATA_REQUEST_CODE
        )
    }

    private fun setupListeners() {
        vToolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        btnGooglePay.setOnClickListener {
            requestPayment()
        }

        etCardNumber.onTextChangedWithDebounce = {
            cardInfoViewModel.onCardPanEntered(it)
        }
        etCardNumber.flowTextChangedWithDebounce.launchIn(lifecycleScope)

        etCardNumber.onTextChanged = {
            viewModel.onCardPanEntered(it)
        }

        etCardNumber.onScanClicked = {
            startCardScanner(this)
        }

        etExpireDate.doOnTextChanged { text, _, _, _ ->
            viewModel.onExpireDateEntered(text.toString().replace("/", ""))
        }

        vCvvInput.onTextChanged = {
            viewModel.onCvvEntered(it)
        }

        btnPay.setOnClickListener {
            viewModel.onPayClicked(
                etCardNumber.getCardNumber(),
                etExpireDate.text.toString(),
                vCvvInput.getCvv(),
                switchSaveCard.isChecked
            )
        }
    }

    private fun observeData() {
        viewModel.apply {
            vToolbar.title =
                getString(R.string.ioka_payment_toolbar, order.amount.amount.toAmountFormat())
            btnPay.setText(
                getString(
                    R.string.ioka_payment_button,
                    order.amount.amount.toAmountFormat()
                )
            )
            groupGooglePay.isVisible = withGooglePay
            switchSaveCard.isVisible = canSaveCard

            payState.observe(this@PayActivity) {
                handleState(it)
            }
        }
    }

    private fun handleState(state: PaymentState) {
        when (state) {
            PaymentState.LOADING -> {
                btnPay.setState(ButtonState.Loading)
                disableInputs()
            }

            PaymentState.DISABLED -> {
                btnPay.setState(ButtonState.Disabled)
            }

            is PaymentState.PENDING -> {
                btnPay.setState(ButtonState.Default)

                onActionRequired(
                    this,
                    PaymentConfirmationBehavior(
                        url = state.actionUrl,
                        orderToken = viewModel.orderToken,
                        paymentId = viewModel.paymentId
                    )
                )
            }

            PaymentState.SUCCESS -> {
                btnPay.setState(ButtonState.Default)

                onSuccessfulAttempt()
            }

            is PaymentState.ERROR -> {
                btnPay.setState(ButtonState.Default)

                showErrorToast(state.cause ?: getString(R.string.ioka_common_server_error))
            }

            is PaymentState.FAILED -> {
                btnPay.setState(ButtonState.Default)

                onFailedAttempt(
                    state.cause ?: getString(R.string.ioka_result_failed_payment_common_cause)
                )
            }

            PaymentState.DEFAULT -> {
                btnPay.setState(ButtonState.Default)
                enableInputs()
            }
        }
    }

    private fun enableInputs() {
        etCardNumber.isEnabled = true
        vCvvInput.isEnabled = true
        etExpireDate.isEnabled = true
        switchSaveCard.isEnabled = true
        btnGooglePay.isEnabled = true
    }

    private fun disableInputs() {
        etCardNumber.isEnabled = false
        vCvvInput.isEnabled = false
        etExpireDate.isEnabled = false
        switchSaveCard.isEnabled = false
        btnGooglePay.isEnabled = false
    }

    override fun onSuccessfulAttempt() {
        val intent = ResultActivity.provideIntent(
            this, SuccessResultLauncher(
                subtitle = getString(
                    R.string.ioka_result_success_payment_subtitle,
                    viewModel.order.externalId
                ),
                amount = viewModel.order.amount
            )
        )

        startActivity(intent)
        finish()
    }

    override fun onFailedAttempt(cause: String?) {
        val intent = ResultActivity.provideIntent(
            this, ErrorResultLauncher(
                subtitle = cause ?: getString(R.string.ioka_result_failed_payment_common_cause)
            )
        )

        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        super.onBackPressed()
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super<BaseActivity>.onActivityResult(requestCode, resultCode, data)
        super<Scannable>.onActivityResult(requestCode, resultCode, data)

        if (requestCode == LOAD_PAYMENT_DATA_REQUEST_CODE) {
            processGooglePayResult(resultCode, data)
            btnGooglePay.isClickable = true
        }
    }

    private fun processGooglePayResult(resultCode: Int, data: Intent?) {
        when (resultCode) {
            RESULT_OK ->
                data?.let { intent ->
                    PaymentData.getFromIntent(intent)?.let(::handlePaymentSuccess)
                }

            AutoResolveHelper.RESULT_ERROR -> {
                AutoResolveHelper.getStatusFromIntent(data)?.let {

                }
            }
        }
    }

    private fun handlePaymentSuccess(paymentData: PaymentData) {
        val paymentInformation = paymentData.toJson() ?: return

        try {
            // Token will be null if PaymentDataRequest was not constructed using fromJson(String).
            val paymentMethodData =
                JSONObject(paymentInformation).getJSONObject("paymentMethodData")
            val billingName = paymentMethodData.getJSONObject("info")
                .getJSONObject("billingAddress").getString("name")
            Log.d("BillingName", billingName)

            // Logging token string.
            Log.d(
                "GooglePaymentToken", paymentMethodData
                    .getJSONObject("tokenizationData")
                    .getString("token")
            )

        } catch (e: JSONException) {
            Log.e("handlePaymentSuccess", "Error: $e")
        }

    }

}