package copper_server.extractor.extractors

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import copper_server.extractor.Extractor
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.server.MinecraftServer

class Fuels : Extractor {
    override fun extract(server: MinecraftServer): JsonElement {
        val json = JsonObject()
        for (fuel in server.fuelRegistry.fuelItems) 
            json.add(
                    Item.getRawId(fuel).toString(),
                    JsonPrimitive(server.fuelRegistry.getFuelTicks(ItemStack(fuel)))
            )
        
        return json
    }
}
