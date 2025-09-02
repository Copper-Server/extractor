package copper_server.extractor.extractors

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import copper_server.extractor.Extractor
import net.minecraft.block.ComposterBlock
import net.minecraft.registry.Registries
import net.minecraft.server.MinecraftServer

class ComposterIncreaseChance : Extractor {
    override fun extract(server: MinecraftServer): JsonElement {
        val json = JsonObject()
        for ((item, chance) in ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE)
            json.add(Registries.ITEM.getRawId(item.asItem()!!).toString(), JsonPrimitive(chance))
        
        return json
    }
}
