package nl.teunk.currere.data.security

import android.content.Context
import java.security.SecureRandom

object PassphraseProvider {

    private const val PREF_NAME = "currere_db_passphrase"
    private const val KEY_PASSPHRASE = "encrypted_passphrase"
    private const val KEYSET_NAME = "currere_db_keyset"
    private const val KEYSET_PREF_FILE = "currere_db_keyset_prefs"
    private const val PASSPHRASE_LENGTH = 32

    fun getPassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_PASSPHRASE, null)
        if (existing != null) {
            val aead = TinkEncryption.getAead(context, KEYSET_NAME, KEYSET_PREF_FILE)
            val encrypted = android.util.Base64.decode(existing, android.util.Base64.NO_WRAP)
            return aead.decrypt(encrypted, null)
        }

        val passphrase = ByteArray(PASSPHRASE_LENGTH).also { SecureRandom().nextBytes(it) }
        val aead = TinkEncryption.getAead(context, KEYSET_NAME, KEYSET_PREF_FILE)
        val encrypted = aead.encrypt(passphrase, null)
        val encoded = android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP)
        prefs.edit().putString(KEY_PASSPHRASE, encoded).apply()
        return passphrase
    }
}
