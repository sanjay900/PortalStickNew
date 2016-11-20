package net.tangentmc.portalStick.utils;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public enum GelType {
    //TODO: should we base ground on the config?
    CONVERSION(new ItemStack(Material.SNOW_BLOCK),69,73,77,81),
    JUMP(new ItemStack(Material.WOOL, 1, (short) 3),70,74,78,82),
    SPEED(new ItemStack(Material.WOOL, 1, (short) 1),71,75,79,83),
    WATER(new ItemStack(Material.ICE),72,76,80,84);
    @Getter
    ItemStack ground;
    int[] blobs;

    GelType(ItemStack ground, int... blobs) {
        this.ground = ground;
        this.blobs = blobs;
    }

    public static Random random = new Random();

    public ItemStack randomBlob() {
        return Util.setUnbreakable(new ItemStack(Material.DIAMOND_HOE, 1, (short) blobs[random.nextInt(blobs.length)]));
    }

    public static GelType fromDispenser(ItemStack is) {
        for (GelType type : values()) {
            if (type.ground.getType() == is.getType() && type.getGround().getData().getData() == is.getData().getData()) {
                return type;
            }
        }
        return null;
    }
}
