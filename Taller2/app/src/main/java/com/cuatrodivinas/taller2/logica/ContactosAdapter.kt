package com.cuatrodivinas.taller2.logica

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import android.widget.TextView
import com.cuatrodivinas.taller2.R

class ContactosAdapter(context: Context?, c: Cursor?, flags: Int) : CursorAdapter(context, c, flags) {
    private val CONTACT_ID_INDEX = 0
    private val DISPLAY_NAME_INDEX = 1

    override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
        return LayoutInflater.from(context)
            .inflate(R.layout.layout_contactos, parent, false)
    }

    override fun bindView(view: View?, context: Context?, cursor: Cursor?) {
        val tvIdContacto = view?.findViewById<TextView>(R.id.txtNum)
        val tvNombre = view?.findViewById<TextView>(R.id.txtNombre)
        val idnum = cursor?.getInt(CONTACT_ID_INDEX)
        val nombre = cursor?.getString(DISPLAY_NAME_INDEX)
        tvIdContacto?.text = idnum?.toString()
        tvNombre?.text = nombre
    }
}