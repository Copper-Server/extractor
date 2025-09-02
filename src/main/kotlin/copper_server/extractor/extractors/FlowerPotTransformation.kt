package copper_server.extractor.extractors

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import copper_server.extractor.Extractor
import net.minecraft.block.FlowerPotBlock
import net.minecraft.registry.Registries
import net.minecraft.server.MinecraftServer

class FlowerPotTransformation : Extractor {
    override fun extract(server: MinecraftServer): JsonElement {
        val json = JsonObject()
        for ((block, pottedBlock) in FlowerPotBlock.CONTENT_TO_POTTED) {
            if (Registries.BLOCK.getRawId(block) == 0) continue
            json.add(
                    Registries.ITEM.getRawId(block.asItem()!!).toString(),
                    JsonPrimitive(Registries.BLOCK.getRawId(pottedBlock))
            )
        }

        return json
    }
}
