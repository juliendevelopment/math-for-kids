package be.arte.mathforkids.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.Executors

/**
 * Everything the app remembers, in one JSON file inside the app's private storage.
 * Nothing leaves the device.
 */
class Store(private val file: File) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val io = Executors.newSingleThreadExecutor()

    var data: SaveData by mutableStateOf(load())
        private set

    private fun load(): SaveData = try {
        if (file.exists()) json.decodeFromString(SaveData.serializer(), file.readText()) else SaveData()
    } catch (e: Exception) {
        // Keep the unreadable file aside rather than silently overwriting a child's progress.
        runCatching { file.copyTo(File(file.parentFile, file.name + ".corrompu"), overwrite = true) }
        SaveData()
    }

    fun update(change: (SaveData) -> SaveData) {
        val next = change(data)
        if (next == data) return
        data = next
        io.execute {
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(json.encodeToString(SaveData.serializer(), next))
            if (!tmp.renameTo(file)) {
                file.writeText(tmp.readText())
                tmp.delete()
            }
        }
    }

    fun profile(id: String?): Profile? = data.profiles.firstOrNull { it.id == id }

    fun updateProfile(id: String, change: (Profile) -> Profile) =
        update { d -> d.copy(profiles = d.profiles.map { if (it.id == id) change(it) else it }) }

    fun addProfile(p: Profile) = update { it.copy(profiles = it.profiles + p) }

    fun deleteProfile(id: String) = update { d -> d.copy(profiles = d.profiles.filterNot { it.id == id }) }
}
