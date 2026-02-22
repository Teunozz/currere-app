package nl.teunk.currere

import android.app.Application
import androidx.health.connect.client.HealthConnectClient
import nl.teunk.currere.data.health.HealthConnectSource

class CurrereApp : Application() {

    val healthConnectSource: HealthConnectSource by lazy {
        HealthConnectSource(HealthConnectClient.getOrCreate(this))
    }
}
