package com.example.czytam

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity





class CoinManagerActivity : AppCompatActivity() {
    private lateinit var tvCoinBalance: TextView
    private lateinit var tvZlotyValue: TextView

    private lateinit var btnConvert: Button
    private lateinit var btnParents: Button
    private lateinit var btnSettings: Button
    private lateinit var btnClear: Button
    private lateinit var btnClose: TextView

    private var coins: Int = 0
    private var kRate: Double = 0.1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coin_manager)

        // Views
        tvCoinBalance = findViewById(R.id.tvCoinBalance)
        tvZlotyValue = findViewById(R.id.tvZlotyValue)
        btnConvert = findViewById(R.id.btnConvert)
        btnParents = findViewById(R.id.btnParents)
        btnSettings = findViewById(R.id.btnSettings)
        btnClear = findViewById(R.id.btnClear)
        btnClose = findViewById(R.id.btnClose)

        // Load coins and K
        coins = CoinManager.getCoins()
        kRate = CoinManager.kRate
        updateBalance()

        // ------------------ Button Listeners ------------------

        // Convert to złoty
        btnConvert.setOnClickListener {
            val value = coins * kRate
            AlertDialog.Builder(this)
                .setTitle("Wartość w złotych")
                .setMessage("Masz ${String.format("%.2f", value)} zł")
                .setPositiveButton("OK") { _, _ ->
                    tvZlotyValue.text = "W złotych: ${String.format("%.2f", value)}"
                }
                .show()
        }

        // Parents button → toggle visibility of hidden buttons
        btnParents.setOnClickListener {
            val newVisibility = if (btnSettings.visibility == Button.VISIBLE) Button.GONE else Button.VISIBLE
            btnSettings.visibility = newVisibility
            btnClear.visibility = newVisibility
        }

        // Settings (change K)
        btnSettings.setOnClickListener {
            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            input.hint = "Obecne K = ${String.format("%.2f", kRate)}"

            AlertDialog.Builder(this)
                .setTitle("Ustaw nagrody (K)")
                .setView(input)
                .setPositiveButton("Zapisz") { _, _ ->
                    val newK = input.text.toString().toDoubleOrNull()
                    if (newK != null) {
                        CoinManager.saveKRate(this, newK)
                        kRate = newK
                        Toast.makeText(this, "Nowe K = ${String.format("%.2f", newK)}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Niepoprawna wartość", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Anuluj", null)
                .show()
        }

        // Clear balance with confirmation
        btnClear.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Potwierdzenie")
                .setMessage("Czy na pewno chcesz wyczyścić rachunek?")
                .setPositiveButton("Tak") { _, _ ->
                    coins = 0
                    CoinManager.coin = 0
                    tvZlotyValue.text = ""
                    updateBalance()
                    Toast.makeText(this, "Rachunek wyczyszczony", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Nie", null)
                .show()
        }

        // Close button
        btnClose.setOnClickListener { finish() }
    }

    private fun updateBalance() {
        tvCoinBalance.text = "Coins: ${CoinManager.getCoins()}"
    }
}
