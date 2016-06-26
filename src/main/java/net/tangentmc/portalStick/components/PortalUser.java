package net.tangentmc.portalStick.components;

import lombok.Getter;
import lombok.Setter;
import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.utils.Config.Sound;
import net.tangentmc.portalStick.utils.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PortalUser {
    String owner;
    Portal primary;
    Portal secondary;
    Cube cube;
    ItemStack previous;
    public PortalUser(String owner) {
        this.owner = owner;
    }

    public boolean isRegion() {
        return this.owner.startsWith("§region§_");
    }

    public boolean hasCube() {
        return cube != null;
    }

    public boolean setPrimary(Block block, Vector vector) {
        if (this.primary != null) {
            this.primary.close();
            this.primary.delete();
        }
        this.primary = new Portal(owner, vector, block, true);
        if (this.primary.isFinished()) {
            Util.playSound(Sound.PORTAL_CREATE_BLUE, new V10Block(block));
            this.primary.open();
            if (this.secondary != null) {
                this.secondary.open();
            }
            return true;
        } else
            this.primary = null;
        return false;
    }

    public void deletePortals() {
        if (this.secondary != null) this.secondary.delete();
        if (this.primary != null) this.primary.delete();
        this.secondary = null;
        this.primary = null;

    }

    public boolean setSecondary(Block block, Vector vector) {
        if (this.secondary != null) {
            this.secondary.close();
            this.secondary.delete();
        }
        this.secondary = new Portal(owner, vector, block, false);
        if (this.secondary.isFinished()) {
            this.secondary.open();
            Util.playSound(Sound.PORTAL_CREATE_ORANGE, new V10Block(block));
            if (this.primary != null) {
                this.primary.open();
            }
            return true;
        } else
            this.secondary = null;
        return false;
    }

    List<Item> items = new ArrayList<>();
    private boolean usingTool = false;
    private boolean hasDefaultTexture = true;
    private V10Block pointOne;
    private V10Block pointTwo;
    private Entity heldEntity;

    public void addItem(Item itemDrop) {
        items.add(itemDrop);
    }

    public void removeAllDrops() {
        items.forEach(Item::remove);
        items.clear();
        if (hasCube()) {
            this.cube.respawn();
        }
    }

    public void removeAllPortals() {
        if (primary != null) primary.delete();
        if (secondary != null) secondary.delete();
    }

    int currentAmt = 4;
    Entity vehicle;

    public void testHeld() {
        if (heldEntity != null && !isRegion()) {
            Location loc = Bukkit.getPlayer(owner).getEyeLocation().add(Bukkit.getPlayer(owner).getLocation().getDirection().multiply(currentAmt));
            heldEntity.teleport(loc);
        }
    }

    public void setHeldEntity(Entity en) {
        if (this.heldEntity != null)
            heldEntity.setGravity(true);
        this.heldEntity = en;
        if (this.heldEntity != null)
            heldEntity.setGravity(false);
    }
}
