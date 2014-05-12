package ganymedes01.aobd.recipes;

import ganymedes01.aobd.AOBD;
import ganymedes01.aobd.ore.Ore;
import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.RecipeInputOreDict;
import ic2.api.recipe.Recipes;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import cpw.mods.fml.common.registry.GameRegistry;

public class RecipesHandler {

	public static void init() {
		craftingRecipes();
		if (AOBD.enableIC2)
			IC2Recipes();
		if (AOBD.enableRailcraft)
			RailcraftRecipes();
		if (AOBD.enableMekanism)
			MekanismRecipes();
		if (AOBD.enableEnderIO)
			EnderIORecipes();
	}

	private static void EnderIORecipes() {
		for (Ore ore : Ore.ores)
			if (ore.shouldEnderIO())
				addSAGMillRecipe("ore" + ore.name(), (float) ore.energy(360.0), new ItemStack[] { getOreDictItem("dust" + ore.name(), 2), getOreDictItem("dust" + ore.extra()), new ItemStack(Blocks.cobblestone) }, new float[] { 1.0F, 0.2F, 0.15F });
	}

	private static void MekanismRecipes() {
		for (Ore ore : Ore.ores)
			if (ore.shouldMeka()) {
				//				for (ItemStack ore : OreDictionary.getOres("ore" + metal.name()))
				//					RecipeHelper.addPurificationChamberRecipe(ore, DustsItem.getItem("clump" + metal.name(), 3));
				//
				//				RecipeHelper.addCrusherRecipe(DustsItem.getItem("clump" + metal.name()), DustsItem.getItem("dustDirty" + metal.name()));
				//				if (AOBD.enableIC2)
				//					Recipes.macerator.addRecipe(new RecipeInputOreDict("clump" + metal.name()), null, DustsItem.getItem("dustDirty" + metal.name()));
				//				RecipeHelper.addEnrichmentChamberRecipe(DustsItem.getItem("dustDirty" + metal.name()), getOreDictItem("dust" + metal.name(), 1));
			}
	}

	private static void RailcraftRecipes() {
		try {
			Class<?> RailcraftCraftingManager = Class.forName("mods.railcraft.api.crafting.RailcraftCraftingManager");
			Object rockCrusher = RailcraftCraftingManager.getDeclaredField("rockCrusher").get(null);
			Method createNewRecipe = rockCrusher.getClass().getMethod("createNewRecipe", ItemStack.class, boolean.class, boolean.class);

			for (Ore ore : Ore.ores)
				if (ore.shouldRC())
					for (ItemStack stack : OreDictionary.getOres("ore" + ore.name())) {
						Object recipe = createNewRecipe.invoke(rockCrusher, stack, true, false);
						recipe.getClass().getMethod("addOutput", ItemStack.class, float.class).invoke(recipe, getOreDictItem("crushed" + ore.name(), 2), 1.0F);
					}
		} catch (Exception e) {
		}
	}

	private static void IC2Recipes() {
		ItemStack stoneDust = getICItem("stoneDust");

		for (Ore ore : Ore.ores)
			if (ore.shouldIC2()) {
				String name = ore.name();
				try {
					Recipes.macerator.addRecipe(new RecipeInputOreDict("ore" + name), null, getOreDictItem("crushed" + name, 2));
					Recipes.macerator.addRecipe(new RecipeInputOreDict("ingot" + name), null, getOreDictItem("dust" + name));

					addCentrifugeRecipe(new RecipeInputOreDict("crushed" + name), (int) ore.energy(1500), getOreDictItem("dust" + name), getOreDictItem("dustTiny" + ore.extra()), stoneDust.copy());
					addOreWashingRecipe(new RecipeInputOreDict("crushed" + name), getOreDictItem("crushedPurified" + name), getOreDictItem("dustTiny" + name, 2), stoneDust.copy());

					addCentrifugeRecipe(new RecipeInputOreDict("crushedPurified" + name), (int) ore.energy(1500), getOreDictItem("dust" + name, 1), getOreDictItem("dustTiny" + ore.extra()));
				} catch (Exception e) {
					continue;
				}
			}
	}

	private static void craftingRecipes() {
		for (Ore ore : Ore.ores) {
			String name = ore.name();
			GameRegistry.addRecipe(new ShapedOreRecipe(getOreDictItem("dust" + name), "xxx", "xxx", "xxx", 'x', "dustTiny" + name));
			GameRegistry.addSmelting(getOreDictItem("crushed" + name), getOreDictItem("ingot" + name), 0.2F);
			GameRegistry.addSmelting(getOreDictItem("crushedPurified" + name), getOreDictItem("ingot" + name), 0.2F);
		}
	}

	private static void addCentrifugeRecipe(IRecipeInput input, int minHeat, ItemStack... output) {
		NBTTagCompound metadata = new NBTTagCompound();
		metadata.setInteger("minHeat", minHeat);

		Recipes.centrifuge.addRecipe(input, metadata, output);
	}

	private static void addOreWashingRecipe(IRecipeInput input, ItemStack... output) {
		NBTTagCompound metadata = new NBTTagCompound();
		metadata.setInteger("amount", 1000);

		Recipes.oreWashing.addRecipe(input, metadata, output);
	}

	private static ItemStack getICItem(String name) {
		try {
			Class<?> itemsClass = Class.forName("ic2.core.Ic2Items");
			return (ItemStack) itemsClass.getField(name).get(null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static ItemStack getOreDictItem(String name) {
		return getOreDictItem(name, 1);
	}

	private static ItemStack getOreDictItem(String name, int size) {
		while (OreDictionary.getOres(name).size() <= 0)
			System.out.println(name);
		ItemStack stack = OreDictionary.getOres(name).get(0).copy();
		stack.stackSize = size;

		return stack;
	}

	@SuppressWarnings("all")
	private static void addSAGMillRecipe(String input, float energy, ItemStack[] outputs, float[] chance) {
		try {
			Object SAGMill = Class.forName("crazypants.enderio.machine.crusher.CrusherRecipeManager").getMethod("getInstance").invoke(null);
			Method addRecipe = SAGMill.getClass().getMethod("addRecipe", Class.forName("crazypants.enderio.machine.recipe.Recipe"));

			Class recipeInput = Class.forName("crazypants.enderio.machine.recipe.RecipeInput");
			Class recipeOuput = Class.forName("crazypants.enderio.machine.recipe.RecipeOutput");
			Constructor oreDictInput = Class.forName("crazypants.enderio.machine.recipe.OreDictionaryRecipeInput").getConstructor(ItemStack.class, int.class, int.class);

			Object[] output = (Object[]) Array.newInstance(Class.forName("crazypants.enderio.machine.recipe.RecipeOutput"), outputs.length);
			Constructor recipe = Class.forName("crazypants.enderio.machine.recipe.Recipe").getConstructor(recipeInput, float.class, output.getClass());

			for (int i = 0; i < outputs.length; i++)
				output[i] = recipeOuput.getConstructor(ItemStack.class, float.class).newInstance(outputs[i], chance[i]);

			addRecipe.invoke(SAGMill, recipe.newInstance(oreDictInput.newInstance(getOreDictItem(input, 1), OreDictionary.getOreID(input), -1), energy, output));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}