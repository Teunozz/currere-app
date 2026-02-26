package nl.teunk.currere.ui.permission

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import nl.teunk.currere.ui.theme.CurrereTheme
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord

val HEALTH_PERMISSIONS = setOf(
    HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    HealthPermission.getReadPermission(DistanceRecord::class),
    HealthPermission.getReadPermission(StepsRecord::class),
    HealthPermission.getReadPermission(HeartRateRecord::class),
    HealthPermission.getReadPermission(SpeedRecord::class),
    HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY,
)

@Composable
fun PermissionScreen(
    onPermissionsGranted: () -> Unit,
) {
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(HEALTH_PERMISSIONS)) {
            onPermissionsGranted()
        } else {
            permissionDenied = true
        }
    }

    if (!permissionDenied) {
        LaunchedEffect(Unit) {
            permissionLauncher.launch(HEALTH_PERMISSIONS)
        }
    }

    PermissionScreenContent(
        permissionDenied = permissionDenied,
        onOpenSettings = {
            val intent = Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS")
            context.startActivity(intent)
        },
        onTryAgain = { permissionLauncher.launch(HEALTH_PERMISSIONS) },
    )
}

@Composable
fun PermissionScreenContent(
    permissionDenied: Boolean,
    onOpenSettings: () -> Unit,
    onTryAgain: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(android.R.drawable.ic_menu_mylocation),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Health Connect permissions required",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Currere needs read access to your exercise sessions, distance, steps, heart rate, and speed data to display your running activity.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(32.dp))

        if (permissionDenied) {
            Button(onClick = onOpenSettings) {
                Text("Open Health Connect settings")
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(onClick = onTryAgain) {
                Text("Try again")
            }
        }
    }
}

@Preview
@Composable
private fun PermissionScreenDeniedPreview() {
    CurrereTheme {
        PermissionScreenContent(
            permissionDenied = true,
            onOpenSettings = {},
            onTryAgain = {},
        )
    }
}
