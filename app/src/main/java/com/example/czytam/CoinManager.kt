package com.example.czytam



import android.content.Context



object CoinManager {
    var coin: Int = 0
    var kRate: Double = 0.1

    private const val PREFS_NAME = "app_prefs"
    private const val KEY_K_RATE = "k_rate"
    private const val KEY_COINS = "coins"

    // Call this once in MainActivity.onCreate()
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        coin = prefs.getInt(KEY_COINS, 0) // load saved coins, default = 0

        // load saved kRate (stored as long bits), default = 0.1
        val kRateBits = prefs.getLong(KEY_K_RATE, java.lang.Double.doubleToRawLongBits(0.1))
        kRate = java.lang.Double.longBitsToDouble(kRateBits)
    }

    fun addCoins(context: Context, amount: Int) {
        coin += amount
        saveCoins(context)
    }

    fun spendCoins(context: Context, amount: Int): Boolean {
        return if (coin >= amount) {
            coin -= amount
            saveCoins(context)
            true
        } else {
            false
        }
    }

    fun getCoins(): Int = coin

    private fun saveCoins(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_COINS, coin).apply()
    }

    fun saveKRate(context: Context, newKRate: Double) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val bits = java.lang.Double.doubleToRawLongBits(newKRate)
        prefs.edit().putLong(KEY_K_RATE, bits).apply()
        kRate = newKRate
    }
}
