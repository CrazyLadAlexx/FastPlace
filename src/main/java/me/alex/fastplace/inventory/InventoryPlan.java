package me.alex.fastplace.inventory;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class InventoryPlan {
    private final PlayerInventory inventory;
    private final ItemStack item;
    private final int[] slots;
    private final int available;

    public InventoryPlan(PlayerInventory inventory, ItemStack item, EquipmentSlot hand) {
        this.inventory = inventory;
        this.item = item;
        int handSlot = hand == EquipmentSlot.OFF_HAND ? 40 : inventory.getHeldItemSlot();
        slots = new int[37];
        int index = 0;
        for (int slot = 0; slot < 36; slot++) {
            if (slot != handSlot) {
                slots[index++] = slot;
            }
        }
        if (handSlot != 40) {
            slots[index++] = 40;
        }
        slots[index] = handSlot;
        int count = 0;
        for (int slot : slots) {
            ItemStack stack = inventory.getItem(slot);
            if (stack != null && stack.getType() == item.getType() && stack.getAmount() > 0
                    && stack.clone().isSimilar(item)) {
                count += stack.getAmount();
            }
        }
        available = count;
    }

    public int available() {
        return available;
    }

    public Reservation reserve(int requested) {
        List<Debit> debits = new ArrayList<>();
        int remaining = requested;
        for (int slot : slots) {
            if (remaining == 0) {
                break;
            }
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.getType() != item.getType() || stack.getAmount() <= 0) {
                continue;
            }
            ItemStack snapshot = stack.clone();
            if (!snapshot.isSimilar(item)) {
                continue;
            }
            int removed = Math.min(snapshot.getAmount(), remaining);
            snapshot.setAmount(snapshot.getAmount() - removed);
            debits.add(new Debit(slot, snapshot.getAmount() == 0 ? null : snapshot));
            remaining -= removed;
        }
        return new Reservation(requested - remaining, debits);
    }

    public final class Reservation {
        private final int amount;
        private final List<Debit> debits;

        private Reservation(int amount, List<Debit> debits) {
            this.amount = amount;
            this.debits = debits;
        }

        public int amount() {
            return amount;
        }

        public void commit() {
            for (Debit debit : debits) {
                inventory.setItem(debit.slot(), debit.remaining());
            }
        }
    }

    private record Debit(int slot, ItemStack remaining) {
    }
}
