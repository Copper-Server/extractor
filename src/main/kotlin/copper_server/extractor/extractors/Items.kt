package copper_server.extractor.extractors

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import copper_server.extractor.Extractor
import net.minecraft.component.ComponentMap
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.RegistryOps
import net.minecraft.server.MinecraftServer

class Items : Extractor {
    override fun extract(server: MinecraftServer): JsonElement {
        val json = JsonObject()
        for (item in server.registryManager.getOrThrow(RegistryKeys.ITEM)) {
            val itemJson = JsonObject()
            itemJson.addProperty("id", Registries.ITEM.getRawId(item))
            itemJson.add(
                "components",
                ComponentMap.CODEC.encodeStart(
                    RegistryOps.of(JsonOps.INSTANCE, server.registryManager),
                    item.components
                ).getOrThrow()
            )

            json.add(Registries.ITEM.getId(item).toString(), itemJson)
        }

        return json
    }
}
