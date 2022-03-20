package kz.ioka.android.ioka.domain.bindCard

import kotlinx.coroutines.Dispatchers
import kz.ioka.android.ioka.data.card.BindCardRequestDto
import kz.ioka.android.ioka.data.card.CardApi
import kz.ioka.android.ioka.domain.common.ResultWrapper
import kz.ioka.android.ioka.domain.common.safeApiCall
import kz.ioka.android.ioka.util.getCustomerId

interface CardRepository {

    suspend fun bindCard(
        customerToken: String,
        apiKey: String,
        cardPan: String,
        expireDate: String,
        cvv: String
    ): ResultWrapper<CardBindingResultModel>

}

class CardRepositoryImpl constructor(
    private val cardApi: CardApi
) : CardRepository {

    override suspend fun bindCard(
        customerToken: String,
        apiKey: String,
        cardPan: String,
        expireDate: String,
        cvv: String
    ): ResultWrapper<CardBindingResultModel> {
        return safeApiCall(Dispatchers.IO) {
            val bindCardResult = cardApi.bindCard(
                customerToken.getCustomerId(),
                customerToken,
                apiKey,
                BindCardRequestDto(cardPan, expireDate, cvv)
            )

            when (bindCardResult.status) {
                CardBindingResultModel.STATUS_APPROVED -> CardBindingResultModel.Success
                CardBindingResultModel.STATUS_DECLINED -> CardBindingResultModel.Declined(
                    bindCardResult.error.message
                )
                else -> CardBindingResultModel.Pending(bindCardResult.action.url)
            }
        }
    }

}