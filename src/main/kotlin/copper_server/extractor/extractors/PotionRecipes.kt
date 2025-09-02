package copper_server.extractor.extractors

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import copper_server.extractor.Extractor
import copper_server.extractor.bridges.BrewingRecipeRegistryExtract
import net.minecraft.recipe.Ingredient
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.RegistryOps
import net.minecraft.server.MinecraftServer

class PotionRecipes : Extractor {
    override fun extract(server: MinecraftServer): JsonElement {
        val potionRegistry = server.registryManager.getOrThrow(RegistryKeys.POTION)
        val itemRegistry = server.registryManager.getOrThrow(RegistryKeys.ITEM)
        val potionRecipes = JsonArray()
        for (it in BrewingRecipeRegistryExtract.getExtractedRecipesItem()) {
            val potionRecipe = JsonObject()
            potionRecipe.addProperty("input", potionRegistry.getRawId(it.input))
            potionRecipe.addProperty("item", itemRegistry.getRawId(it.item))
            potionRecipe.addProperty("output", potionRegistry.getRawId(it.output))
            potionRecipes.add(potionRecipe)
        }

        for (it in BrewingRecipeRegistryExtract.getExtractedRecipesIngredient()) {
            val potionRecipe = JsonObject()
            potionRecipe.addProperty("input", potionRegistry.getRawId(it.input))
            potionRecipe.add(
                "item",
                Ingredient.CODEC.encodeStart(
                    RegistryOps.of(JsonOps.INSTANCE, server.registryManager),
                    it.item
                ).getOrThrow()
            )
            potionRecipe.addProperty("output", potionRegistry.getRawId(it.output))
            potionRecipes.add(potionRecipe)
        }

        return potionRecipes
    }
}
