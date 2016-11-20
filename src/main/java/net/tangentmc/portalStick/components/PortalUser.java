package net.tangentmc.portalStick.components;

import lombok.Getter;
import lombok.Setter;
import net.tangentmc.nmsUtils.utils.V10Block;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.utils.RegionSetting;
import net.tangentmc.portalStick.utils.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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
    boolean upgradedGun = true;
    public PortalUser(String owner) {
        this.owner = owner;
    }

    public boolean isRegion() {
        return this.owner.startsWith("§region§_");
    }

    public boolean hasCube() {
        return cube != null;
    }

    public boolean setPrimary(Block block, Vector vector, Entity en) {
        Region r = PortalStick.getInstance().getRegionManager().getRegion(new V10Block(block));
        if (!isRegion()&&(!r.getBoolean(RegionSetting.ENABLE_BLUE_PORTALS))) return false;
        Location loc = en.getLocation();
        loc.setPitch(0);
        Portal tmp = new Portal(owner, vector, block, true, loc.getDirection());
        if (tmp.isFinished()) {
            //Close region portals if they exist
            //getPri will either point to a region or a user portal, but both could exist.
            if (this.getPrimary() != null) {
                this.getPrimary().delete();
            }
            r.getPortals(true).forEach(Portal::delete);
            this.primary = tmp;
            this.primary.open();
            if (this.getSecondary() != null) {
                this.getSecondary().open();
            } else if (r.getSecondary() != null) {
                r.getSecondary().open();
            }
            setCrosshair();
            return true;
        }
        setCrosshair();
        return false;
    }

    public void deletePortals() {
        if (this.secondary != null) this.secondary.delete();
        if (this.primary != null) this.primary.delete();
        this.secondary = null;
        this.primary = null;
        setCrosshair();
    }
    public boolean setSecondary(Block block, Vector vector, Entity en) {
        Region r = PortalStick.getInstance().getRegionManager().getRegion(new V10Block(block));
        if (!isRegion()&&(!r.getBoolean(RegionSetting.ENABLE_BLUE_PORTALS) || !upgradedGun)) return false;

        Location loc = en.getLocation();
        loc.setPitch(0);
        Portal tmp = new Portal(owner, vector, block, false, loc.getDirection());
        if (tmp.isFinished()) {
            //Close region portals if they exist
            if (this.getSecondary() != null) {
                this.getSecondary().delete();
            }
            r.getPortals(false).forEach(Portal::delete);
            this.secondary = tmp;
            this.secondary.open();
            if (this.getPrimary() != null) {
                this.getPrimary().open();
            } else if (r.getPrimary() != null) {
                r.getPrimary().open();
            }

            setCrosshair();
            return true;
        }
        setCrosshair();
        return false;
    }

    public void setCrosshair() {
        if (isRegion()) return;
        Region r = PortalStick.getInstance().getRegionManager().getRegion(new V10Block(Bukkit.getPlayer(owner).getLocation()));
        ItemStack is;
        if (!Bukkit.getPlayer(owner).getInventory().contains(Util.createPortalGun())) {
            is = new ItemStack(Material.AIR);
        } else if (!r.getBoolean(RegionSetting.ENABLE_ORANGE_PORTALS) || !upgradedGun){
            upgradedGun = false;
            if (this.primary != null) {
                is = CrosshairState.blue_both.create();
            } else {
                is = CrosshairState.blue_none.create();
            }
        } else {
            if (this.primary != null && this.secondary != null) {
                is = CrosshairState.none.create();
            } else if (this.primary != null) {
                is = CrosshairState.blue.create();
            } else if (this.secondary != null) {
                is = CrosshairState.orange.create();
            } else {
                is = Util.setUnbreakable(new ItemStack(Material.DIAMOND_HOE,1, (short)86));
            }
        }
        Bukkit.getPlayer(owner).getInventory().setItemInOffHand(is);
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

    void removeAllDrops() {
        items.forEach(Item::remove);
        items.clear();
        if (hasCube()) {
            this.cube.respawn();
        }
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
    private enum CrosshairState {
        both(86),orange(87),blue(88),none(89),blue_both(90),blue_none(91);
        short data;
        CrosshairState(int i) {
            data = (short)i;
        }
        ItemStack create() {
            return Util.setUnbreakable(new ItemStack(Material.DIAMOND_HOE,1, data));
        }
    }
}
