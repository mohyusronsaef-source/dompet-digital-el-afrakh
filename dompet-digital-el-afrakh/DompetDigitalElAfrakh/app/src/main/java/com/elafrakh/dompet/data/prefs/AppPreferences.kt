package com.elafrakh.dompet.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.elafrakh.dompet.data.security.PinCryptoManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _initialBalanceFlow = MutableStateFlow(prefs.getLong(KEY_INITIAL_BALANCE, 0L))
    val initialBalanceFlow: StateFlow<Long> = _initialBalanceFlow.asStateFlow()

    private val _isDarkModeFlow = MutableStateFlow(prefs.getBoolean(KEY_DARK_MODE, false))
    val isDarkModeFlow: StateFlow<Boolean> = _isDarkModeFlow.asStateFlow()

    private val _hasPinSetupFlow = MutableStateFlow(!prefs.getString(KEY_PIN_HASH, null).isNullOrBlank())
    val hasPinSetupFlow: StateFlow<Boolean> = _hasPinSetupFlow.asStateFlow()

    private val _hasInitialBalanceSetupFlow = MutableStateFlow(
        prefs.getBoolean(KEY_INITIAL_BALANCE_SET, false)
    )
    val hasInitialBalanceSetupFlow: StateFlow<Boolean> = _hasInitialBalanceSetupFlow.asStateFlow()

    fun isPinConfigured(): Boolean = !prefs.getString(KEY_PIN_HASH, null).isNullOrBlank()

    fun isInitialBalanceConfigured(): Boolean = prefs.getBoolean(KEY_INITIAL_BALANCE_SET, false)

    fun getInitialBalanceSnapshot(): Long = prefs.getLong(KEY_INITIAL_BALANCE, 0L)

    fun setInitialBalance(amount: Long) {
        val safeAmount = amount.coerceAtLeast(0L)
        prefs.edit()
            .putLong(KEY_INITIAL_BALANCE, safeAmount)
            .putBoolean(KEY_INITIAL_BALANCE_SET, true)
            .apply()
        _initialBalanceFlow.value = safeAmount
        _hasInitialBalanceSetupFlow.value = true
    }

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        _isDarkModeFlow.value = enabled
    }

    fun saveNewPin(pin: String): String {
        val hashedPin = PinCryptoManager.hashSecret(pin)
        val recoveryCode = PinCryptoManager.generateRecoveryCode()
        val hashedRecovery = PinCryptoManager.hashSecret(
            PinCryptoManager.normalizeRecoveryCode(recoveryCode)
        )
        prefs.edit()
            .putString(KEY_PIN_HASH, hashedPin.hashBase64)
            .putString(KEY_PIN_SALT, hashedPin.saltBase64)
            .putString(KEY_RECOVERY_HASH, hashedRecovery.hashBase64)
            .putString(KEY_RECOVERY_SALT, hashedRecovery.saltBase64)
            .apply()
        _hasPinSetupFlow.value = true
        return recoveryCode
    }

    fun updatePinOnly(newPin: String) {
        val hashedPin = PinCryptoManager.hashSecret(newPin)
        prefs.edit()
            .putString(KEY_PIN_HASH, hashedPin.hashBase64)
            .putString(KEY_PIN_SALT, hashedPin.saltBase64)
            .apply()
        _hasPinSetupFlow.value = true
    }

    fun verifyPin(candidate: String): Boolean {
        val hash = prefs.getString(KEY_PIN_HASH, null)
        val salt = prefs.getString(KEY_PIN_SALT, null)
        return PinCryptoManager.verifySecret(candidate, hash, salt)
    }

    fun verifyRecoveryCode(candidate: String): Boolean {
        val hash = prefs.getString(KEY_RECOVERY_HASH, null)
        val salt = prefs.getString(KEY_RECOVERY_SALT, null)
        return PinCryptoManager.verifySecret(
            PinCryptoManager.normalizeRecoveryCode(candidate),
            hash,
            salt
        )
    }

    fun resetAllDataPrefs() {
        val darkMode = _isDarkModeFlow.value
        prefs.edit()
            .clear()
            .putBoolean(KEY_DARK_MODE, darkMode)
            .apply()
        _initialBalanceFlow.value = 0L
        _hasPinSetupFlow.value = false
        _hasInitialBalanceSetupFlow.value = false
    }

    companion object {
        private const val PREFS_NAME = "el_afrakh_wallet_prefs"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_RECOVERY_HASH = "recovery_hash"
        private const val KEY_RECOVERY_SALT = "recovery_salt"
        private const val KEY_INITIAL_BALANCE = "initial_balance"
        private const val KEY_INITIAL_BALANCE_SET = "initial_balance_set"
        private const val KEY_DARK_MODE = "dark_mode"
    }
}
