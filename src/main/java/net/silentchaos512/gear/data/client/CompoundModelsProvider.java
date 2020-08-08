package net.silentchaos512.gear.data.client;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.client.model.generators.ExistingFileHelper;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.ModelProvider;
import net.silentchaos512.gear.SilentGear;
import net.silentchaos512.gear.api.item.GearType;
import net.silentchaos512.gear.api.item.ICoreItem;
import net.silentchaos512.gear.api.parts.PartType;
import net.silentchaos512.gear.init.ModItems;
import net.silentchaos512.gear.init.Registration;
import net.silentchaos512.gear.item.CompoundPartItem;
import net.silentchaos512.gear.item.FragmentItem;
import net.silentchaos512.gear.util.Const;
import net.silentchaos512.lib.util.NameUtils;

public class CompoundModelsProvider extends ModelProvider<ItemModelBuilder> {
    public CompoundModelsProvider(DataGenerator generator, ExistingFileHelper existingFileHelper) {
        super(generator, SilentGear.MOD_ID, ITEM_FOLDER, CompoundModelBuilder::new, existingFileHelper);
    }

    @Override
    public String getName() {
        return "Silent Gear - Compound Item Models";
    }

    @Override
    protected void registerModels() {
        ModelFile itemGenerated = getExistingFile(mcLoc("item/generated"));
        ModelFile itemHandheld = getExistingFile(mcLoc("item/handheld"));

        Registration.getItems(CompoundPartItem.class).forEach(item ->
                partBuilder(item).parent(itemGenerated));

        fragmentBuilder(ModItems.FRAGMENT.get()).parent(itemGenerated);

        // FIXME
//        gearBuilder(ModItems.SWORD.get()).parent(itemHandheld);
//        gearBuilder(ModItems.DAGGER.get()).parent(itemHandheld);
//        gearBuilder(ModItems.KATANA.get()).parent(itemHandheld);
//        gearBuilder(ModItems.MACHETE.get()).parent(itemHandheld);
//        gearBuilder(ModItems.SPEAR.get()).parent(itemHandheld);
//        gearBuilder(ModItems.PICKAXE.get()).parent(itemHandheld);
//        gearBuilder(ModItems.SHOVEL.get()).parent(itemHandheld);
//        gearBuilder(ModItems.AXE.get()).parent(itemHandheld);
//        gearBuilder(ModItems.PAXEL.get()).parent(itemHandheld);
//        gearBuilder(ModItems.HAMMER.get()).parent(itemHandheld);
//        gearBuilder(ModItems.EXCAVATOR.get()).parent(itemHandheld);
//        gearBuilder(ModItems.LUMBER_AXE.get()).parent(itemHandheld);
//        gearBuilder(ModItems.MATTOCK.get()).parent(itemHandheld);
//        gearBuilder(ModItems.SICKLE.get()).parent(itemHandheld);
//        gearBuilder(ModItems.SHEARS.get()).parent(itemHandheld);
//        gearBuilder(ModItems.BOW.get()).parent(getExistingFile(mcLoc("item/bow")));
//        gearBuilder(ModItems.CROSSBOW.get()).parent(getExistingFile(mcLoc("item/crossbow")));
//        gearBuilder(ModItems.SLINGSHOT.get()).parent(itemHandheld);
////        gearBuilder(ModItems.SHIELD.get()).parent(itemHandheld);
//        gearBuilder(ModItems.HELMET.get()).parent(itemHandheld);
//        gearBuilder(ModItems.CHESTPLATE.get()).parent(itemHandheld);
//        gearBuilder(ModItems.LEGGINGS.get()).parent(itemHandheld);
//        gearBuilder(ModItems.BOOTS.get()).parent(itemHandheld);
    }

    protected CompoundModelBuilder gearBuilder(ICoreItem item) {
        return ((CompoundModelBuilder) getBuilder(NameUtils.from(item.asItem()).getPath()))
                .setLoader(Const.GEAR_MODEL_LOADER)
                .setGearType(item.getGearType());
    }

    protected CompoundModelBuilder partBuilder(CompoundPartItem item) {
        return ((CompoundModelBuilder) getBuilder(NameUtils.from(item).getPath()))
                .setLoader(Const.COMPOUND_PART_MODEL_LOADER)
                .setGearType(item.getGearType())
                .setPartType(item.getPartType());
    }

    private CompoundModelBuilder fragmentBuilder(FragmentItem item) {
        return ((CompoundModelBuilder) getBuilder(NameUtils.from(item).getPath()))
                .setLoader(Const.FRAGMENT_MODEL_LOADER)
                .setGearType(GearType.FRAGMENT)
                .setPartType(PartType.MAIN);
    }
}
