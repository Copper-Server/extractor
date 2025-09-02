package copper_server.extractor.mixin;

import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.entry.RegistryEntry;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import copper_server.extractor.bridges.BrewingRecipeRegistryExtract;

@Mixin(BrewingRecipeRegistry.Builder.class)
public abstract class BrewingRecipeRegistryMixin {
    @Inject(method = "registerPotionRecipe", at = @At("HEAD"))
    private void potionextractor_onRegisterPotionRecipe(RegistryEntry<Potion> input, Item item, RegistryEntry<Potion> output, CallbackInfo ci) {
        BrewingRecipeRegistryExtract.addPotionRecipe(input.value(), item, output.value());
    }
}