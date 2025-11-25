package com.dtbcds.stacktonearbychests;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.InteractionResult;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static com.dtbcds.stacktonearbychests.MathUtil.getBlocksInBox;
import static com.dtbcds.stacktonearbychests.MathUtil.getBox;

public class ForEachBlockContainerTask extends ForEachContainerTask {

    private final ClientLevel world;
    private final MultiPlayerGameMode interactionManager;
    private final float squaredReachDistance;
    private final Entity cameraEntity;

    private final Collection<String> filter;

    private final Iterator<BlockPos> blocks;
    private final Set<BlockPos> searchedBlocks = new HashSet<>();
    private boolean hasSearchedEnderChest = false;

    public ForEachBlockContainerTask(Minecraft client,
            Entity cameraEntity,
            ClientLevel world,
            LocalPlayer player,
            MultiPlayerGameMode interactionManager,
            Consumer<AbstractContainerMenu> action,
            Collection<String> filter) {
        super(client, player, action);
        this.world = world;
        this.interactionManager = interactionManager;
        float reachDistance = interactionManager.getPickRange();
        this.squaredReachDistance = Mth.square(reachDistance);
        this.cameraEntity = cameraEntity;
        this.blocks = getBlocksInBox(getBox(cameraEntity.getEyePosition(0), reachDistance))
                .iterator();
        this.filter = filter;
    }

    @Override
    protected boolean findAndOpenNextContainer() {
        while (blocks.hasNext()) {
            BlockPos pos = blocks.next().immutable();

            if (searchedBlocks.contains(pos)) {
                continue;
            }
            if (!isOpenable(world, pos)) {
                continue;
            }
            BlockState state = world.getBlockState(pos);
            if (!filter.contains(ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString())) {
                continue;
            }

            Vec3 origin = cameraEntity.getEyePosition(0);
            Vec3 closestPos = MathUtil.getClosestPoint(pos, state.getShape(world, pos), origin);
            if (closestPos.distanceToSqr(origin) > squaredReachDistance) {
                continue;
            }

            searchedBlocks.add(pos);
            if (state.getBlock() == Blocks.ENDER_CHEST) {
                if (hasSearchedEnderChest) {
                    continue;
                }
                hasSearchedEnderChest = true;
            } else if (state.getBlock() == Blocks.SHULKER_BOX) {
                Direction facing = state.getValue(ShulkerBoxBlock.FACING);
                BlockPos facingBlockPos = pos.relative(facing);
                for (var dir : Direction.values()) {
                    if (dir == facing || dir == facing.getOpposite()) {
                        continue;
                    }

                    BlockPos adjacentBlockPos = facingBlockPos.relative(dir);
                    BlockState adjacentBlockState = world.getBlockState(adjacentBlockPos);
                    if (adjacentBlockState.getBlock() == Blocks.SHULKER_BOX
                            && adjacentBlockState.getValue(ShulkerBoxBlock.FACING) == dir.getOpposite()) {
                        searchedBlocks.add(adjacentBlockPos);
                    }
                }
            } else {
                getTheOtherHalfOfLargeChest(world, pos).ifPresent(searchedBlocks::add);
            }

            var hitResult = new BlockHitResult(closestPos,
                    MathUtil.getFacingDirection(closestPos.subtract(origin)).getOpposite(), pos, false);
            InteractionResult result = interactionManager.useItemOn(player, InteractionHand.MAIN_HAND, hitResult);

            if (result == InteractionResult.FAIL || result == InteractionResult.PASS) {
                continue;
            }

            return true;
        }

        return false;
    }

    private boolean isOpenable(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        BlockEntity blockEntity = world.getBlockEntity(pos);
        // Removed SophisticatedStorageCompat check
        if (!(blockEntity instanceof net.minecraft.world.Container) // Inventory -> Container
                && block != Blocks.ENDER_CHEST) {
            return false;
        }
        if (blockEntity instanceof ShulkerBoxBlockEntity shulkerBoxBlockEntity
                && isShulkerBoxBlocked(state, world, pos, shulkerBoxBlockEntity)) {
            return false;
        }
        if (state.getBlock() instanceof ChestBlock || state.getBlock() == Blocks.ENDER_CHEST) {
            if (ChestBlock.isChestBlockedAt(world, pos)) {
                return false;
            }
            return getTheOtherHalfOfLargeChest(world, pos)
                    .map(offsetPos -> !ChestBlock.isChestBlockedAt(world, offsetPos))
                    .orElse(true);
        }
        return true;
    }

    private boolean isShulkerBoxBlocked(BlockState state, Level world, BlockPos pos,
            ShulkerBoxBlockEntity shulkerBoxBlockEntity) {
        if (shulkerBoxBlockEntity.getAnimationStatus() != ShulkerBoxBlockEntity.AnimationStatus.CLOSED) {
            return false;
        }

        // Shulker.getPeekAabb doesn't exist in Forge, calculate the peek box manually
        Direction facing = state.getValue(ShulkerBoxBlock.FACING);
        float f = 0.5f * shulkerBoxBlockEntity.getProgress(1.0f);
        AABB box = AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(pos).relative(facing, 0.5 - 0.5 * f))
                .inflate(0.5 * f, 0.5 * f, 0.5 * f)
                .deflate(1.0E-6);
        return !world.noCollision(box);
    }

    private Optional<BlockPos> getTheOtherHalfOfLargeChest(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            // getConnectedDirection returns the direction in which the other half of the
            // chest is located
            BlockPos offsetPos = pos.relative(ChestBlock.getConnectedDirection(state));
            BlockState theOtherHalf = world.getBlockState(offsetPos);
            if (theOtherHalf.getBlock() == state.getBlock()
                    && state.getValue(ChestBlock.FACING) == theOtherHalf.getValue(ChestBlock.FACING)
                    && ChestBlock.getConnectedDirection(state) == ChestBlock.getConnectedDirection(theOtherHalf)
                            .getOpposite()) {
                return Optional.ofNullable(offsetPos);
            }
        }
        return Optional.empty();
    }

}
