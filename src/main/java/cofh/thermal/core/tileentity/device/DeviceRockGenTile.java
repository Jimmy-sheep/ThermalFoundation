package cofh.thermal.core.tileentity.device;

import cofh.core.network.packet.client.TileStatePacket;
import cofh.lib.inventory.ItemStorageCoFH;
import cofh.lib.tileentity.ICoFHTickableTile;
import cofh.lib.util.helpers.AugmentDataHelper;
import cofh.thermal.core.inventory.container.device.DeviceRockGenContainer;
import cofh.thermal.core.util.managers.device.RockGenManager;
import cofh.thermal.lib.tileentity.DeviceTileBase;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static cofh.core.client.renderer.model.ModelUtils.FLUID;
import static cofh.lib.util.StorageGroup.OUTPUT;
import static cofh.lib.util.constants.Constants.BUCKET_VOLUME;
import static cofh.lib.util.constants.NBTTags.*;
import static cofh.lib.util.helpers.ItemHelper.itemsEqualWithTags;
import static cofh.thermal.core.init.TCoreReferences.DEVICE_ROCK_GEN_TILE;
import static cofh.thermal.lib.common.ThermalAugmentRules.createAllowValidator;
import static cofh.thermal.lib.common.ThermalConfig.deviceAugments;

public class DeviceRockGenTile extends DeviceTileBase implements ICoFHTickableTile.IServerTickable {

    public static final BiPredicate<ItemStack, List<ItemStack>> AUG_VALIDATOR = createAllowValidator(TAG_AUGMENT_TYPE_UPGRADE);

    protected static final Supplier<ItemStack> COBBLESTONE = () -> new ItemStack(Blocks.COBBLESTONE, 0);

    protected ItemStorageCoFH outputSlot = new ItemStorageCoFH(e -> false).setEmptyItem(COBBLESTONE).setEnabled(() -> isActive);

    protected Block below = Blocks.AIR;
    protected Block adjacent = Blocks.AIR;
    protected int adjLava = 0;

    protected boolean cached;
    protected boolean valid;

    protected int process;
    protected int processMax = RockGenManager.instance().getDefaultEnergy();
    protected int genAmount = 1;

    public DeviceRockGenTile(BlockPos pos, BlockState state) {

        super(DEVICE_ROCK_GEN_TILE, pos, state);

        inventory.addSlot(outputSlot, OUTPUT);

        addAugmentSlots(deviceAugments);
        initHandlers();

        renderFluid = new FluidStack(Fluids.LAVA, BUCKET_VOLUME);
    }

    public Block getBelow() {

        return below;
    }

    public Block getAdjacent() {

        return adjacent;
    }

    public int getAdjLava() {

        return adjLava;
    }

    @Override
    protected void updateValidity() {

        if (level == null || !level.isAreaLoaded(worldPosition, 1)) {
            return;
        }
        adjLava = 0;
        valid = false;

        Block[] adjBlocks = new Block[4];
        BlockPos[] cardinals = new BlockPos[]{
                worldPosition.north(),
                worldPosition.south(),
                worldPosition.west(),
                worldPosition.east(),
        };
        for (int i = 0; i < 4; ++i) {
            BlockPos adj = cardinals[i];
            FluidState fluidState = level.getFluidState(adj);
            if (fluidState.getType().equals(Fluids.LAVA)) {
                ++adjLava;
            }
            adjBlocks[i] = fluidState.isEmpty() || fluidState.isSource() ? level.getBlockState(adj).getBlock() : Blocks.AIR;
        }
        if (adjLava > 0) {
            Block under = level.getBlockState(worldPosition.below()).getBlock();
            RockGenManager.RockGenRecipe recipe = RockGenManager.instance().getResult(under, adjBlocks);
            ItemStack result = recipe.getResult();
            if (!result.isEmpty()) {
                outputSlot.setEmptyItem(() -> new ItemStack(result.getItem(), 0));

                if (!outputSlot.isEmpty() && !itemsEqualWithTags(result, outputSlot.getItemStack())) {
                    outputSlot.clear();
                }
                Block prevBelow = below;
                Block prevAdj = adjacent;

                below = recipe.getBelow();
                adjacent = recipe.getAdjacent();

                if (below != prevBelow || adjacent != prevAdj) {
                    TileStatePacket.sendToClient(this);
                }
                processMax = recipe.getTime();
                genAmount = Math.max(1, result.getCount());
                if (level.getBiome(worldPosition).value().getBiomeCategory() == Biome.BiomeCategory.NETHER) {
                    processMax = Math.max(1, processMax / 2);
                }
                process = processMax;
                valid = true;
            }
        }
        cached = true;
    }

