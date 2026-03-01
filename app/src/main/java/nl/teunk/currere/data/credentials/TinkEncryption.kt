package nl.teunk.currere.data.credentials

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.security.KeyStore
import androidx.core.content.edit

object TinkEncryption {

    private const val KEYSET_NAME = "currere_credentials_keyset"
    private const val PREF_FILE_NAME = "currere_credentials_keyset_prefs"
    private const val MASTER_KEY_ALIAS = "currere_master_key"
    private const val MASTER_KEY_URI = "android-keystore://$MASTER_KEY_ALIAS"

    fun getAead(context: Context): Aead {
        AeadConfig.register()
        return try {
            buildAead(context)
        } catch (_: Exception) {
            clearCorruptedKey(context)
            buildAead(context)
        }
    }

    private fun buildAead(context: Context): Aead {
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREF_FILE_NAME)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
        return keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    private fun clearCorruptedKey(context: Context) {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        keyStore.deleteEntry(MASTER_KEY_ALIAS)
        context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE).edit { clear() }
    }
}
