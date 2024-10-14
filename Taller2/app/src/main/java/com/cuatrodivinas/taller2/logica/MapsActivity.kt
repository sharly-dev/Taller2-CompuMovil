package com.cuatrodivinas.taller2.logica

import android.Manifest
import android.content.pm.PackageManager
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
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.bonuspack.location.GeocoderNominatim
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import java.util.*
private lateinit var lightSensorListener: SensorEventListener


class MapsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationOverlay: MyLocationNewOverlay
    private var searchMarker: Marker? = null
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private lateinit var lightSensorListener: SensorEventListener
    private var isMapDark = false
    private var isStraightDistanceToastShown = false
    private var isRouteDistanceToastShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupMapActivity()
        checkLocationPermission()
        setupMapEventListeners()
        setupSearchButton()
        setupLightSensor()
    }

    // Configuración inicial de la actividad y mapa
    private fun setupMapActivity() {
        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE))
    }

    // Verificación de permisos de ubicación
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            initializeMap()
        }
    }

    // Inicialización del mapa
    private fun initializeMap() {
        val mapView = binding.map
        mapView.setMultiTouchControls(true)
        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mapView)
        locationOverlay.enableMyLocation()
        locationOverlay.enableFollowLocation()
        mapView.overlays.add(locationOverlay)
        locationOverlay.runOnFirstFix { centerMapOnMyLocation() }
    }

    // Centrar mapa en la ubicación del usuario
    private fun centerMapOnMyLocation() {
        runOnUiThread {
            val myLocation: GeoPoint? = locationOverlay.myLocation
            if (myLocation != null) {
                binding.map.controller.setZoom(20)
                binding.map.controller.setCenter(myLocation)
            }
        }
    }

    // Configurar eventos del mapa (long click)
    private fun setupMapEventListeners() {
        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?) = false
            override fun longPressHelper(p: GeoPoint?): Boolean {
                p?.let {
                    resetToastFlags()
                    getAddressAndPlaceMarker(it)
                    calculateAndShowDistance(it)
                    createRouteToMarker(it)
                }
                return true
            }
        }
        val eventsOverlay = org.osmdroid.views.overlay.MapEventsOverlay(mapEventsReceiver)
        binding.map.overlays.add(eventsOverlay)
    }

    // Configuración del botón de búsqueda
    private fun setupSearchButton() {
        binding.searchButton.setOnClickListener {
            val addressText = binding.searchInput.text.toString().trim()
            if (addressText.isNotEmpty()) {
                searchLocationByAddress(addressText)
            } else {
                Toast.makeText(this, "Ingrese una dirección", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Configuración del sensor de luz
    private fun setupLightSensor() {
        lightSensorListener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
            override fun onSensorChanged(event: SensorEvent) {
                val lightIntensity = event.values[0]
                if (lightIntensity < 10000 && !isMapDark) {
                    setDarkMapTheme()
                } else if (lightIntensity >= 10000 && isMapDark) {
                    setLightMapTheme()
                }
            }
        }
        sensorManager.registerListener(lightSensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun setDarkMapTheme() {
        val darkTileSource = XYTileSource("DarkTile", 0, 19, 256, ".png", arrayOf("https://basemaps.cartocdn.com/dark_all/"))
        binding.map.setTileSource(darkTileSource)
        isMapDark = true
    }

    private fun setLightMapTheme() {
        binding.map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        isMapDark = false
    }

    // Resetear los flags de Toast
    private fun resetToastFlags() {
        isStraightDistanceToastShown = false
        isRouteDistanceToastShown = false
    }

    private fun zoomToFitRoute(roadOverlay: Polyline) {
        val boundingBox = org.osmdroid.util.BoundingBox.fromGeoPoints(roadOverlay.points)
        // Establecer el padding para dejar espacio debajo del TextBox (en píxeles)
        val topPadding = 300  // Ajusta este valor según el tamaño de tu TextBox
        // Ajusta el zoom al bounding box pero luego disminuye el nivel de zoom en 1
        binding.map.zoomToBoundingBox(boundingBox, true, topPadding) // Ajustar zoom para incluir todos los puntos
    }

    private fun searchLocationByAddress(addressText: String) {
        val geocoderNominatim = GeocoderNominatim(Locale.getDefault(), "user-agent-example")
        isStraightDistanceToastShown = false
        isRouteDistanceToastShown = false

        geocoderNominatim.setService("https://nominatim.openstreetmap.org/")

        Thread {
            try {
                val addresses = geocoderNominatim.getFromLocationName(addressText, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0]
                    val geoPoint = GeoPoint(address.latitude, address.longitude)

                    runOnUiThread {
                        binding.map.controller.setCenter(geoPoint)

                        if (searchMarker == null) {
                            searchMarker = Marker(binding.map)
                            searchMarker!!.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            binding.map.overlays.add(searchMarker)
                        }

                        searchMarker!!.position = geoPoint
                        searchMarker!!.title = address.getAddressLine(0) ?: "Dirección no disponible"
                        searchMarker!!.setIcon(ResourcesCompat.getDrawable(resources, R.drawable.ic_location, null))

                        binding.map.invalidate()

                        // Calcular la distancia y crear la ruta
                        calculateAndShowDistance(geoPoint)

                        // Crear la ruta al marcador y ajustar el zoom para que toda la ruta sea visible
                        createRouteToMarker(geoPoint)

                        // Desactivar el seguimiento de la ubicación después de ajustar el zoom
                        binding.map.postDelayed({
                            locationOverlay.disableFollowLocation()
                        }, 2000)
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
                        // Ocultar la ventana de información después de 3 segundos (3000 milisegundos)
                        binding.map.postDelayed({
                            searchMarker!!.closeInfoWindow()
                        }, 4000)
                        // Mover la cámara a la ubicación del marcador
                        binding.map.controller.setZoom(20)
                        binding.map.controller.setCenter(geoPoint)
                        binding.map.invalidate() // Refrescar el mapa
                        // Calcular la distancia y mostrarla
                        calculateAndShowDistance(geoPoint)
                        // Crear la ruta al marcador
                        createRouteToMarker(geoPoint)
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

    // Función para calcular la distancia entre la ubicación actual y el marcador
    private fun calculateAndShowDistance(markerLocation: GeoPoint) {
        if (!isStraightDistanceToastShown) {  // Mostrar el Toast solo si no se ha mostrado aún
            val myLocation: GeoPoint? = locationOverlay.myLocation
            if (myLocation != null) {
                val distance = myLocation.distanceToAsDouble(markerLocation)
                val distanceKm = String.format(Locale.getDefault(), "%.2f", distance / 1000)
                Toast.makeText(this, "Distancia recta al marcador: $distanceKm km", Toast.LENGTH_LONG).show()
                isStraightDistanceToastShown = true // Marcamos que ya se mostró
            } else {
                Toast.makeText(this, "Ubicación actual no disponible", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createRouteToMarker(markerLocation: GeoPoint) {
        val roadManager = OSRMRoadManager(this)
        roadManager.setService("https://router.project-osrm.org/route/v1/driving/")

        val waypoints = ArrayList<GeoPoint>()
        val myLocation: GeoPoint? = locationOverlay.myLocation
        if (myLocation != null) {
            waypoints.add(myLocation)
            waypoints.add(markerLocation)

            // Obtener la ruta en un hilo separado
            Thread {
                val road: Road = roadManager.getRoad(waypoints)
                runOnUiThread {
                    if (road.mStatus == Road.STATUS_OK) {
                        binding.map.overlays.removeIf { it is Polyline }

                        // Crear Polyline a partir de la ruta y añadirla al mapa
                        val roadOverlay = Polyline()
                        roadOverlay.setPoints(road.mRouteHigh)
                        roadOverlay.color = ContextCompat.getColor(this, R.color.primaryColor)
                        roadOverlay.width = 10f  // Ajustar el grosor de la ruta
                        binding.map.overlays.add(roadOverlay)

                        // Ajustar el zoom para mostrar toda la ruta
                        zoomToFitRoute(roadOverlay)

                        // Después de ajustar el zoom, desactivar el seguimiento de la ubicación
                        binding.map.postDelayed({
                            locationOverlay.disableFollowLocation()
                        }, 2000) // Dar un pequeño retraso para asegurar que el zoom se ajusta antes

                        // Calcular y mostrar la distancia real según la ruta
                        if (!isRouteDistanceToastShown) {
                            val distanceKm = String.format(Locale.getDefault(), "%.2f", road.mLength)
                            Toast.makeText(this, "Distancia según la ruta: $distanceKm km", Toast.LENGTH_LONG).show()
                            isRouteDistanceToastShown = true
                        }

                        binding.map.invalidate() // Refrescar el mapa
                    } else {
                        Toast.makeText(this, "Error al obtener la ruta", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        } else {
            Toast.makeText(this, "Ubicación actual no disponible", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.map.onPause()
        sensorManager.unregisterListener(lightSensorListener)
    }
}