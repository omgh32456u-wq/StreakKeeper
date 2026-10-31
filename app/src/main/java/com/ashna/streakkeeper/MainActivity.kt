package com.ashna.streakkeeper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ashna.streakkeeper.ui.AppRoot
import com.ashna.streakkeeper.ui.StreakKeeperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StreakKeeperTheme {
                val context = LocalContext.current
                val permissionLauncher =
                    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(
                            context, Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

                val vm: StreakViewModel = viewModel(factory = viewModelFactory {
                    initializer {
                        StreakViewModel((this[APPLICATION_KEY] as StreakApp).repository)
                    }
                })

                LaunchedEffect(Unit) { vm.refreshStreaks() }

                AppRoot(vm)
            }
        }
    }
}
