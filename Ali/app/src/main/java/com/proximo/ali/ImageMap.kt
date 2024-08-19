package com.proximo.ali

import com.proximo.ali.R.drawable

object ImageMap {
    private val imageMap = mapOf(
        "25am" to drawable.vinteecincoam,
        "30am" to drawable.trintaam,
        "1030p" to drawable.miletrintap,
        "1030pduo" to drawable.miletrintapduo,
        "ecolift50" to drawable.ecoliftcinquenta,
        "ecolift70" to drawable.ecolift70,
        "es1330l" to drawable.es1330l,
        "liftpodft140" to drawable.liftpodft140,
        "opt0507" to drawable.opt0507,
        "toucant26e" to drawable.toucant26e
    )

    fun getDrawableResId(modelName: String): Int {
        return imageMap[modelName] ?: drawable.default_machine_image
    }
}
