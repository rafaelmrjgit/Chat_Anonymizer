package com.rma.chatanonymizer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.rma.chatanonymizer.databinding.ActivityMainBinding
import com.rma.chatanonymizer.logic.ChatAnonymizer
import com.rma.chatanonymizer.ui.ChatAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var anonymizedLines: List<String> = emptyList()

    private val importLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let { processFile(it) }
        }

    private val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

    // NOVO: Launcher para criar o arquivo no sistema
    private val saveLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri: Uri? ->
            uri?.let { writeFile(it) }
        }

    private val prefs by lazy { getSharedPreferences("themes_config", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        val lightDarkMode = prefs.getInt("light_dark_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(lightDarkMode)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setupListeners()
    }


    private fun setupListeners() {
        binding.btnImport.setOnClickListener {
            importLauncher.launch(arrayOf("text/plain"))
        }

        binding.btnShare.setOnClickListener {
            shareAnonymizedText()
        }

        binding.btnSave.setOnClickListener {
            if (anonymizedLines.isNotEmpty()) {
                // Abre o seletor do Android para o usuário escolher onde salvar
                saveLauncher.launch(getString(R.string.default_filename))
            }
        }
    }

    private fun processFile(uri: Uri) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnImport.isEnabled = false

        lifecycleScope.launch {
            try {
                val lines = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        // força a leitura utf-8
                        BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readLines()
                    } ?: emptyList()
                }

                val anonymizer = ChatAnonymizer(
                    maskUrls = binding.switchMaskUrls.isChecked,
                    removeTimestamps = binding.switchRemoveTimestamps.isChecked,
                    participantPrefix = getString(R.string.participant_prefix),
                    phonePlaceholder = getString(R.string.placeholder_phone),
                    emailPlaceholder = getString(R.string.placeholder_email),
                    urlPlaceholder = getString(R.string.placeholder_url)
                )

                anonymizedLines = withContext(Dispatchers.Default) {
                    anonymizer.anonymize(lines)
                }

                updateUI()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, R.string.msg_error_import, Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnImport.isEnabled = true
            }
        }
    }

    private fun updateUI() {
        if (anonymizedLines.isNotEmpty()) {
            binding.rvPreview.visibility = View.VISIBLE
            binding.cardViewResult.visibility = View.VISIBLE
            binding.rvPreview.layoutManager = LinearLayoutManager(this)
            binding.rvPreview.adapter = ChatAdapter(anonymizedLines)
        }
    }


    private fun shareAnonymizedText() {
        lifecycleScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    val tempFile = File(cacheDir, getString(R.string.default_filename))
                    tempFile.writeText(anonymizedLines.joinToString("\n"))
                    tempFile
                }

                val contentUri = FileProvider.getUriForFile(
                    this@MainActivity,
                    "${packageName}.fileprovider",
                    file
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.btn_share)))
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    R.string.msg_error_share,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }



    // ESCREVE O CONTEÚDO NO URI SELECIONADO PELO USUÁRIO
    private fun writeFile(uri: Uri) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri)?.use { output ->
                        // 1. ESCREVEMOS A ETIQUETA BOM PRIMEIRO
                        output.write(UTF8_BOM)

                        // 2. ESCREVEMOS O TEXTO EM UTF-8
                        val fullText = anonymizedLines.joinToString("\n")
                        output.write(fullText.toByteArray(Charsets.UTF_8))
                    }
                }
                Toast.makeText(this@MainActivity, R.string.msg_success_save, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, R.string.msg_error_save, Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        // recupera o estado do item de menu
        val themeItem = menu?.findItem(R.id.action_theme)

        // verifica se o modo noturno está ativo
        val isNightMode = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

        // define o ícone com base no estado atual
        if (isNightMode) {
            themeItem?.setIcon(R.drawable.ic_light_mode_24dp)
        } else {
            themeItem?.setIcon(R.drawable.ic_dark_mode_24dp)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_theme -> {
                //modo noturno está ativo no sistema?
                val isNightModeActive = resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                        android.content.res.Configuration.UI_MODE_NIGHT_YES
                if (isNightModeActive) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    prefs.edit {putInt("light_dark_mode", AppCompatDelegate.MODE_NIGHT_NO)}
                }else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    prefs.edit {putInt("light_dark_mode", AppCompatDelegate.MODE_NIGHT_YES)}
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
}

