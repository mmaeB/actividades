package com.example.maps__gps

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.maps__gps.ui.theme.Maps__gpsTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Maps__gpsTheme {
                MainScreen()
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Cliente para obtener la ubicación real
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    var showMap by remember { mutableStateOf(false) }
    var locationType by remember { mutableStateOf("plaza") } // "plaza" o "real"
    
    // Coordenadas de la Plaza de Armas de CAJAMARCA
    val plazaCajamarca = LatLng(-7.1570173, -78.5174515)
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(plazaCajamarca, 15f)
    }

    // Estado para los permisos de ubicación
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Función para mover la cámara a la ubicación actual detectada por el GPS
    val moverAMiUbicacion = {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                scope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 16f)
                    )
                }
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        hasLocationPermission = granted
        if (granted && locationType == "real") {
            moverAMiUbicacion()
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (!showMap) {
                // Pantalla de inicio con los 2 botones
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "GPS Cajamarca",
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(bottom = 40.dp)
                    )
                    
                    Button(
                        onClick = {
                            locationType = "plaza"
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(plazaCajamarca, 17f)
                            showMap = true
                        },
                        modifier = Modifier.fillMaxWidth(0.8f).height(56.dp)
                    ) {
                        Text("Ver Plaza de Armas (Cajamarca)")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            locationType = "real"
                            launcher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                            // Si ya tiene permisos, intentamos mover la cámara de inmediato
                            moverAMiUbicacion()
                            showMap = true
                        },
                        modifier = Modifier.fillMaxWidth(0.8f).height(56.dp)
                    ) {
                        Text("Mi ubicación en tiempo real")
                    }
                }
            } else {
                // Pantalla del Mapa
                Box(modifier = Modifier.fillMaxSize()) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            // Activa el punto azul de ubicación
                            isMyLocationEnabled = hasLocationPermission
                        ),
                        uiSettings = MapUiSettings(
                            myLocationButtonEnabled = true,
                            zoomControlsEnabled = true
                        )
                    ) {
                        if (locationType == "plaza") {
                            Marker(
                                state = rememberMarkerState(position = plazaCajamarca),
                                title = "Plaza de Armas de Cajamarca",
                                snippet = "Corazón de la Ciudad del Cumbe"
                            )
                        }
                    }
                    
                    // Botón para volver al menú de botones
                    FilledTonalButton(
                        onClick = { showMap = false },
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Text("Volver")
                    }
                }
            }
        }
    }
}
