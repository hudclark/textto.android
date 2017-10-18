package com.moduloapps.textto.onboarding

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

/**
 * Created by hudson on 7/15/17.
 */
class SquareLinearLayout : LinearLayout {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }

}