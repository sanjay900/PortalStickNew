package net.tangentmc.portalStick.components;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import net.tangentmc.nmsUtils.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import lombok.Getter;
import lombok.NoArgsConstructor;
import net.tangentmc.nmsUtils.entities.HologramFactory;
import net.tangentmc.nmsUtils.entities.NMSHologram;
import net.tangentmc.portalStick.PortalStick;
import net.tangentmc.portalStick.components.Wire.PoweredReason;
import net.tangentmc.portalStick.utils.Config.Sound;
import net.tangentmc.portalStick.utils.RegionSetting;
import net.tangentmc.portalStick.utils.Util;
@Getter
@NoArgsConstructor
public class Grill {
    public Grill(V10Block initial) {
        init = initial;
        complete = placeRecursiveEmancipationGrill(init);
        if (complete) {
            open();
            PortalStick.getInstance().getGrillManager().grills.add(this);
        }
    }
    V10Block init;
    int max;
    private HashSet<V10Block> border;
    private HashSet<V10Block> inside;
    boolean complete;
    public boolean placeRecursiveEmancipationGrill(V10Block initial) {
        String borderID = PortalStick.getInstance().getRegionManager().getRegion(initial).getString(RegionSetting.GRILL_MATERIAL);
        if (!BlockUtil.compareBlockToString(initial, borderID))
            return false;


        //Attempt to get complete border
        border = new HashSet<>();
        inside = new HashSet<>();
        startRecurse(initial, borderID, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.DOWN, BlockFace.UP);
        if (!complete)
            startRecurse(initial, borderID, BlockFace.UP, BlockFace.WEST, BlockFace.EAST, BlockFace.DOWN, BlockFace.SOUTH, BlockFace.NORTH);
        if (!complete)
            startRecurse(initial, borderID, BlockFace.UP, BlockFace.SOUTH, BlockFace.NORTH, BlockFace.DOWN, BlockFace.EAST, BlockFace.WEST);
        return complete;
    }


    private void startRecurse(V10Block initial, String id, BlockFace one, BlockFace two, BlockFace three, BlockFace four, BlockFace iOne, BlockFace iTwo) {
        border.clear();
        inside.clear();
        max = 0;
        complete = false;
        recurse(initial, id, initial, one, two, three, four);
        generateInsideBlocks(id, initial, iOne, iTwo);

        if (inside.size() == 0)
            complete = false;
    }
    Vector rmin;
    Vector rmax;
    private void generateInsideBlocks(String borderID, V10Block initial, BlockFace iOne, BlockFace iTwo) {

        //Work out maximums and minimums
        Vector max = border.toArray(new V10Block[0])[0].getHandle().toVector();
        Vector min = border.toArray(new V10Block[0])[0].getHandle().toVector();
        rmax = border.toArray(new V10Block[0])[0].getHandle().toVector();
        rmin = border.toArray(new V10Block[0])[0].getHandle().toVector();
        for (V10Block block : border.toArray(new V10Block[0])) {
            if (block.getX() >= max.getX()) max.setX(block.getX());
            if (block.getY() >= max.getY()) max.setY(block.getY());
            if (block.getZ() >= max.getZ()) max.setZ(block.getZ());
            if (block.getX() <= min.getX()) min.setX(block.getX());
            if (block.getY() <= min.getY()) min.setY(block.getY());
            if (block.getZ() <= min.getZ()) min.setZ(block.getZ());
        }
        rmin = min;
        rmax = max;

        //Loop through all blocks in the min-max range checking for 'inside' blocks
        World world = initial.getHandle().getWorld();
        Block rb;
        for (int y = (int)min.getY(); y <= (int)max.getY(); y++) {
            for (int x = (int)min.getX(); x <= (int)max.getX(); x++) {
                for (int z = (int)min.getZ(); z <= (int)max.getZ(); z++) {
                    rb = world.getBlockAt(x, y, z);
                    initial = new V10Block(rb);
                    if (border.contains(initial) || inside.contains(initial))
                        continue;
                    boolean add = true;
                    for (BlockFace face : FaceUtil.BLOCK_SIDES) {
                        if (face == iOne || face == iTwo)
                            continue;
                        Block temp = rb.getRelative(face);
                        while (temp.getLocation().toVector().isInAABB(min, max)) {

                            if (BlockUtil.compareBlockToString(temp, borderID))
                                break;
                            temp = temp.getRelative(face);
                        }
                        if (!BlockUtil.compareBlockToString(temp, borderID)) {
                            add = false;
                            break;
                        }
                    }

                    if (add) {
                        inside.add(initial);
                    }
                }
            }
        }
    }

