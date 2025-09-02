package copper_server.extractor.extractors

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import copper_server.extractor.Extractor
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.RegistryOps
import net.minecraft.resource.featuretoggle.FeatureFlags
import net.minecraft.server.MinecraftServer

class Potion : Extractor {
    override fun extract(server: MinecraftServer): JsonElement {
        val potionData = JsonObject()
        val potionRegistry = server.registryManager.getOrThrow(RegistryKeys.POTION)
        val statusEffectRegistry = server.registryManager.getOrThrow(RegistryKeys.STATUS_EFFECT)
        for (potion in potionRegistry) {
            val json = JsonObject()
            json.addProperty("id", potionRegistry.getRawId(potion))
            val effects = JsonArray()
            for (effect in potion.effects) {
                val effectJson = JsonObject()
                effectJson.addProperty(
                    "id",
                    statusEffectRegistry.getRawId(effect.effectType.value())
                )
                effectJson.addProperty("duration", effect.duration)
                effectJson.addProperty("amplifier", effect.amplifier)
                effects.add(effectJson)
            }
            json.add("effects", effects)
            json.add(
                "required_features",
                FeatureFlags.CODEC.encodeStart(
                    RegistryOps.of(JsonOps.INSTANCE, server.registryManager),
                    potion.requiredFeatures
                ).getOrThrow()
            )
            potionData.add(potionRegistry.getId(potion)!!.toString(), json)
        }

        return potionData
    }
}
