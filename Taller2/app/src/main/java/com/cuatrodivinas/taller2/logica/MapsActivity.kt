package com.cuatrodivinas.taller2.logica

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.cuatrodivinas.taller2.R
import com.cuatrodivinas.taller2.databinding.ActivityMapsBinding
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.bonuspack.location.GeocoderNominatim
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import java.util.*

class MapsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationOverlay: MyLocationNewOverlay
    private var searchMarker: Marker? = null
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private lateinit var lightSensorListener: SensorEventListener
    private var isMapDark = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Habilitar política de red en el hilo principal
        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        // Configurar ViewBinding
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar osmdroid
        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE))

        // Comprobar permisos de ubicación
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            initializeMap()
        }

        // Configurar evento LongClick para colocar marcador
        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                return false
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                if (p != null) {
                    // Al hacer LongClick, obtener la dirección y colocar el marcador
                    getAddressAndPlaceMarker(p)
                }
                return true
            }
        }

        // Añadir el overlay de eventos al mapa para manejar los toques largos
        val eventsOverlay = org.osmdroid.views.overlay.MapEventsOverlay(mapEventsReceiver)
        binding.map.overlays.add(eventsOverlay)

        // Configuración de la búsqueda de la dirección
        binding.searchButton.setOnClickListener {
            val addressText = binding.searchInput.text.toString().trim()
            if (addressText.isNotEmpty()) {
                searchLocationByAddress(addressText)
            } else {
                Toast.makeText(this, "Ingrese una dirección", Toast.LENGTH_SHORT).show()
            }
        }

        // Configurar sensor de luz (parte del mapa oscuro)
        lightSensorListener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_LIGHT) {
                    val lightIntensity = event.values[0]
                    if (lightIntensity < 10000 && !isMapDark) {
                        val darkTileSource = XYTileSource(
                            "DarkTile",
                            0, 19, 256, ".png",
                            arrayOf("https://basemaps.cartocdn.com/dark_all/")
                        )
                        binding.map.setTileSource(darkTileSource)
                        isMapDark = true
                    } else if (lightIntensity >= 10000 && isMapDark) {
                        binding.map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
                        isMapDark = false
                    }
                }
            }
        }
        sensorManager.registerListener(lightSensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)

        // Configurar la búsqueda cuando se presiona el botón de búsqueda
        val searchInput = binding.searchInput
        val searchButton = binding.searchButton
        searchButton.setOnClickListener {
            val address = searchInput.text.toString()
            if (address.isNotEmpty()) {
                searchLocationByAddress(address)
            }
        }
    }

    private fun initializeMap() {
        val mapView = binding.map
        mapView.setMultiTouchControls(true)

        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mapView)
        locationOverlay.enableMyLocation()
        locationOverlay.enableFollowLocation()
        mapView.overlays.add(locationOverlay)

        locationOverlay.runOnFirstFix {
            runOnUiThread {
                val myLocation: GeoPoint? = locationOverlay.myLocation
                if (myLocation != null) {
                    mapView.controller.setZoom(20)
                    mapView.controller.setCenter(myLocation)
                }
            }
        }
    }

    // Función para buscar la ubicación a partir de una dirección ingresada
    private fun searchLocationByAddress(addressText: String) {
        val geocoderNominatim = GeocoderNominatim(Locale.getDefault(), "user-agent-example")

        // Configura la URL base para usar HTTPS en lugar de HTTP
        geocoderNominatim.setService("https://nominatim.openstreetmap.org/")

        // Realizamos la búsqueda en un hilo separado para evitar bloquear el hilo principal
        Thread {
            try {
                val addresses = geocoderNominatim.getFromLocationName(addressText, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0]
                    val geoPoint = GeoPoint(address.latitude, address.longitude)

                    runOnUiThread {
                        binding.map.controller.setZoom(20)
                        binding.map.controller.setCenter(geoPoint)

                        // Si ya hay un marcador de búsqueda, actualizarlo
                        if (searchMarker == null) {
                            searchMarker = Marker(binding.map)
                            searchMarker!!.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            binding.map.overlays.add(searchMarker)
                        }

                        searchMarker!!.position = geoPoint
                        searchMarker!!.title = address.getAddressLine(0) ?: "Dirección no disponible"
                        searchMarker!!.setIcon(ResourcesCompat.getDrawable(resources, R.drawable.ic_location, null))

                        binding.map.invalidate() // Refrescar el mapa
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "No se encontró la dirección", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Error al buscar la dirección", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    // Función para obtener la dirección con GeocoderNominatim y colocar el marcador en un LongClick
    private fun getAddressAndPlaceMarker(geoPoint: GeoPoint) {
        val geocoderNominatim = GeocoderNominatim(Locale.getDefault(), "user-agent-example")

        // Forzar el uso de HTTPS
        geocoderNominatim.setService("https://nominatim.openstreetmap.org/")

        // Realizamos la geocodificación inversa en un hilo separado para no bloquear la UI
        Thread {
            try {
                val addresses = geocoderNominatim.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0]
                    val addressText = address.getAddressLine(0) ?: "Dirección no disponible"

                    runOnUiThread {
                        // Colocar el marcador en la ubicación del LongClick con la dirección obtenida
                        if (searchMarker == null) {
                            searchMarker = Marker(binding.map)
                            searchMarker!!.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            binding.map.overlays.add(searchMarker)
                        }

                        searchMarker!!.position = geoPoint
                        searchMarker!!.title = addressText
                        searchMarker!!.setIcon(ResourcesCompat.getDrawable(resources, R.drawable.ic_location, null))

                        // Muestra la ventana de información del marcador automáticamente
                        searchMarker!!.showInfoWindow()

                        // Mover la cámara a la ubicación del marcador
                        binding.map.controller.setZoom(20)
                        binding.map.controller.setCenter(geoPoint)

                        binding.map.invalidate() // Refrescar el mapa
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "No se encontró dirección", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Error obteniendo la dirección", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()


    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.map.onPause()
        sensorManager.unregisterListener(lightSensorListener)  // Opcional para liberar el sensor
    }
}
