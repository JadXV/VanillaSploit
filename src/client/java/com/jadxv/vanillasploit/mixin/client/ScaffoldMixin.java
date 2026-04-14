package com.jadxv.vanillasploit.mixin.client;

import com.jadxv.vanillasploit.ModuleManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
@SuppressWarnings("deprecation")
public abstract class ScaffoldMixin {

    // Cancel sneaking pose when scaffold is enabled
    @Inject(method = "isSneaking", at = @At("HEAD"), cancellable = true)
    private void onIsSneaking(CallbackInfoReturnable<Boolean> cir) {
        ModuleManager manager = ModuleManager.getInstance();
        if (manager.scaffoldEnabled) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        ModuleManager manager = ModuleManager.getInstance();
        if (!manager.scaffoldEnabled) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        
        if (mc.interactionManager == null || mc.world == null) return;

        // Check if sneak key is pressed (not isSneaking which we cancel)
        boolean scaffoldDown = mc.options.sneakKey.isPressed();
        
        BlockPos targetPos;
        if (scaffoldDown) {
            // Scaffold down: place block at the side of the block we're standing on, one block lower
            targetPos = new BlockPos(
                (int) Math.floor(player.getX()),
                (int) Math.floor(player.getY() - 1.5),
                (int) Math.floor(player.getZ())
            );
        } else {
            // Normal scaffold: place block at player's feet level
            targetPos = new BlockPos(
                (int) Math.floor(player.getX()),
                (int) Math.floor(player.getY() - 0.5),
                (int) Math.floor(player.getZ())
            );
        }
        
        // Check if there's air at target
        BlockState stateBelow = mc.world.getBlockState(targetPos);
        if (!stateBelow.isAir() && !stateBelow.isLiquid()) return;
        
        // Find a block item in hotbar
        int blockSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                blockSlot = i;
                break;
            }
        }
        
        if (blockSlot == -1) return; // No blocks in hotbar
        
        // Switch to block slot if needed
        int previousSlot = player.getInventory().getSelectedSlot();
        if (previousSlot != blockSlot) {
            player.getInventory().setSelectedSlot(blockSlot);
        }
        
        // Try to place at target position
        if (tryPlace(mc, player, targetPos)) return;
        
        // Also try one block in each horizontal direction for diagonal movement
        BlockPos[] extraPositions = {
            targetPos.north(),
            targetPos.south(),
            targetPos.east(),
            targetPos.west(),
            targetPos.north().east(),
            targetPos.north().west(),
            targetPos.south().east(),
            targetPos.south().west()
        };
        
        for (BlockPos pos : extraPositions) {
            BlockState state = mc.world.getBlockState(pos);
            if (state.isAir() || state.isLiquid()) {
                if (tryPlace(mc, player, pos)) return;
            }
        }
    }
    
    private boolean tryPlace(MinecraftClient mc, ClientPlayerEntity player, BlockPos target) {
        // All possible adjacent positions and their placement directions
        BlockPos[] adjacentPositions = {
            target.down(),
            target.north(),
            target.south(),
            target.east(),
            target.west(),
            target.up(),
            // Diagonals
            target.down().north(),
            target.down().south(),
            target.down().east(),
            target.down().west(),
        };
        
        Direction[] placementDirections = {
            Direction.UP,
            Direction.SOUTH,
            Direction.NORTH,
            Direction.WEST,
            Direction.EAST,
            Direction.DOWN,
            // For diagonals, we place on top
            Direction.UP,
            Direction.UP,
            Direction.UP,
            Direction.UP,
        };
        
        for (int i = 0; i < adjacentPositions.length; i++) {
            BlockPos adjacent = adjacentPositions[i];
            BlockState adjacentState = mc.world.getBlockState(adjacent);
            
            if (!adjacentState.isAir() && !adjacentState.isLiquid() && adjacentState.isSolidBlock(mc.world, adjacent)) {
                Direction dir = placementDirections[i];
                Vec3d hitVec = Vec3d.ofCenter(adjacent).add(
                    dir.getOffsetX() * 0.5,
                    dir.getOffsetY() * 0.5,
                    dir.getOffsetZ() * 0.5
                );
                BlockHitResult hitResult = new BlockHitResult(hitVec, dir, adjacent, false);
                
                mc.interactionManager.interactBlock(player, Hand.MAIN_HAND, hitResult);
                player.swingHand(Hand.MAIN_HAND);
                return true;
            }
        }
        return false;
    }
}
