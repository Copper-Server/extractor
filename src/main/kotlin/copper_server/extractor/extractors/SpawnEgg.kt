package copper_server.extractor.extractors

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import copper_server.extractor.Extractor
import net.minecraft.item.SpawnEggItem
import net.minecraft.registry.Registries
import net.minecraft.server.MinecraftServer

class SpawnEgg : Extractor {
    override fun extract(server: MinecraftServer): JsonElement {
        val json = JsonObject()

        for (spawnEggItem in SpawnEggItem.getAll()) {
            val type = spawnEggItem.getEntityType(server.registryManager, spawnEggItem.defaultStack)
            json.addProperty(
                Registries.ITEM.getRawId(spawnEggItem).toString(),
                Registries.ENTITY_TYPE.getRawId(type)
            )
        }

        return json
    }
}
