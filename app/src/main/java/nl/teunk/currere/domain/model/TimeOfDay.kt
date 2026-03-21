package nl.teunk.currere.domain.model

import androidx.annotation.StringRes
import nl.teunk.currere.R

enum class TimeOfDay(@param:StringRes val labelResId: Int) {
    MORNING(R.string.morning_run),
    AFTERNOON(R.string.afternoon_run),
    EVENING(R.string.evening_run),
    NIGHT(R.string.night_run),
}
