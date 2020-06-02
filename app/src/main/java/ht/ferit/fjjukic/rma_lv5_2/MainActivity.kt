package ht.ferit.fjjukic.rma_lv5_2

import android.Manifest.permission.*
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.RemoteViews
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var sound: Sound
    private lateinit var map: GoogleMap
    private var permissions: Array<String> = arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, WRITE_EXTERNAL_STORAGE)
    private val permissionRequest = 10
    private lateinit var client: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private lateinit var tvWidth: TextView
    private lateinit var tvLength: TextView
    private lateinit var tvCountry: TextView
    private lateinit var tvCity: TextView
    private lateinit var tvAddress: TextView
    private lateinit var btnScreenshot: Button
    private var imageUri: Uri? = null
    private val imageCaptureCode = 1001
    private var street = ""
    lateinit var notificationManager : NotificationManager
    lateinit var notificationChannel : NotificationChannel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        loadLayout()
        loadSound()
        loadMap()
        setNotificationManager()
    }

    private fun loadLayout() {
        this.tvWidth = findViewById(R.id.tvWidth)
        this.tvLength = findViewById(R.id.tvLength)
        this.tvCountry = findViewById(R.id.tvCountry)
        this.tvCity = findViewById(R.id.tvCity)
        this.tvAddress = findViewById(R.id.tvAddress)
        this.btnScreenshot = findViewById(R.id.btnScreenshot)
        setListener()
    }

    private fun setListener() {
        this.btnScreenshot.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(WRITE_EXTERNAL_STORAGE), permissionRequest)
            }
            else {
                takeImage()
            }
        }
    }

    private fun loadSound() {
        this.sound = Sound()
        this.sound.load(this, R.raw.pin_drop, 1)
        this.sound.load(this, R.raw.pin_pull, 1)
    }

    private fun loadMap(){
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        client = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.mapType = GoogleMap.MAP_TYPE_TERRAIN
        map.uiSettings.isZoomControlsEnabled = true
        setMapLongClick(map)
        setUpMap()
        map.setOnInfoWindowLongClickListener {
            this.sound.play(R.raw.pin_pull)
            it.remove()
        }
    }

    private fun setMapLongClick(map:GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            this.sound.play(R.raw.pin_drop)
            placeMarkerOnMap(latLng)
        }
    }

    private fun setUpMap(){
        for (permission in permissions){
            if(ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, permissions, permissionRequest)
                return
            }
        }
        enableMyLocation()
    }

    private fun placeMarkerOnMap(location: LatLng) {
        val markerOptions = MarkerOptions().position(location)
        val title = getAddress(location)
        markerOptions.title(title)
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.custom_marker))
        val marker = map.addMarker(markerOptions)
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 14f))
        marker.showInfoWindow()
    }

    private fun enableMyLocation() {
        map.isMyLocationEnabled = true
        client.lastLocation.addOnSuccessListener(this) { location ->
            if (location != null) {
                lastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                setTextViews(currentLatLng)

                val snippet = String.format(
                    Locale.getDefault(),
                    "Wow! Konačno znam gdje se nalazim!"
                )
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14f))
                val marker = map.addMarker(MarkerOptions()
                    .position(currentLatLng)
                    .title("Ovdje sam")
                    .snippet(snippet))
                marker.showInfoWindow()
            }
        }
    }

    private fun setTextViews(location: LatLng) {
        val geoCoder = Geocoder(this, Locale.getDefault())
        val addressList: List<Address>? =
            geoCoder.getFromLocation(
                location.latitude, location.longitude, 1
            )
        if (addressList != null && addressList.isNotEmpty()) {
            this.street = addressList[0].getAddressLine(0).split(",")[0]
            this.tvWidth.text = "Geografska širina: ${location.latitude}"
            this.tvLength.text = "Geografska dužina: ${location.longitude}"
            this.tvCountry.text = "Država: ${addressList[0].countryName}"
            this.tvCity.text = "Mjesto: ${addressList[0].locality}"
            this.tvAddress.text = "Adresa: ${this.street}"
        }
    }

    private fun getAddress(latLng: LatLng): String {
        val geoCoder = Geocoder(this)
        val addresses = geoCoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
        return addresses[0].getAddressLine(0)
    }

    private fun takeImage(){
        val values = ContentValues()
        values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, this.street)
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(cameraIntent, imageCaptureCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        sendNotification()
    }

    private fun sendNotification() {
        val intent = Intent(Intent.ACTION_VIEW, imageUri)
        val pendingIntent = PendingIntent.getActivity(this,0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val contentView = RemoteViews(packageName, R.layout.notification_layout)
        contentView.setTextViewText(R.id.tv_title,"Spremljena je nova slika")
        contentView.setTextViewText(R.id.tv_content, imageUri.toString())

        notificationChannel = NotificationChannel(R.string.channelId.toString(), R.string.notificationTitle.toString(), NotificationManager.IMPORTANCE_HIGH)
        notificationChannel.enableLights(true)
        notificationChannel.lightColor = Color.GREEN
        notificationChannel.enableVibration(false)
        notificationManager.createNotificationChannel(notificationChannel)

        val builder = NotificationCompat.Builder(this, R.string.channelId.toString())
            .setContent(contentView)
            .setSmallIcon(R.drawable.email_icon)
            .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.drawable.email_icon))
            .setContentIntent(pendingIntent)

        notificationManager.notify(1234, builder.build())
    }

    private fun setNotificationManager() {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
}
