package nl.teunk.currere.data.credentials

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import com.google.crypto.tink.Aead
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class ServerCredentials(
    val baseUrl: String,
    val token: String,
)

class CredentialsManager(context: Context) {

    private val aead: Aead = TinkEncryption.getAead(context)

    private val store: DataStore<ServerCredentials?> = DataStoreFactory.create(
        serializer = EncryptedCredentialsSerializer(aead),
        produceFile = { File(context.filesDir, "datastore/credentials.enc") },
    )

    val credentials: Flow<ServerCredentials?> = store.data

    suspend fun save(baseUrl: String, token: String) {
        store.updateData { ServerCredentials(baseUrl = baseUrl.trimEnd('/'), token = token) }
    }

    suspend fun clear() {
        store.updateData { null }
    }
}

private class EncryptedCredentialsSerializer(
    private val aead: Aead,
) : Serializer<ServerCredentials?> {

    override val defaultValue: ServerCredentials? = null

    override suspend fun readFrom(input: InputStream): ServerCredentials? {
        val encrypted = input.readBytes()
        if (encrypted.isEmpty()) return null
        return try {
            val decrypted = aead.decrypt(encrypted, null)
            Json.decodeFromString<ServerCredentials>(decrypted.decodeToString())
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun writeTo(t: ServerCredentials?, output: OutputStream) {
        if (t == null) {
            output.write(ByteArray(0))
        } else {
            val json = Json.encodeToString(ServerCredentials.serializer(), t)
            val encrypted = aead.encrypt(json.toByteArray(), null)
            output.write(encrypted)
        }
    }
}
