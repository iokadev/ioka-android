package kz.ioka.android.ioka.util

import androidx.appcompat.app.AppCompatActivity

open class ViewAction(
    open var singleAction: (AppCompatActivity) -> Unit
) {

    open fun invoke(activity: AppCompatActivity) {
        singleAction.invoke(activity)
    }

}