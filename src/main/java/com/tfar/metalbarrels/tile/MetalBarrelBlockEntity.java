package com.tfar.metalbarrels.tile;

import com.tfar.metalbarrels.container.MetalBarrelContainer;
import com.tfar.metalbarrels.init.ModBlockEntities;
import com.tfar.metalbarrels.init.ModMenuTypes;
import com.tfar.metalbarrels.util.MetalBarrelBlockEntityType;
import com.tfar.metalbarrels.block.MetalBarrelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.data.models.blockstates.PropertyDispatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MetalBarrelBlockEntity extends RandomizableContainerBlockEntity {

  private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
  private ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
    protected void onOpen(Level p_155062_, BlockPos p_155063_, BlockState p_155064_) {
      MetalBarrelBlockEntity.this.playSound(p_155064_, SoundEvents.BARREL_OPEN);
      MetalBarrelBlockEntity.this.updateBlockState(p_155064_, true);
    }

    protected void onClose(Level p_155072_, BlockPos p_155073_, BlockState p_155074_) {
      MetalBarrelBlockEntity.this.playSound(p_155074_, SoundEvents.BARREL_CLOSE);
      MetalBarrelBlockEntity.this.updateBlockState(p_155074_, false);
    }

    protected void openerCountChanged(Level p_155066_, BlockPos p_155067_, BlockState p_155068_, int p_155069_, int p_155070_) {
    }

    protected boolean isOwnContainer(Player p_155060_) {
      if (p_155060_.containerMenu instanceof MetalBarrelContainer) {
        Container container = ((MetalBarrelContainer)p_155060_.containerMenu).getContainer();
        return container == MetalBarrelBlockEntity.this;
      } else {
        return false;
      }
    }
  };

  public MetalBarrelBlockEntity(BlockPos p_155052_, BlockState p_155053_) {
    super(ModBlockEntities.COPPER_BARREL.get(), p_155052_, p_155053_);
  }

  @Override
  protected void saveAdditional(CompoundTag p_187459_) {
    super.saveAdditional(p_187459_);
    if (!this.trySaveLootTable(p_187459_)) {
      ContainerHelper.saveAllItems(p_187459_, this.items);
    }

  }

  @Override
  public void load(CompoundTag p_155055_) {
    super.load(p_155055_);
    this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
    if (!this.tryLoadLootTable(p_155055_)) {
      ContainerHelper.loadAllItems(p_155055_, this.items);
    }

  }

  @Override
  public int getContainerSize() {
    return 27;
  }

  protected NonNullList<ItemStack> getItems() {
    return this.items;
  }

  protected void setItems(NonNullList<ItemStack> p_58610_) {
    this.items = p_58610_;
  }

  protected Component getDefaultName() {
    return Component.translatable("container.barrel");
  }

  @Override
  protected AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory) {
    //return ChestMenu.threeRows(id, inventory, this);
    return MetalBarrelContainer.copper(id, inventory);
  }

  @Override
  public void startOpen(Player p_58616_) {
    if (!this.remove && !p_58616_.isSpectator()) {
      this.openersCounter.incrementOpeners(p_58616_, this.getLevel(), this.getBlockPos(), this.getBlockState());
    }

  }

  @Override
  public void stopOpen(Player p_58614_) {
    if (!this.remove && !p_58614_.isSpectator()) {
      this.openersCounter.decrementOpeners(p_58614_, this.getLevel(), this.getBlockPos(), this.getBlockState());
    }

  }

  public void recheckOpen() {
    if (!this.remove) {
      this.openersCounter.recheckOpeners(this.getLevel(), this.getBlockPos(), this.getBlockState());
    }

  }

  void updateBlockState(BlockState p_58607_, boolean p_58608_) {
    this.level.setBlock(this.getBlockPos(), p_58607_.setValue(BarrelBlock.OPEN, Boolean.valueOf(p_58608_)), 3);
  }

  void playSound(BlockState p_58601_, SoundEvent p_58602_) {
    Vec3i vec3i = p_58601_.getValue(BarrelBlock.FACING).getNormal();
    double d0 = (double)this.worldPosition.getX() + 0.5D + (double)vec3i.getX() / 2.0D;
    double d1 = (double)this.worldPosition.getY() + 0.5D + (double)vec3i.getY() / 2.0D;
    double d2 = (double)this.worldPosition.getZ() + 0.5D + (double)vec3i.getZ() / 2.0D;
    this.level.playSound((Player)null, d0, d1, d2, p_58602_, SoundSource.BLOCKS, 0.5F, this.level.random.nextFloat() * 0.1F + 0.9F);
  }
}