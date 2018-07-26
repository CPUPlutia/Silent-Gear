package net.silentchaos512.gear.api.parts;

import com.google.common.collect.Multimap;
import com.google.common.primitives.UnsignedInts;
import com.google.gson.*;
import lombok.AccessLevel;
import lombok.Getter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.silentchaos512.gear.SilentGear;
import net.silentchaos512.gear.api.event.GetStatModifierEvent;
import net.silentchaos512.gear.api.stats.CommonItemStats;
import net.silentchaos512.gear.api.stats.ItemStat;
import net.silentchaos512.gear.api.stats.StatInstance;
import net.silentchaos512.gear.api.stats.StatInstance.Operation;
import net.silentchaos512.gear.api.stats.StatModifierMap;
import net.silentchaos512.gear.config.Config;
import net.silentchaos512.gear.util.GearData;
import net.silentchaos512.gear.util.GearHelper;
import net.silentchaos512.lib.util.StackHelper;

import javax.annotation.Nullable;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

// TODO: javadoc
@Getter(value = AccessLevel.PUBLIC)
public abstract class ItemPart {
    protected ResourceLocation registryName;

    protected static final ResourceLocation BLANK_TEXTURE = new ResourceLocation(SilentGear.MOD_ID, "items/blank");
    private static final Gson GSON = (new GsonBuilder()).create();

    @Getter(value = AccessLevel.NONE)
    protected Supplier<ItemStack> craftingStack = () -> ItemStack.EMPTY;
    protected String craftingOreDictName = "";
    @Getter(value = AccessLevel.NONE)
    protected Supplier<ItemStack> craftingStackSmall = () -> ItemStack.EMPTY;
    protected String craftingOreDictNameSmall = "";
    protected int tier = 0;
    protected boolean enabled = true;
    protected boolean hidden = false;
    protected String textureSuffix;
    protected int textureColor = 0xFFFFFF;
    protected int brokenColor = 0xFFFFFF;
    protected TextFormatting nameColor = TextFormatting.GRAY;
    protected String localizedNameOverride = "";
    private final boolean userDefined;

    /**
     * Numerical index for model caching. This value could change any time the mod updates or new
     * materials are added, so don't use it for persistent data! Also good for identifying subtypes
     * in JEI.
     */
    @Getter(value = AccessLevel.NONE)
    protected int modelIndex;
    private static int lastModelIndex = -1;

    @Getter(value = AccessLevel.NONE)
    protected Multimap<ItemStat, StatInstance> stats = new StatModifierMap();

    public ItemPart(ResourceLocation registryName, boolean userDefined) {
        this.registryName = registryName;
        this.textureSuffix = registryName.getPath().replaceFirst("[a-z]+_", "");
        this.modelIndex = ++lastModelIndex;
        this.userDefined = userDefined;
        loadJsonResources();
    }

    // ===========================
    // = Stats and Miscellaneous =
    // ===========================

    public ItemStack getCraftingStack() {
        return craftingStack.get();
    }

    public ItemStack getCraftingStackSmall() {
        return craftingStackSmall.get();
    }

    public Collection<StatInstance> getStatModifiers(ItemStat stat, ItemPartData part) {
        List<StatInstance> mods = new ArrayList<>(this.stats.get(stat));
        GetStatModifierEvent event = new GetStatModifierEvent(part, stat, mods);
        MinecraftForge.EVENT_BUS.post(event);
        return event.getModifiers();
    }

    /**
     * Default operation to use if the resource file does not specify on operation for the given
     * stat
     */
    public StatInstance.Operation getDefaultStatOperation(ItemStat stat) {
        if (stat == CommonItemStats.HARVEST_LEVEL)
            return StatInstance.Operation.MAX;
        else if (this instanceof PartMain)
            return StatInstance.Operation.AVG;
        else if (stat == CommonItemStats.ATTACK_SPEED || stat == CommonItemStats.RARITY)
            return StatInstance.Operation.ADD;
        else if (this instanceof PartRod)
            return StatInstance.Operation.MUL2;
        else if (this instanceof PartTip)
            return StatInstance.Operation.ADD;

        return StatInstance.Operation.ADD;
    }

    public int getRepairAmount(ItemStack gear, ItemPartData part) {
        // Base value on material durability
        ItemPartData gearPrimary = GearData.getPrimaryPart(gear);
        if (gearPrimary != null && part.part.tier < gearPrimary.part.tier) return 0;
        Collection<StatInstance> mods = getStatModifiers(CommonItemStats.DURABILITY, part);
        return (int) (CommonItemStats.DURABILITY.compute(0f, mods) / 2);
    }

    // ============
    // = Crafting =
    // ============

    public boolean matchesForCrafting(ItemStack partRep, boolean matchOreDict) {
        if (StackHelper.isEmpty(partRep))
            return false;
        if (partRep.isItemEqual(this.craftingStack.get()))
            return true;
        if (matchOreDict)
            return StackHelper.matchesOreDict(partRep, this.craftingOreDictName);
        return false;
    }

