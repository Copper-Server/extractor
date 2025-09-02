package copper_server.extractor.extractors

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import copper_server.extractor.Extractor
import net.minecraft.entity.EntityPose
import net.minecraft.server.MinecraftServer

class EntityPose : Extractor {
    override fun extract(server: MinecraftServer): JsonElement {
        val json = JsonObject()
        for (pose in EntityPose.entries)
            json.addProperty(pose.name, pose.index)

        return json
    }
}
