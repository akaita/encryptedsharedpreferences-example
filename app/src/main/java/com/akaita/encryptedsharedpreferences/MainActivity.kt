/*
* Created by Mikel (mikel@4rtstudio.com) on 12/10/2019.
*/
package com.akaita.encryptedsharedpreferences

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : AppCompatActivity() {

    private val preferencesName = "SharedPreferences"

    // Step 0: EncryptedSharedPreferences take long to initialize/open, therefor it's better to do it only once and keep an instance
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initEncrypted.setOnCheckedChangeListener { _, checked -> initSharedPreferences(checked) }
        saveButton.setOnClickListener { saveValue() }
        readButton.setOnClickListener { readValue() }

        initEncrypted.isChecked = true
    }

    private fun initSharedPreferences(checked: Boolean) {
        resetSharedPreferences()

        if (checked) {
            initEncryptedSharedPreferences()
        } else {
            initCleartextSharedPreferences()
        }

        hideKeyboard()
        showRawFile()
    }

    @SuppressLint("ApplySharedPref")
    private fun resetSharedPreferences() {
        getSharedPreferences(preferencesName, MODE_PRIVATE)
            .edit()
            .clear()
            .commit() //note: I use `commit` in order to measure raw performance. Please use `apply` in your apps
    }

    private fun initEncryptedSharedPreferences() {
        val startTs = System.currentTimeMillis()

        // Step 1: Create or retrieve the Master Key for encryption/decryption
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        // Step 2: Initialize/open an instance of EncryptedSharedPreferences
        sharedPreferences = EncryptedSharedPreferences.create(
            preferencesName,
            masterKeyAlias,
            applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val endTs = System.currentTimeMillis()
        initTimestamp.visibility = View.VISIBLE
        initTimestamp.text = getString(R.string.timestamp).format(endTs - startTs)
    }


    private fun initCleartextSharedPreferences() {
        val startTs = System.currentTimeMillis()

        sharedPreferences = getSharedPreferences(preferencesName, MODE_PRIVATE)

        val endTs = System.currentTimeMillis()
        initTimestamp.visibility = View.VISIBLE
        initTimestamp.text = getString(R.string.timestamp).format(endTs - startTs)
    }

    @SuppressLint("ApplySharedPref")
    private fun saveValue() {
        val startTs = System.currentTimeMillis()

        // Step 3: Save data to the EncryptedSharedPreferences as usual
        sharedPreferences.edit()
            .putString("DATA", saveText.text.toString())
            .commit() //note: I use `commit` in order to measure raw performance. Please use `apply` in your apps

        val endTs = System.currentTimeMillis()
        saveTimestamp.visibility = View.VISIBLE
        saveTimestamp.text = getString(R.string.timestamp).format(endTs - startTs)

        hideKeyboard()
        showRawFile()
    }

    private fun readValue() {
        val startTs = System.currentTimeMillis()

        // Step 3: Read data from EncryptedSharedPreferences as usual
        val value = sharedPreferences.getString("DATA", "")
        readText.setText(value)

        val endTs = System.currentTimeMillis()
        readTimestamp.visibility = View.VISIBLE
        readTimestamp.text = getString(R.string.timestamp).format(endTs - startTs)

        hideKeyboard()
        showRawFile()
    }

    private fun showRawFile() {
        val preferencesFile = File("${applicationInfo.dataDir}/shared_prefs/$preferencesName.xml")
        if (preferencesFile.exists()) {
            fileText.text = preferencesFile.readText().highlight()
        } else {
            fileText.text = ""
        }
    }


    //region info dialog
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAboutDialog() {
        val builder = AlertDialog.Builder(this).apply {
            setIcon(R.mipmap.ic_launcher)
            setTitle(getString(R.string.app_name))
            setView(layoutInflater.inflate(R.layout.about, null))
            setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
        }
        builder.create().show()
    }
    //endregion
}