    public boolean matchesForDecorating(ItemStack partRep, boolean matchOreDict) {
        if (!craftingOreDictName.isEmpty()) {
            String nuggetName = craftingOreDictName.replaceFirst("gem|ingot", "nugget");
            // TODO matchesForDecorating?
        }
        return matchesForCrafting(partRep, matchOreDict);
    }

    public boolean isBlacklisted() {
        return isBlacklisted(this.craftingStack.get());
    }

    public boolean isBlacklisted(ItemStack partRep) {
        return !this.enabled;
    }

    // ===================================
    // = Display (textures and tooltips) =
    // ===================================

    /**
     * Gets a texture to use based on the item class
     *
     * @param part           The part
     * @param gear           The equipment item (tool/weapon/armor)
     * @param gearClass      The gear class string (pickaxe/sword/etc.)
     * @param animationFrame Animation frame, usually 0
     */
    @Nullable
    public abstract ResourceLocation getTexture(ItemPartData part, ItemStack gear, String gearClass, IPartPosition position, int animationFrame);

    @Nullable
    public abstract ResourceLocation getTexture(ItemPartData part, ItemStack gear, String gearClass, int animationFrame);

    @Nullable
    public ResourceLocation getTexture(ItemPartData part, ItemStack equipment, String gearClass, IPartPosition position) {
        return getTexture(part, equipment, gearClass, position, 0);
    }

    /**
     * Gets a texture to use for a broken item based on the item class
     *
     * @param part      The part
     * @param gear      The equipment item (tool/weapon/armor)
     * @param gearClass The gear class string (pickaxe/sword/etc.)
     */
    @Nullable
    public ResourceLocation getBrokenTexture(ItemPartData part, ItemStack gear, String gearClass, IPartPosition position) {
        return getTexture(part, gear, gearClass, position, 0);
    }

    /**
     * Used for model caching. Be sure to include the animation frame if it matters!
     */
    public String getModelIndex(ItemPartData part, int animationFrame) {
        return this.modelIndex + (animationFrame == 3 ? "_3" : "");
    }

    public int getColor(ItemPartData part, ItemStack gear, int animationFrame) {
        if (!gear.isEmpty() && GearHelper.isBroken(gear))
            return this.brokenColor;
        return this.textureColor;
    }

    /**
     * Adds information to the tooltip instance an equipment item
     *
     * @param part    The data instance the part
     * @param gear    The equipment (tool/weapon/armor) stack
     * @param tooltip Current tooltip lines
     */
    public void addInformation(ItemPartData part, ItemStack gear, World world, List<String> tooltip, boolean advanced) {
    }

    /**
     * Gets a translation key for the part
     */
    public String getTranslationKey(@Nullable ItemPartData part) {
        return "material." + this.registryName.getNamespace() + "." + this.registryName.getPath() + ".name";
    }

    /**
     * Gets a translated name for the part, suitable for display
     *
     * @param part The part
     * @param gear The equipment (tool/weapon/armor) stack
     */
    public String getTranslatedName(ItemPartData part, ItemStack gear) {
        if (!localizedNameOverride.isEmpty())
            return localizedNameOverride;
        return /* nameColor + */ SilentGear.i18n.translate(getTranslationKey(part));
    }

    /**
     * Gets a string that represents the type instance part (main, rod, tip, etc.)
     */
    public abstract String getTypeName();

    @Override
    public String toString() {
        String str = "ItemPart{";
        str += "Key: " + this.registryName + ", ";
        str += "CraftingStack: " + this.craftingStack.get() + ", ";
        str += "CraftingOreDictName: '" + this.craftingOreDictName + "', ";
        str += "Tier: " + getTier();
        str += "}";
        return str;
    }

    // ====================================
    // = Resource file and NBT management =
    // ====================================

    /**
     * Get the location instance the resource file that contains material information
     */
    protected String getResourceFileLocation() {
        return "assets/" + this.registryName.getNamespace() + "/materials/" + this.registryName.getPath() + ".json";
    }

