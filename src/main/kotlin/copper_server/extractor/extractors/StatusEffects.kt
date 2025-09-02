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

class StatusEffects : Extractor {
    override fun extract(server: MinecraftServer): JsonElement {
        val statusJson = JsonObject()
        val statusEffectRegistry = server.registryManager.getOrThrow(RegistryKeys.STATUS_EFFECT)
        val attributesRegistry = server.registryManager.getOrThrow(RegistryKeys.ATTRIBUTE)
        for (effect in statusEffectRegistry) {
            val json = JsonObject()
            json.addProperty("id", statusEffectRegistry.getRawId(effect))
            json.addProperty("fade_in_ticks", effect.fadeInTicks)
            json.addProperty("fade_out_ticks", effect.fadeOutTicks)
            json.addProperty("fade_out_threshold_ticks", effect.fadeOutThresholdTicks)
            json.addProperty("is_instant", effect.isInstant)
            json.addProperty("translation_key", effect.translationKey)
            json.addProperty("name", effect.name.string)
            json.addProperty("category", effect.category.name)
            json.addProperty("rgb", effect.color)
            json.addProperty("is_beneficial", effect.isBeneficial)
            json.add(
                "required_features",
                FeatureFlags.CODEC.encodeStart(
                    RegistryOps.of(JsonOps.INSTANCE, server.registryManager),
                    effect.requiredFeatures
                ).getOrThrow()
            )
            val attributeModifiers = JsonArray()
            effect.forEachAttributeModifier(
                0,
                { reg, mod ->
                    val modJson = JsonObject()
                    modJson.addProperty("id", attributesRegistry.getRawId(reg.value()))
                    modJson.addProperty("operation", mod.operation().toString())
                    modJson.addProperty("value", mod.value())
                    attributeModifiers.add(modJson)
                }
            )
            json.add("attribute_modifiers", attributeModifiers)
            statusJson.add(statusEffectRegistry.getId(effect)!!.toString(), json)
        }

        return statusJson
    }
}
