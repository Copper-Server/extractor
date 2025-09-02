package copper_server.extractor.bridges;

import net.minecraft.item.Item
import net.minecraft.potion.Potion
import net.minecraft.recipe.Ingredient

object BrewingRecipeRegistryExtract {
    class IngredientResult(val input: Potion, val item: Ingredient, val output: Potion)

    class ItemResult(val input: Potion, val item: Item, val output: Potion)

    private val extractedPotionRecipesIngredient = mutableListOf<IngredientResult>()
    private val extractedPotionRecipesItem = mutableListOf<ItemResult>()

    @JvmStatic
    fun addPotionRecipe(input: Potion, item: Ingredient, output: Potion) {
        extractedPotionRecipesIngredient.add(IngredientResult(input, item, output))
    }

    @JvmStatic
    fun addPotionRecipe(input: Potion, item: Item, output: Potion) {
        extractedPotionRecipesItem.add(ItemResult(input, item, output))
    }

    fun getExtractedRecipesIngredient(): List<IngredientResult> {
        return extractedPotionRecipesIngredient
    }

    fun getExtractedRecipesItem(): List<ItemResult> {
        return extractedPotionRecipesItem
    }
}
