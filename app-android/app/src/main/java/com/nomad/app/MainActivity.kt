package com.nomad.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.nomad.app.location.LocationManager
import com.nomad.app.ui.theme.NomadTheme

class MainActivity : ComponentActivity() {

    private lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationManager = LocationManager(this)

        enableEdgeToEdge()
        setContent {
            NomadTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NomadApp(
                        locationManager = locationManager,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
