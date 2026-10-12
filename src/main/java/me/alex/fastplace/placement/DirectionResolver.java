package me.alex.fastplace.placement;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class DirectionResolver {
    public BlockFace resolve(Player player, PlacementRequest request, PlacementBounds bounds) {
        if (!request.template().groundRow()) {
            return request.direction();
        }
        Vector look = player.getLocation().getDirection();
        BlockFace preferred = Math.abs(look.getX()) > Math.abs(look.getZ())
                ? (look.getX() >= 0 ? BlockFace.EAST : BlockFace.WEST)
                : (look.getZ() >= 0 ? BlockFace.SOUTH : BlockFace.NORTH);
        if (available(request, preferred, bounds)) {
            return preferred;
        }
        BlockFace alternate = preferred.getModX() != 0
                ? (look.getZ() >= 0 ? BlockFace.SOUTH : BlockFace.NORTH)
                : (look.getX() >= 0 ? BlockFace.EAST : BlockFace.WEST);
        for (BlockFace face : new BlockFace[]{alternate, alternate.getOppositeFace(), preferred.getOppositeFace()}) {
            if (available(request, face, bounds)) {
                return face;
            }
        }
        return preferred;
    }

    private boolean available(PlacementRequest request, BlockFace direction, PlacementBounds bounds) {
        if (bounds.clamp(request, direction) == 0) {
            return false;
        }
        for (PlacementTemplate.Part part : request.template().parts()) {
            if (!request.loaded(direction, 1, part) || !PlacementTransaction.replaceable(request.block(direction, 1, part))) {
                return false;
            }
        }
        PlacementTemplate.Part base = request.template().lowestPart();
        Block block = request.block(direction, 1, base);
        Material previous = block.getType();
        return block.canPlace(base.data(previous));
    }
}
