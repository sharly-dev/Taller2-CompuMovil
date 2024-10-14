package com.cuatrodivinas.taller2.logica

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cuatrodivinas.taller2.R
import com.cuatrodivinas.taller2.databinding.ActivityContactosBinding
import com.cuatrodivinas.taller2.databinding.ActivityMainBinding
import com.cuatrodivinas.taller2.datos.Data.Companion.MY_PERMISSION_REQUEST_READ_CONTACTS

class ContactosActivity : AppCompatActivity() {
    private lateinit var binding: ActivityContactosBinding
    private var mProjection: Array<String>? = null
    private var mCursor: Cursor? = null
    private var mContactsAdapter: ContactosAdapter? = null
    private var mlista: ListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mlista = binding.listViewContactos
        mProjection = arrayOf(ContactsContract.Profile._ID, ContactsContract.Profile.DISPLAY_NAME_PRIMARY)
        mContactsAdapter = ContactosAdapter(this, null, 0)
        mlista?.adapter = mContactsAdapter

        pedirPermiso(this, android.Manifest.permission.READ_CONTACTS,
            "Necesitamos acceder a tus contactos para mostrarlos", MY_PERMISSION_REQUEST_READ_CONTACTS
        )
    }

    private fun pedirPermiso(context: Activity, permiso: String, justificacion: String, idCode: Int) {
        if (ContextCompat.checkSelfPermission(context, permiso) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(permiso)) {
                // Explicar al usuario por qué necesitamos el permiso
                Toast.makeText(context, justificacion, Toast.LENGTH_SHORT).show()
            }
            requestPermissions(arrayOf(permiso), idCode)
        } else {
            Toast.makeText(context, "Permiso Contactos ya fue concedido", Toast.LENGTH_SHORT).show()
            // Llenar el ListView con los contactos
            initView()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSION_REQUEST_READ_CONTACTS -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission is granted. Continue the action or workflow in your app.
                    Toast.makeText(this, "Permiso concedido!", Toast.LENGTH_SHORT).show()
                    // Llenar el ListView con los contactos
                    initView()
                } else {
                    // Explain to the user that the feature is unavailable
                    Toast.makeText(this, "Permiso denegado!", Toast.LENGTH_SHORT).show()
                }
                return
            }
            else -> {
                // Ignore all other requests.

            }
        }
    }

    private fun initView() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            // Si el usuario concedió el permiso
            mCursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI, mProjection, null, null, null
            )
            mContactsAdapter?.changeCursor(mCursor)
        }
    }
}