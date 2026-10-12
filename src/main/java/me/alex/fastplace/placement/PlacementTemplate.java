package me.alex.fastplace.placement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Candle;
import org.bukkit.block.data.type.SeaPickle;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Snow;
import org.bukkit.block.data.type.TurtleEgg;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;

public final class PlacementTemplate {
    private final ItemStack item;
    private final List<Part> parts;
    private final List<Part> writeOrder;
    private final Part lowestPart;
    private final boolean groundRow;
    private final int minX;
    private final int maxX;
    private final int minY;
    private final int maxY;
    private final int minZ;
    private final int maxZ;

    private PlacementTemplate(ItemStack item, List<Part> parts, boolean groundRow) {
        this.item = item;
        this.parts = List.copyOf(parts);
        this.writeOrder = parts.stream().sorted(Comparator.comparingInt(Part::y).reversed()).toList();
        this.lowestPart = parts.stream().min(Comparator.comparingInt(Part::y)).orElseThrow();
        this.groundRow = groundRow;
        minX = parts.stream().mapToInt(Part::x).min().orElseThrow();
        maxX = parts.stream().mapToInt(Part::x).max().orElseThrow();
        minY = lowestPart.y();
        maxY = parts.stream().mapToInt(Part::y).max().orElseThrow();
        minZ = parts.stream().mapToInt(Part::z).min().orElseThrow();
        maxZ = parts.stream().mapToInt(Part::z).max().orElseThrow();
    }

    public static PlacementTemplate capture(BlockPlaceEvent event) {
        Block origin = event.getBlockPlaced();
        ItemStack item = event.getItemInHand().clone();
        item.setAmount(1);
        List<BlockState> replaced = event instanceof BlockMultiPlaceEvent multiple
                ? multiple.getReplacedBlockStates() : List.of(event.getBlockReplacedState());
        List<Part> parts = new ArrayList<>(replaced.size());
        for (BlockState previous : replaced) {
            Block block = previous.getBlock();
            BlockData data = normalize(block.getBlockData(), previous.getBlockData());
            parts.add(new Part(block.getX() - origin.getX(), block.getY() - origin.getY(),
                    block.getZ() - origin.getZ(), data, BlockMetadata.capture(item, data),
                    List.copyOf(block.getCollisionShape().getBoundingBoxes())));
        }
        BlockData data = origin.getBlockData();
        Material material = data.getMaterial();
        String name = material.name();
        boolean groundRow = Tag.SAPLINGS.isTagged(material) || Tag.CROPS.isTagged(material)
                || Tag.FLOWERS.isTagged(material) || data instanceof Ageable
                || name.endsWith("_FUNGUS") || name.endsWith("_ROOTS")
                || material == Material.SUGAR_CANE || material == Material.BAMBOO || material == Material.BAMBOO_SAPLING;
        return new PlacementTemplate(item, parts, groundRow);
    }

    public ItemStack item() {
        return item;
    }

    public List<Part> parts() {
        return parts;
    }

    public List<Part> writeOrder() {
        return writeOrder;
    }

    public boolean groundRow() {
        return groundRow;
    }

    public Part lowestPart() {
        return lowestPart;
    }

    public int minX() { return minX; }
    public int maxX() { return maxX; }
    public int minY() { return minY; }
    public int maxY() { return maxY; }
    public int minZ() { return minZ; }
    public int maxZ() { return maxZ; }

    static BlockData normalize(BlockData original, BlockData replaced) {
        BlockData data = original.clone();
        if (data instanceof Ageable ageable) {
            ageable.setAge(0);
        }
        if (data instanceof Slab slab && slab.getType() == Slab.Type.DOUBLE) {
            Slab.Type previous = replaced instanceof Slab old ? old.getType() : Slab.Type.BOTTOM;
            slab.setType(previous == Slab.Type.BOTTOM ? Slab.Type.TOP : Slab.Type.BOTTOM);
        }
        if (data instanceof Snow snow) {
            snow.setLayers(1);
        }
        if (data instanceof SeaPickle pickle) {
            pickle.setPickles(1);
        }
        if (data instanceof TurtleEgg egg) {
            egg.setEggs(1);
        }
        if (data instanceof Candle candle) {
            candle.setCandles(1);
        }
        return data;
    }

    public static final class Part {
        private final int x;
        private final int y;
        private final int z;
        private final BlockData dry;
        private final BlockData wet;
        private final BlockMetadata metadata;
        private final List<BoundingBox> collision;

        private Part(int x, int y, int z, BlockData data, BlockMetadata metadata, List<BoundingBox> collision) {
            this.x = x;
            this.y = y;
            this.z = z;
            dry = data;
            if (dry instanceof Waterlogged waterlogged) {
                waterlogged.setWaterlogged(false);
                wet = dry.clone();
                ((Waterlogged) wet).setWaterlogged(true);
            } else {
                wet = dry;
            }
            this.metadata = metadata;
            this.collision = collision;
        }

        public int x() { return x; }
        public int y() { return y; }
        public int z() { return z; }
        public Material material() { return dry.getMaterial(); }
        public BlockData data(Material replaced) { return replaced == Material.WATER ? wet : dry; }
        public BlockMetadata metadata() { return metadata; }
        public List<BoundingBox> collision() { return collision; }
    }
}
