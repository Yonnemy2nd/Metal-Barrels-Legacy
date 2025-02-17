package me.maxish0t.metalbarrels.common.block.entity;

import me.maxish0t.metalbarrels.common.block.MetalBarrelBlock;
import me.maxish0t.metalbarrels.common.container.MetalBarrelContainer;
import me.maxish0t.metalbarrels.common.init.ModBlockEntities;
import me.maxish0t.metalbarrels.util.ModReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.data.models.blockstates.PropertyDispatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MetalBarrelBlockEntity extends BlockEntity implements MenuProvider, Nameable {

  protected final int width;
  protected final int height;
  protected final PropertyDispatch.TriFunction<Integer, Inventory, ContainerLevelAccess, AbstractContainerMenu> containerFactory;
  protected Component customName;
  protected Component ownerName;
  protected ListTag whitelistedPlayers = new ListTag();
  private boolean isLocked = false;
  public final LazyOptional<IItemHandler> optional;
  public final ItemStackHandler handler;
  public int players = 0;

  public static @NotNull MetalBarrelBlockEntity copper(BlockPos blockPos, BlockState blockState) {
    return new MetalBarrelBlockEntity(ModBlockEntities.COPPER_BARREL.get(), blockPos, blockState,
            9, 5, MetalBarrelContainer::copper);
  }

  public static @NotNull MetalBarrelBlockEntity iron(BlockPos blockPos, BlockState blockState) {
    return new MetalBarrelBlockEntity(ModBlockEntities.IRON_BARREL.get(), blockPos, blockState,
            9, 6, MetalBarrelContainer::iron);
  }

  public static @NotNull MetalBarrelBlockEntity silver(BlockPos blockPos, BlockState blockState) {
    return new MetalBarrelBlockEntity(ModBlockEntities.SILVER_BARREL.get(), blockPos, blockState,
            9, 8, MetalBarrelContainer::silver);
  }

  public static @NotNull MetalBarrelBlockEntity gold(BlockPos blockPos, BlockState blockState) {
    return new MetalBarrelBlockEntity(ModBlockEntities.GOLD_BARREL.get(), blockPos, blockState,
            9, 9, MetalBarrelContainer::gold);
  }

  public static @NotNull MetalBarrelBlockEntity diamond(BlockPos blockPos, BlockState blockState) {
    return new MetalBarrelBlockEntity(ModBlockEntities.DIAMOND_BARREL.get(), blockPos, blockState,
            12, 9, MetalBarrelContainer::diamond);
  }

  public static @NotNull MetalBarrelBlockEntity obsidian(BlockPos blockPos, BlockState blockState) {
    return new MetalBarrelBlockEntity(ModBlockEntities.OBSIDIAN_BARREL.get(), blockPos, blockState,
            12, 9, MetalBarrelContainer::obsidian);
  }

  public static @NotNull MetalBarrelBlockEntity netherite(BlockPos blockPos, BlockState blockState) {
    return new MetalBarrelBlockEntity(ModBlockEntities.NETHERITE_BARREL.get(), blockPos, blockState,
            15, 9, MetalBarrelContainer::netherite);
  }

  public static @NotNull MetalBarrelBlockEntity crystal(BlockPos blockPos, BlockState blockState) {
    return new MetalBarrelBlockEntity(ModBlockEntities.CRYSTAL_BARREL.get(), blockPos, blockState,
            12, 9, MetalBarrelContainer::crystal);
  }

  public MetalBarrelBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState,
                                int width, int height, PropertyDispatch.TriFunction<Integer, Inventory,
          ContainerLevelAccess, AbstractContainerMenu> containerFactory) {
    super(blockEntityType, blockPos, blockState);

    this.width = width;
    this.height = height;
    this.containerFactory = containerFactory;
    handler = new ItemStackHandler(width * height) {
      @Override
      protected void onContentsChanged(int slot) {
        super.onContentsChanged(slot);
        setChanged();
      }
    };
    optional = LazyOptional.of(() -> handler);
  }

  @Override
  public void load(@NotNull CompoundTag tag) {
    super.load(tag);

    CompoundTag invTag = tag.getCompound("inv");
    handler.deserializeNBT(invTag);

    if (tag.contains("CustomName", 8))
      this.customName = Component.Serializer.fromJson(tag.getString("CustomName"));

    if (tag.contains("ownerName"))
      this.ownerName = Component.literal(tag.getString("ownerName"));

    if (tag.contains("barrelLock"))
      this.isLocked = tag.getBoolean("barrelLock");

    if (tag.contains("playerNames"))
      this.whitelistedPlayers = tag.getList("playerNames", Tag.TAG_COMPOUND);
  }

  @Override
  protected void saveAdditional(@NotNull CompoundTag tag) {
    super.saveAdditional(tag);

    CompoundTag compound = this.handler.serializeNBT();
    tag.put("inv", compound);

    if (this.customName != null)
      tag.putString("CustomName", Component.Serializer.toJson(this.customName));

    if (this.ownerName != null)
      tag.putString("ownerName", this.ownerName.getString());

    if (this.whitelistedPlayers != null)
      tag.put("playerNames", this.whitelistedPlayers);
  }

  @Nullable
  @Override
  public Packet<ClientGamePacketListener> getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this, BlockEntity::getUpdateTag);
  }


  @Override
  public @NotNull CompoundTag getUpdateTag() {
    CompoundTag compoundTag = super.getUpdateTag();

    if (this.ownerName != null)
      compoundTag.putString("ownerName", this.ownerName.getString());

    compoundTag.putBoolean("barrelLock", this.isLocked);

    CompoundTag compound = this.handler.serializeNBT();
    compoundTag.put("inv", compound);

    if (this.whitelistedPlayers != null)
      compoundTag.put("playerNames", this.whitelistedPlayers);

    return compoundTag;
  }

  @Override
  public void handleUpdateTag(CompoundTag tag) {
    super.handleUpdateTag(tag);
    this.ownerName = Component.literal(tag.getString("ownerName"));
    this.isLocked = tag.getBoolean("barrelLock");

    CompoundTag invTag = tag.getCompound("inv");
    handler.deserializeNBT(invTag);

    this.whitelistedPlayers = tag.getList("playerNames", Tag.TAG_COMPOUND);
  }

  @Override
  public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
    super.onDataPacket(net, pkt);
    if (pkt.getTag() != null) {
      handleUpdateTag(pkt.getTag());
      load(pkt.getTag());
    }
  }

  @Nullable
  @Override
  public AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory, @NotNull Player player) {
    if (this.level == null)
      return null;
    return containerFactory.apply(id, inventory, ContainerLevelAccess.create(this.level, this.getBlockPos()));
  }

  @Override
  public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
    return cap == ForgeCapabilities.ITEM_HANDLER ? optional.cast() : super.getCapability(cap, side);
  }

  @Override
  public void setRemoved() {
    super.setRemoved();
    optional.invalidate();
  }

  @Override
  public @NotNull Component getName() {
    return this.customName != null ? this.customName : this.getDefaultName();
  }

  @Override
  public @NotNull Component getDisplayName() {
    return this.getName();
  }

  @Nullable
  @Override
  public Component getCustomName() {
    return this.customName;
  }

  protected Component getDefaultName() {
    return Component.literal(getBlockState().getBlock().getName().getString());
  }

  public void setCustomName(Component name) {
    this.customName = name;
  }

  public Component getOwner() {
    return this.ownerName;
  }

  public void setOwner(Component playerUUID) {
    this.ownerName = playerUUID;
  }

  public void changeState(BlockState blockState, boolean face) {
    if (blockState.getBlock() instanceof MetalBarrelBlock) {
      assert this.level != null;
      this.level.setBlock(this.getBlockPos(), blockState.setValue(BarrelBlock.OPEN, face), 3);
    }
  }

  public void soundStuff(BlockState blockState, SoundEvent soundEvent) {
    if (!(blockState.getBlock() instanceof MetalBarrelBlock))
      return;

    Vec3i vec3i = blockState.getValue(BarrelBlock.FACING).getNormal();
    double x = this.getBlockPos().getX() + 0.5D + vec3i.getX() / 2.0D;
    double y = this.getBlockPos().getY() + 0.5D + vec3i.getY() / 2.0D;
    double z = this.getBlockPos().getZ() + 0.5D + vec3i.getZ() / 2.0D;

    if (level != null)
      this.level.playSound(null, x, y, z, soundEvent, SoundSource.BLOCKS, 0.5F, this.level.random.nextFloat() * 0.1F + 0.9F);
  }

  public ItemStackHandler getItemStackHandler() {
    return this.handler;
  }

  public void setLocked(boolean isLocked) {
    this.isLocked = isLocked;
  }

  public boolean getLocked() {
    return this.isLocked;
  }

  public void addWhitelistedPlayer(String username) {
    CompoundTag tag = new CompoundTag();
    tag.putString("name", username);

    boolean hasName = false;
    for (int i = 0; i < this.whitelistedPlayers.size(); i++)
      if (this.whitelistedPlayers.getCompound(i).getString("name").equals(username))
        hasName = true;

    if (!hasName)
      this.whitelistedPlayers.add(tag);
    else
      ModReference.LOGGER.warn("Cannot add the username " + username + " in the barrel whitelist as it is already added!");
  }

  public void removeWhitelistedPlayer(String username) {
    for (int i = 0; i < this.whitelistedPlayers.size(); i++) {
      if (this.whitelistedPlayers.getCompound(i).getString("name").equals(username))
        this.whitelistedPlayers.remove(i);
      else
        ModReference.LOGGER.warn("Cannot find the username " + username + " in the barrel whitelist!");
    }
  }

  public List<String> getWhitelistedPlayers() {
    List<String> playerNames = new ArrayList<>();
    for (int i = 0; i < this.whitelistedPlayers.size(); i++)
      playerNames.add(this.whitelistedPlayers.getCompound(i).getString("name"));
    return playerNames;
  }
}