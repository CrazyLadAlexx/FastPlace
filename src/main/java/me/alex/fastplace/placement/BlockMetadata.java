package me.alex.fastplace.placement;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Nameable;
import org.bukkit.Material;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public final class BlockMetadata {
    private static final BlockMetadata EMPTY = new BlockMetadata(null);
    private final BlockState source;

    private BlockMetadata(BlockState source) {
        this.source = source;
    }

    public static BlockMetadata capture(ItemStack item, BlockData data) {
        if (!item.hasItemMeta()) {
            return EMPTY;
        }
        ItemMeta meta = item.getItemMeta();
        boolean stored = meta instanceof BlockStateMeta stateMeta && stateMeta.hasBlockState();
        BlockState state = stored ? ((BlockStateMeta) meta).getBlockState() : data.createBlockState();
        if (!(state instanceof TileState)) {
            return EMPTY;
        }
        boolean changed = stored;
        if (meta instanceof SkullMeta skullMeta && state instanceof Skull skull) {
            PlayerProfile profile = skullMeta.getPlayerProfile();
            if (profile != null) {
                skull.setPlayerProfile(profile);
                changed = true;
            }
            if (skullMeta.getNoteBlockSound() != null) {
                skull.setNoteBlockSound(skullMeta.getNoteBlockSound());
                changed = true;
            }
        }
        if (meta instanceof BannerMeta bannerMeta && state instanceof Banner banner) {
            banner.setPatterns(bannerMeta.getPatterns());
            changed = true;
        }
        if (meta.hasDisplayName() && state instanceof Nameable nameable) {
            nameable.customName(meta.displayName());
            changed = true;
        }
        return changed ? new BlockMetadata(state.copy()) : EMPTY;
    }

    public void place(Block block, BlockData data) {
        boolean physics = data.getMaterial() != Material.TNT;
        if (source == null) {
            block.setBlockData(data, physics);
            return;
        }
        BlockState state = source.copy(block.getLocation());
        state.setBlockData(data);
        state.update(true, physics);
    }
}
