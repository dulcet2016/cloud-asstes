package com.manha.eventassettracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.manha.eventassettracker.ui.AppNavHost
import com.manha.eventassettracker.ui.theme.EventAssetTrackerTheme
import com.manha.eventassettracker.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EventAssetTrackerApp(viewModel)
        }
    }
}

@Composable
private fun EventAssetTrackerApp(viewModel: AppViewModel) {
    EventAssetTrackerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppNavHost(viewModel = viewModel)
        }
    }
}
