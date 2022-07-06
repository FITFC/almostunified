package com.almostreliable.unified;

import com.almostreliable.unified.recipe.RecipeDumper;
import com.almostreliable.unified.recipe.RecipeTransformationResult;
import com.almostreliable.unified.recipe.RecipeTransformer;
import com.almostreliable.unified.recipe.handler.RecipeHandlerFactory;
import com.almostreliable.unified.utils.ReplacementMap;
import com.almostreliable.unified.utils.TagMap;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AlmostUnifiedRuntime {

    protected final ModConfig config;
    protected final RecipeHandlerFactory recipeHandlerFactory;
    @Nullable protected TagManager tagManager;
    protected List<String> modPriorities = new ArrayList<>();

    public AlmostUnifiedRuntime(RecipeHandlerFactory recipeHandlerFactory) {
        this.recipeHandlerFactory = recipeHandlerFactory;
        config = new ModConfig(BuildConfig.MOD_ID);
    }

    public void run(Map<ResourceLocation, JsonElement> recipes) {
        config.load();
        modPriorities = config.getModPriorities();
        onRun();
        TagMap tagMap = createTagMap();
        ReplacementMap replacementMap = new ReplacementMap(tagMap, config.getAllowedTags(), modPriorities);
        RecipeTransformer transformer = new RecipeTransformer(recipeHandlerFactory, replacementMap);
        RecipeTransformationResult result = transformer.transformRecipes(recipes);
        new RecipeDumper(result).dump();
    }

    public void updateTagManager(TagManager tagManager) {
        this.tagManager = tagManager;
    }

    protected TagMap createTagMap() {
        if (tagManager == null) {
            throw new IllegalStateException("Internal error. TagManager was not updated correctly");
        }

        return TagMap.create(tagManager);
    }

    protected abstract void onRun();
}
