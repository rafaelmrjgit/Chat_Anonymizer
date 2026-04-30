package com.rma.chatanonymizer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.rma.chatanonymizer.databinding.ActivityMainBinding
import com.rma.chatanonymizer.logic.ChatAnonymizer
import com.rma.chatanonymizer.ui.ChatAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
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
                saveLauncher.launch("conversa_anonimizada.txt")
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
                        // FORÇAMOS A LEITURA EM UTF-8
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
            binding.layoutActions.visibility = View.VISIBLE
            binding.rvPreview.layoutManager = LinearLayoutManager(this)
            binding.rvPreview.adapter = ChatAdapter(anonymizedLines)
        }
    }

    private fun shareAnonymizedText() {
        val fullText = anonymizedLines.joinToString("\n")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, fullText)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.btn_share)))
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
                Toast.makeText(this@MainActivity, "Erro ao salvar", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

