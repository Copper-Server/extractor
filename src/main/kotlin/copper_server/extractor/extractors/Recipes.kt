package copper_server.extractor.extractors

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.mojang.serialization.JsonOps
import copper_server.extractor.Extractor
import net.minecraft.recipe.Recipe
import net.minecraft.registry.RegistryOps
import net.minecraft.server.MinecraftServer

class Recipes : Extractor {
    override fun extract(server: MinecraftServer): JsonElement {
        val json = JsonArray()

        for (recipeRaw in server.recipeManager.values()) {
            val recipe = recipeRaw.value
            json.add(
                Recipe.CODEC.encodeStart(
                    RegistryOps.of(JsonOps.INSTANCE, server.registryManager),
                    recipe
                ).getOrThrow()
            )
        }

        return json
    }
}
