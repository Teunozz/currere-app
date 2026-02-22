package nl.teunk.currere

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import nl.teunk.currere.ui.navigation.CurrereNavGraph
import nl.teunk.currere.ui.theme.CurrereTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CurrereTheme {
                CurrereNavGraph()
            }
        }
    }
}
