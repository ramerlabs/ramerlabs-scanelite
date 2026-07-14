package com.ramerlabs.scanelite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ramerlabs.scanelite.ui.camera.CameraScreen
import com.ramerlabs.scanelite.ui.crop.CropScreen
import com.ramerlabs.scanelite.ui.editor.PageEditorScreen
import com.ramerlabs.scanelite.ui.home.HomeScreen
import com.ramerlabs.scanelite.ui.license.LicenseGateScreen
import com.ramerlabs.scanelite.ui.navigation.Routes
import com.ramerlabs.scanelite.ui.review.ReviewScreen
import com.ramerlabs.scanelite.ui.session.SessionViewModel
import com.ramerlabs.scanelite.ui.settings.SettingsScreen
import com.ramerlabs.scanelite.ui.share.ShareHubScreen
import com.ramerlabs.scanelite.ui.splash.SplashScreen
import com.ramerlabs.scanelite.ui.theme.ScanEliteTheme
import com.ramerlabs.scanelite.ui.theme.SeBgPrimary
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScanEliteTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = SeBgPrimary) {
                    LicenseGateScreen {
                        ScanEliteNav()
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanEliteNav() {
    val navController = rememberNavController()
    val activity = LocalContext.current as ComponentActivity
    val sessionViewModel: SessionViewModel = hiltViewModel(activity)

    NavHost(navController = navController, startDestination = Routes.Splash) {
        composable(Routes.Splash) {
            SplashScreen {
                navController.navigate(Routes.Home) {
                    popUpTo(Routes.Splash) { inclusive = true }
                }
            }
        }
        composable(Routes.Home) {
            HomeScreen(
                onNewScan = {
                    sessionViewModel.resetSession()
                    navController.navigate(Routes.Camera)
                },
                onSettings = { navController.navigate(Routes.Settings) }
            )
        }
        composable(Routes.Camera) {
            CameraScreen(
                sessionViewModel = sessionViewModel,
                onClose = { navController.popBackStack() },
                onCapturedContinue = {
                    navController.navigate(Routes.Editor) {
                        launchSingleTop = true
                    }
                },
                onBatchDone = {
                    navController.navigate(Routes.Review)
                }
            )
        }
        composable(Routes.Editor) {
            PageEditorScreen(
                sessionViewModel = sessionViewModel,
                onContinue = { navController.navigate(Routes.Review) },
                onAddNext = {
                    navController.navigate(Routes.Camera) {
                        popUpTo(Routes.Camera) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Routes.Review) {
            ReviewScreen(
                sessionViewModel = sessionViewModel,
                onShare = {
                    sessionViewModel.ensureAutoName()
                    sessionViewModel.setCropPageIndex(0)
                    navController.navigate(Routes.Crop)
                },
                onSaveOnly = {
                    sessionViewModel.ensureAutoName()
                    sessionViewModel.resetSession()
                    navController.navigate(Routes.Home) {
                        popUpTo(Routes.Home) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.Crop) {
            CropScreen(
                sessionViewModel = sessionViewModel,
                onFinished = { navController.navigate(Routes.Share) }
            )
        }
        composable(Routes.Share) {
            ShareHubScreen(
                sessionViewModel = sessionViewModel,
                onDone = {
                    navController.navigate(Routes.Home) {
                        popUpTo(Routes.Home) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.Settings) {
            SettingsScreen(
                sessionViewModel = sessionViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
