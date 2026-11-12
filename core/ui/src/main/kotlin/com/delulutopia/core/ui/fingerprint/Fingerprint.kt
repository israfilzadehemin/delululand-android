package com.delulutopia.core.ui.fingerprint

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.delulutopia.core.i18n.t
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Fingerprint unlock (design X1, agreed: biometrics stand in for typing the PIN at login). The PIN
 * is kept on this phone only, encrypted with an Android Keystore key that opens only after a strong
 * biometric check; the server and its PIN rules are unchanged. A new fingerprint enrolled on the
 * phone invalidates the key, so the PIN is asked again.
 */
object Fingerprint {
    private const val STORE = "AndroidKeyStore"
    private const val PREFS = "fingerprint"
    private const val OFFERED = "offered."
    private const val PIN = "pin."

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun alias(userId: Int) = "dl_pin_$userId"

    /** A strong biometric is enrolled and usable on this phone. */
    fun available(context: Context): Boolean = BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    fun isOn(context: Context, userId: Int): Boolean = prefs(context).contains(PIN + userId)

    /** X1: offered once per citizen on this phone. */
    fun offered(context: Context, userId: Int): Boolean = prefs(context).getBoolean(OFFERED + userId, false)

    fun markOffered(context: Context, userId: Int) = prefs(context).edit().putBoolean(OFFERED + userId, true).apply()

    fun turnOff(context: Context, userId: Int) {
        prefs(context).edit().remove(PIN + userId).apply()
        runCatching { KeyStore.getInstance(STORE).apply { load(null) }.deleteEntry(alias(userId)) }
    }

    private fun key(userId: Int, create: Boolean): SecretKey? {
        val store = KeyStore.getInstance(STORE).apply { load(null) }
        (store.getKey(alias(userId), null) as? SecretKey)?.let { if (!create) return it }
        if (!create) return null
        val spec = KeyGenParameterSpec.Builder(alias(userId), KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, STORE).run {
            init(spec)
            generateKey()
        }
    }

    private fun prompt(activity: FragmentActivity, title: String, cipher: Cipher, onCipher: (Cipher) -> Unit, onFail: () -> Unit) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                result.cryptoObject?.cipher?.let(onCipher) ?: onFail()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onFail()
        }
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setNegativeButtonText(t("pin.usePin"))
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()
        BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback).authenticate(info, BiometricPrompt.CryptoObject(cipher))
    }

    /** Asks for a fingerprint, then keeps [pin] for [userId]; [onDone] says whether it is on. */
    fun turnOn(activity: FragmentActivity, userId: Int, pin: String, title: String, onDone: (Boolean) -> Unit) {
        val cipher = runCatching {
            Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key(userId, create = true)) }
        }.getOrElse {
            onDone(false)
            return
        }
        prompt(activity, title, cipher, { c ->
            val sealed = runCatching { c.iv + c.doFinal(pin.toByteArray()) }.getOrNull()
            if (sealed == null) {
                onDone(false)
            } else {
                prefs(activity).edit().putString(PIN + userId, Base64.encodeToString(sealed, Base64.NO_WRAP)).apply()
                onDone(true)
            }
        }) { onDone(false) }
    }

    /** Asks for a fingerprint and hands back the kept PIN; [onFail] for Use PIN, cancel or a lost key. */
    fun unlock(activity: FragmentActivity, userId: Int, title: String, onPin: (String) -> Unit, onFail: () -> Unit) {
        val sealed = prefs(activity).getString(PIN + userId, null)?.let { Base64.decode(it, Base64.NO_WRAP) }
        if (sealed == null || sealed.size <= 12) return onFail()
        val cipher = try {
            Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, key(userId, create = false) ?: throw KeyPermanentlyInvalidatedException(), GCMParameterSpec(128, sealed, 0, 12))
            }
        } catch (_: KeyPermanentlyInvalidatedException) {
            turnOff(activity, userId)
            return onFail()
        } catch (_: Exception) {
            return onFail()
        }
        prompt(activity, title, cipher, { c ->
            runCatching { String(c.doFinal(sealed, 12, sealed.size - 12)) }.fold(onPin) { onFail() }
        }, onFail)
    }

    /** What a just-entered PIN waits in until the signed-in app offers fingerprint unlock (X1). */
    @Volatile var justEntered: Pair<Int, String>? = null
}