    private void recurse(V10Block initial, String id, V10Block vb, BlockFace one, BlockFace two, BlockFace three, BlockFace four) {
        if (max >= 100) return;
        if (vb.equals(initial) && border.size() > 2) {
            complete = true;
            return;
        }
        if (BlockUtil.compareBlockToString(vb, id) && !border.contains(vb)) {
            border.add(vb);
            max++;
            Block b = vb.getHandle().getBlock();
            recurse(initial, id, new V10Block(b.getRelative(one)), one, two, three, four);
            recurse(initial, id, new V10Block(b.getRelative(two)), one, two, three, four);
            recurse(initial, id, new V10Block(b.getRelative(three)), one, two, three, four);
            recurse(initial, id, new V10Block(b.getRelative(four)), one, two, three, four);
        }
    }
    List<NMSHologram> grill = new ArrayList<>();
    public void open() {
        World w = inside.toArray(new V10Block[0])[0].getHandle().getWorld();
        //-1? An extra block is counted due to how you need to fill the inside, and the block count starts including the block under the grill
        int height = rmax.getBlockY()-rmin.getBlockY()-1;
        for (int x = rmin.getBlockX();x<=rmax.getBlockX();x++) {
            for (int z = rmin.getBlockZ();z<=rmax.getBlockZ();z++) {
                if (new Location(w,x+0.5,rmax.getY()-1,z+0.5).getBlock().getType() == Material.MOSSY_COBBLESTONE) continue;
                HologramFactory factory = new HologramFactory().withLocation(new Location(w,x+0.5,rmax.getY()-0.5,z+0.5));
                for (int y = 0; y <= height;y++) {
                    if (Utils.isSolid(w.getBlockAt(x,rmax.getBlockY()-y,z).getType())) continue;
                    factory = factory.withHead(Util.setUnbreakable(new ItemStack(Material.DIAMOND_HOE,1,(short)92)),1);
                }
                NMSHologram holo = factory.build();
                grill.add(holo);
                for (Entity en : holo.getLines()) {
                    new SubGrill(en);
                }
            }
        }
        PortalStick.getInstance().getGrillManager().grills.add(this);
    }
    public void emacipate(Entity en) {
        PortalUser user = PortalStick.getInstance().getUser(en.getName());
        for (V10Block b : border) {
            PortalStick.getInstance().getWireManager().powerBlock(b.getHandle().getBlock(),true,PoweredReason.GRILL);
            Bukkit.getScheduler().runTaskLater(PortalStick.getInstance(), () -> PortalStick.getInstance().getWireManager().powerBlock(b.getHandle().getBlock(),false,PoweredReason.GRILL),10l);
        }
        if (en instanceof Player) {
            user.removeAllDrops();
            user.deletePortals();
            if (Cooldown.tryCooldown(en, this.toString(), 1000)) {
                Util.playSound(Sound.GRILL_EMANCIPATE, new V10Block(en.getLocation()));
            }
            ((Player)en).getInventory().clear();
        } else {
            en.remove();
        }

        if (Cooldown.tryCooldown(en, this.toString(), 1000))
            for (V10Block b : inside) {
                b.getHandle().getWorld().playEffect(b.getHandle(), Effect.SMOKE, 16);
                Util.playSound(Sound.GRILL_EMANCIPATE, new V10Block(b.getHandle()));
            }
        Region region = PortalStick.getInstance().getRegionManager().getRegion(new V10Block(en.getLocation()));
        if (en instanceof InventoryHolder) {
            InventoryHolder ih = (InventoryHolder)en;
            Inventory inv = ih.getInventory();
            boolean changed = false;
            ItemStack[] inv2;
            List<String> ice = region.getList(RegionSetting.GRILL_REMOVE_EXCEPTIONS);
            ItemStack newSlot;
            if(inv instanceof PlayerInventory)
            {
                PlayerInventory pi = (PlayerInventory)inv;
                inv2 = pi.getArmorContents();

                for(int i = 0; i < inv2.length; i++)
                {
                    if(inv2[i] == null)
                        continue;
                    newSlot = Util.checkItemSlot(inv2[i], ice);
                    if(newSlot != inv2[i])
                    {
                        inv2[i] = newSlot;
                        changed = true;
                    }
                }
                if(changed)
                {
                    pi.setArmorContents(inv2);
                    changed = false;
                }
            }
            inv2 = inv.getContents();
            for(int i = 0; i < inv2.length; i++)
            {
                if(inv2[i] == null)
                    continue;
                newSlot = Util.checkItemSlot(inv2[i], ice);
                if(newSlot != inv2[i])
                {
                    inv2[i] = newSlot;
                    changed = true;
                }
            }
            if(region.getBoolean(RegionSetting.GRILL_GIVE_GUN_IF_NEEDED))
            {
                boolean hasGun = false;
                for(int i = 0; i < inv2.length; i++)
                {

                    if(Util.isPortalGun(inv2[i]))
                    {
                        hasGun = true;
                        break;
                    }
                }
                if(!hasGun)
                {

                    ItemStack gun = Util.createPortalGun();
                    for(int i = 0; i < inv2.length; i++)
                    {
                        if(inv2[i] == null)
                        {
                            inv2[i] = gun;
                            changed = hasGun = true;
                            break;
                        }
                    }
                    if(!hasGun)
                        en.getLocation().getWorld().dropItemNaturally(en.getLocation(), gun);
                }
            }
            if(region.getBoolean(RegionSetting.GRILL_GIVE_BOOTS_IF_NEEDED))
            {
                ItemStack boots = Utils.getItemData(region.getString(RegionSetting.FALL_DAMAGE_BOOTS));
                if(inv instanceof PlayerInventory)
                {
                    PlayerInventory pi = (PlayerInventory)inv;
                    if(pi.getBoots() == null)
                    {
                        pi.setBoots(boots);
                    }
                }

            }

            if(region.getBoolean(RegionSetting.GRILL_REMOVE_EXTRA_GUNS))
            {
                boolean hasGun = false;
                for(int i = 0; i < inv2.length; i++)
                {

                    if(Util.isPortalGun(inv2[i]))
                    {
                        if(hasGun)
                        {
                            inv2[i] = null;
                            changed = true;
                        }
                        else
                        {
                            if(inv2[i].getAmount() != 1)
                                inv2[i].setAmount(1);
                            hasGun = true;
                        }
                    }
                }
            }
            if(changed)
                inv.setContents(inv2);
        }
        if (en instanceof Player) {
            user.setCrosshair();
            ((Player)en).updateInventory();
        }
    }

    public String getStringLocation()
    {
        Location loc = init.getHandle();
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ();
    }

    public void close() {
        grill.forEach(NMSHologram::remove);
    }
    public void remove() {
        grill.forEach(NMSHologram::remove);
        PortalStick.getInstance().getGrillManager().grills.remove(this);
        PortalStick.getInstance().getConfiguration().saveAll();
    }

    @MetadataSaver.Metadata(metadataName = "grillen")
    public class SubGrill extends MetadataSaver {
        @Getter
        Grill grill = Grill.this;

        protected SubGrill(Entity en) {
            initMetadata(en);
        }
    }
}
