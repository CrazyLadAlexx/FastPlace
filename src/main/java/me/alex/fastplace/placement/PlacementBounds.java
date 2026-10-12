package me.alex.fastplace.placement;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.BlockFace;

public final class PlacementBounds {
    private final double minX;
    private final double maxX;
    private final double minZ;
    private final double maxZ;
    private final int minY;
    private final int maxY;

    public PlacementBounds(World world) {
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();
        double radius = border.getSize() / 2.0;
        minX = center.getX() - radius;
        maxX = center.getX() + radius;
        minZ = center.getZ() - radius;
        maxZ = center.getZ() + radius;
        minY = world.getMinHeight();
        maxY = world.getMaxHeight();
    }

    public int clamp(PlacementRequest request, BlockFace direction) {
        PlacementTemplate template = request.template();
        if (request.x() + template.minX() + 0.5 < minX || request.x() + template.maxX() + 0.5 >= maxX
                || request.z() + template.minZ() + 0.5 < minZ || request.z() + template.maxZ() + 0.5 >= maxZ
                || request.y() + template.minY() < minY || request.y() + template.maxY() >= maxY) {
            return 0;
        }
        double limit = switch (direction) {
            case EAST -> Math.ceil(maxX - request.x() - template.maxX() - 0.5) - 1;
            case WEST -> Math.floor(request.x() + template.minX() + 0.5 - minX);
            case SOUTH -> Math.ceil(maxZ - request.z() - template.maxZ() - 0.5) - 1;
            case NORTH -> Math.floor(request.z() + template.minZ() + 0.5 - minZ);
            case UP -> maxY - 1 - request.y() - template.maxY();
            case DOWN -> request.y() + template.minY() - minY;
            default -> 0;
        };
        return (int) Math.max(0, Math.min(request.distance(), limit));
    }
}
