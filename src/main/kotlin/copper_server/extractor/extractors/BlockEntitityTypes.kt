package copper_server.extractor.extractors

import com.google.gson.JsonElement
import com.google.gson.JsonArray
import copper_server.extractor.Extractor
import net.minecraft.registry.Registries
import net.minecraft.server.MinecraftServer

class BlockEntityTypes : Extractor {
    override fun extract(server: MinecraftServer): JsonElement {
        val blockEntitiesJson = JsonArray()
        for (blockEntity in Registries.BLOCK_ENTITY_TYPE) blockEntitiesJson.add(
                Registries.BLOCK_ENTITY_TYPE.getId(blockEntity)!!.toString()
        )
        return blockEntitiesJson
    }
}