    private void loadJsonResources() {
        // Main resource file in JAR
        String path = getResourceFileLocation();
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(path);
        if (resourceAsStream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream, "UTF-8"))) {
                readResourceFile(reader);
                SilentGear.log.info("Successfully read {}", path);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Override in config folder
        File file = new File(Config.INSTANCE.getDirectory().getPath(), "materials/" + this.registryName.getPath() + ".json");
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            readResourceFile(reader);
            SilentGear.log.info("Successfully read {}", file.getAbsolutePath());
        } catch (FileNotFoundException e) {
            // Ignore
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads material information from a JSON file. Does not handle file IO exceptions.
     */
    private void readResourceFile(BufferedReader reader) {
        JsonElement je = GSON.fromJson(reader, JsonElement.class);
        JsonObject json = je.getAsJsonObject();
        processJson(json);
    }

    /**
     * Process the JSON from a loaded resource file. Override if you need to load extra data.
     *
     * @param json The root JsonObject from the current file
     */
    protected void processJson(JsonObject json) {
        // Read stats
        JsonElement elementStats = json.get("stats");
        if (elementStats.isJsonArray()) {
            JsonArray array = elementStats.getAsJsonArray();
            StatModifierMap statMap = new StatModifierMap();
            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                String name = obj.has("name") ? JsonUtils.getString(obj, "name") : "";
                ItemStat stat = ItemStat.ALL_STATS.get(name);

                if (stat != null) {
                    float value = obj.has("value") ? JsonUtils.getFloat(obj, "value") : 0f;
                    Operation op = obj.has("op") ? Operation.byName(JsonUtils.getString(obj, "op")) : getDefaultStatOperation(stat);
                    String id = "mat_" + this.getTranslationKey(null) + "_" + stat.getUnlocalizedName() + (statMap.get(stat).size() + 1);
                    statMap.put(stat, new StatInstance(id, value, op));
                }
            }

            // Move the newly loaded modifiers into the stat map, replacing existing ones
            statMap.forEach((stat, instance) -> {
                this.stats.removeAll(stat);
                this.stats.put(stat, instance);
            });
        }

        // Read crafting item data
        JsonElement elementCraftingItems = json.get("crafting_items");
        if (elementCraftingItems.isJsonObject()) {
            JsonObject objTop = elementCraftingItems.getAsJsonObject();
            // Normal item (ingot, gem)
            if (objTop.has("normal") && objTop.get("normal").isJsonObject()) {
                JsonObject obj = objTop.get("normal").getAsJsonObject();
                craftingStack = readItemData(obj);
                if (obj.has("oredict"))
                    craftingOreDictName = JsonUtils.getString(obj, "oredict");
            }
            // Small item (nugget, shard)
            if (objTop.has("small") && objTop.get("small").isJsonObject()) {
                JsonObject obj = objTop.get("small").getAsJsonObject();
                craftingStackSmall = readItemData(obj);
                if (obj.has("oredict"))
                    craftingOreDictNameSmall = JsonUtils.getString(obj, "oredict");
            }
        }

        // Display properties
        JsonElement elementDisplay = json.get("display");
        if (elementDisplay.isJsonObject()) {
            JsonObject obj = elementDisplay.getAsJsonObject();
            if (obj.has("hidden"))
                this.hidden = JsonUtils.getBoolean(obj, "hidden");
            if (obj.has("texture_suffix"))
                this.textureSuffix = JsonUtils.getString(obj, "texture_suffix");
            if (obj.has("texture_color"))
                this.textureColor = readColorCode(JsonUtils.getString(obj, "texture_color"));
            if (obj.has("broken_color"))
                this.brokenColor = readColorCode(JsonUtils.getString(obj, "broken_color"));
            if (obj.has("name_color"))
                this.nameColor = TextFormatting.getValueByName(obj.get("name_color").getAsString());
            if (obj.has("override_localization"))
                this.localizedNameOverride = JsonUtils.getString(obj, "override_localization");
        }

        // Availability (enabled, tier, blacklisting)
        JsonElement elementAvailability = json.get("availability");
        if (elementAvailability.isJsonObject()) {
            JsonObject obj = elementAvailability.getAsJsonObject();
            this.enabled = obj.has("enabled") ? JsonUtils.getBoolean(obj, "enabled") : this.enabled;
            this.tier = obj.has("tier") ? JsonUtils.getInt(obj, "tier") : this.tier;
            // TODO: blacklist
        }
    }

    protected int readColorCode(String str) {
        try {
            return UnsignedInts.parseUnsignedInt(str, 16);
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
            return 0xFFFFFF;
        }
    }

    /**
     * Parse ItemStack data from a JSON object
     */
    protected Supplier<ItemStack> readItemData(JsonObject json) {
        if (!json.has("item"))
            return () -> ItemStack.EMPTY;

        final String itemName = JsonUtils.getString(json, "item");
        return () -> {
            Item item = Item.getByNameOrId(itemName);
            if (item == null)
                return ItemStack.EMPTY;
            int meta = json.has("data") ? JsonUtils.getInt(json, "data") : 0;
            return new ItemStack(item, 1, meta);
        };
    }

    public void writeToNBT(NBTTagCompound tags) {
        tags.setString("Key", this.registryName.toString());
    }

    @Nullable
    public static ItemPart fromNBT(NBTTagCompound tags) {
        String key = tags.getString("Key");
        return PartRegistry.get(key);
    }

    public void postInitChecks() {
        if (getCraftingStack().isEmpty())
            SilentGear.log.warn("Part \"{}\"{}has no crafting item.", this.registryName,
                    (this.userDefined ? " (user defined) " : " "));
    }
}
