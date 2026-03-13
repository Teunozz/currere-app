package nl.teunk.currere.data.security

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.security.KeyStore
import androidx.core.content.edit

object TinkEncryption {

    private const val MASTER_KEY_ALIAS = "currere_master_key"
    private const val MASTER_KEY_URI = "android-keystore://$MASTER_KEY_ALIAS"

    fun getAead(
        context: Context,
        keysetName: String = "currere_credentials_keyset",
        prefFileName: String = "currere_credentials_keyset_prefs",
    ): Aead {
        AeadConfig.register()
        return try {
            buildAead(context, keysetName, prefFileName)
        } catch (_: Exception) {
            clearCorruptedKey(context, prefFileName)
            buildAead(context, keysetName, prefFileName)
        }
    }

    private fun buildAead(context: Context, keysetName: String, prefFileName: String): Aead {
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, keysetName, prefFileName)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
        return keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    private fun clearCorruptedKey(context: Context, prefFileName: String) {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        keyStore.deleteEntry(MASTER_KEY_ALIAS)
        context.getSharedPreferences(prefFileName, Context.MODE_PRIVATE).edit { clear() }
    }
}
