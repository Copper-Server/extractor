package copper_server.extractor.extractors

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import copper_server.extractor.Extractor
import net.minecraft.registry.Registries
import net.minecraft.server.MinecraftServer

class EntityAttributes : Extractor {
    override fun extract(server: MinecraftServer): JsonElement {
        val json = JsonObject()
        for (attribute in Registries.ATTRIBUTE)
            json.addProperty(Registries.ATTRIBUTE.getId(attribute)!!.toString(), attribute.defaultValue)

        return json
    }
}
