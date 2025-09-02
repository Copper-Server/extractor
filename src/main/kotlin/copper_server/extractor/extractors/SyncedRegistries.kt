package copper_server.extractor.extractors

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import copper_server.extractor.Extractor
import net.minecraft.registry.RegistryLoader
import net.minecraft.server.MinecraftServer

class SyncedRegistries : Extractor {
    override fun extract(server: MinecraftServer): JsonElement {
        val json = JsonArray()
        for (entry in RegistryLoader.SYNCED_REGISTRIES) 
            json.add(entry.key().value.path)

        return json
    }
}
