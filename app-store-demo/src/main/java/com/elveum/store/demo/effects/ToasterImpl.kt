package com.elveum.store.demo.effects

import android.content.Context
import android.widget.Toast
import com.uandcode.hilt.autobind.AutoBinds
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@AutoBinds
class ToasterImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : Toaster {

    override fun toast(message: String?) {
        Toast.makeText(context, message ?: "Error!", Toast.LENGTH_SHORT).show()
    }

}