    @Override
    protected void updateActiveState() {

        if (!cached) {
            updateValidity();
        }
        super.updateActiveState();
    }

    @Override
    protected boolean isValid() {

        return valid;
    }

    @Override
    public void tickServer() {

        updateActiveState();

        if (!isActive) {
            return;
        }
        --process;
        if (process > 0) {
            return;
        }
        process = processMax;
        outputSlot.modify((int) (genAmount * baseMod));
    }

    @Nonnull
    @Override
    public IModelData getModelData() {

        return new ModelDataMap.Builder()
                .withInitial(FLUID, renderFluid)
                .build();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {

        return new DeviceRockGenContainer(i, level, worldPosition, inventory, player);
    }

    // region GUI
    @Override
    public int getScaledProgress(int scale) {

        if (!isActive || processMax <= 0 || outputSlot.isFull()) {
            return 0;
        }
        return scale * (processMax - process) / processMax;
    }
    // endregion

    // region NETWORK
    @Override
    public FriendlyByteBuf getGuiPacket(FriendlyByteBuf buffer) {

        super.getGuiPacket(buffer);

        buffer.writeInt(process);
        buffer.writeInt(processMax);

        return buffer;
    }

    @Override
    public void handleGuiPacket(FriendlyByteBuf buffer) {

        super.handleGuiPacket(buffer);

        process = buffer.readInt();
        processMax = buffer.readInt();
    }

    // STATE
    @Override
    public FriendlyByteBuf getStatePacket(FriendlyByteBuf buffer) {

        super.getStatePacket(buffer);

        buffer.writeInt(process);
        buffer.writeInt(adjLava);

        buffer.writeUtf(ForgeRegistries.BLOCKS.getKey(below).toString());
        buffer.writeUtf(ForgeRegistries.BLOCKS.getKey(adjacent).toString());

        return buffer;
    }

    @Override
    public void handleStatePacket(FriendlyByteBuf buffer) {

        super.handleStatePacket(buffer);

        process = buffer.readInt();
        adjLava = buffer.readInt();

        below = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(buffer.readUtf()));
        adjacent = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(buffer.readUtf()));
    }
    // endregion

    // region NBT
    @Override
    public void load(CompoundTag nbt) {

        super.load(nbt);

        process = nbt.getInt(TAG_PROCESS);
        processMax = nbt.getInt(TAG_PROCESS_MAX);
        adjLava = nbt.getInt("Lava");

        below = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(nbt.getString("Below")));
        adjacent = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(nbt.getString("Adjacent")));
    }

    @Override
    public void saveAdditional(CompoundTag nbt) {

        super.saveAdditional(nbt);

        nbt.putInt(TAG_PROCESS, process);
        nbt.putInt(TAG_PROCESS_MAX, processMax);
        nbt.putInt("Lava", adjLava);

        nbt.putString("Below", ForgeRegistries.BLOCKS.getKey(below).toString());
        nbt.putString("Adjacent", ForgeRegistries.BLOCKS.getKey(adjacent).toString());
    }
    // endregion

    // region AUGMENTS
    @Override
    protected Predicate<ItemStack> augValidator() {

        return item -> AugmentDataHelper.hasAugmentData(item) && AUG_VALIDATOR.test(item, getAugmentsAsList());
    }
    // endregion
}
