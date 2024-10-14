package com.cuatrodivinas.taller2.logica

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.cuatrodivinas.taller2.databinding.ActivityImageBinding
import com.cuatrodivinas.taller2.datos.Data.Companion.MY_PERMISSION_REQUEST_CAMERA
import com.cuatrodivinas.taller2.datos.Data.Companion.MY_PERMISSION_REQUEST_GALLERY
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageBinding
    private lateinit var photoUri: Uri

    // El onRequestPermissionsResult se reemplaza por ActivityResultContracts.RequestMultiplePermissions
    // Porque se tienen que solicitar y aceptar varios permisos a la vez
    private val requestGalleryPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        when {
            permissions.all { it.value } -> {
                // Todos los permisos concedidos -> seleccionar una imagen de la galería
                seleccionarDeGaleria()
            }
            permissions.any { !it.value } -> {
                // Algún permiso fue denegado
                Toast.makeText(this, "Permisos de Galería denegados", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()
        binding.btnGaleria.setOnClickListener {
            pedirPermisosGaleria("Necesitamos acceder a la galería para seleccionar y mostrar una imagen en la app")
        }
        binding.btnCamara.setOnClickListener {
            pedirPermiso(this, android.Manifest.permission.CAMERA,
                "Necesitamos acceder a la cámara para tomar la foto", MY_PERMISSION_REQUEST_CAMERA
            )
        }
    }

    // Función para solicitar los permisos de galería y mostrar justificación si es necesario
    private fun pedirPermisosGaleria(justificacion: String) {
        // Array de permisos a solicitar basado en la versión de Android del dispositivo
        val permisos = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO
            )
            else -> arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        // Verificar si se debe mostrar una justificación para cualquiera de los permisos
        if (permisos.any { shouldShowRequestPermissionRationale(it) }) {
            mostrarJustificacion(
                justificacion
            ) {
                // Lanzar la solicitud de permisos después de la justificación
                requestGalleryPermissions.launch(permisos)
            }
        } else {
            // Lanzar la solicitud de permisos sin justificación
            requestGalleryPermissions.launch(permisos)
        }
    }

    // Función para mostrar la justificación con un diálogo y volver a solicitar el permiso
    private fun mostrarJustificacion(mensaje: String, onAccept: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Justificación de permisos")
            .setMessage(mensaje)
            .setPositiveButton("Aceptar") { dialog, _ ->
                onAccept()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun pedirPermiso(context: Context, permiso: String, justificacion: String,
                             idCode: Int){
        if(ContextCompat.checkSelfPermission(context, permiso) !=
            PackageManager.PERMISSION_GRANTED){
            if (shouldShowRequestPermissionRationale(permiso)) {
                // Explicar al usuario por qué necesitamos el permiso
                mostrarJustificacion(justificacion) {
                    requestPermissions(arrayOf(permiso), idCode)
                }
            } else {
                requestPermissions(arrayOf(permiso), idCode)
            }
        }
        else{
            // Permiso ya concedido, tomar la foto
            takePicture()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSION_REQUEST_CAMERA -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permiso concedido, tomar la foto
                    takePicture()
                } else {
                    Toast.makeText(this, "Funcionalidades reducidas", Toast.LENGTH_LONG).show()
                }
                return
            }
        }
    }

    private fun takePicture() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            val photoFile: File = createImageFile()
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            startActivityForResult(takePictureIntent, MY_PERMISSION_REQUEST_CAMERA)
        } catch (e: ActivityNotFoundException) {
            e.message?. let{ Log.e("PERMISSION_APP",it) }
            Toast.makeText(this, "No es posible abrir la cámara", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): File {
        // El timestamp se usa para que el nombre del archivo sea único
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        // El directorio donde se guardará la imagen
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        // Crear el archivo con el nombre "JPEG_YYYYMMDD_HHMMSS.jpg" en el directorio storageDir
        return File.createTempFile(
            "JPEG_${timestamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Guardar la URI del archivo para usarla en el Intent de la cámara
            photoUri = FileProvider.getUriForFile(this@ImageActivity, "com.cuatrodivinas.taller2.fileprovider", this)
        }
    }

    private fun seleccionarDeGaleria() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        try {
            startActivityForResult(intent, MY_PERMISSION_REQUEST_GALLERY)
        } catch (e: ActivityNotFoundException) {
            e.message?. let{ Log.e("PERMISSION_APP",it) }
            Toast.makeText(this, "No es posible abrir la galería", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            MY_PERMISSION_REQUEST_CAMERA -> {
                if (resultCode == RESULT_OK) {
                    binding.imagen.setImageURI(photoUri)
                }
            }
            MY_PERMISSION_REQUEST_GALLERY -> {
                if (resultCode == RESULT_OK) {
                    try {
                        val imageUri = data?.data
                        val imageStream: InputStream? = contentResolver.openInputStream(imageUri!!)
                        val selectedImage = BitmapFactory.decodeStream(imageStream)
                        binding.imagen.setImageBitmap(selectedImage)
                    } catch (e: Exception) {
                        e.message?. let{ Log.e("PERMISSION_APP",it) }
                        Toast.makeText(this, "No fue posible seleccionar la imagen (exc.)", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}