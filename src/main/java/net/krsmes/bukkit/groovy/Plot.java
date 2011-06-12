package net.krsmes.bukkit.groovy;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.io.Serializable;
import java.util.*;


public class Plot implements Serializable {
    private static final long serialVersionUID = 1L;

    public static int PLOT_START_DEPTH = 32;

    String name;
    String owner;
    Location home;

    List<Area> areas = new ArrayList<Area>();
    List<String> visitors = new ArrayList<String>();

    int startDepth = PLOT_START_DEPTH;

    boolean open;
    boolean noExplode;
    boolean noSpawn;
    boolean noTarget;
    boolean noTeleport;
    boolean noChat;
    boolean noLightning;
    boolean noIgnite;
    boolean noDamage;

    int[] placeableArr = new int[0];
    int[] breakableArr = new int[0];
    int[] interactableArr = new int[0];

    int[] unplaceableArr = new int[0];
    int[] unbreakableArr = new int[0];
    int[] uninteractableArr = new int[0];

    public Plot() {}

    public Plot(String name) {
        this.name = name;
    }

    public Plot(String name, Area area) {
        this(name);
        if (area != null) { areas.add(area); }
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Plot) && (name.equals(((Plot) o).name));
    }

    @Override
    public String toString() {
        return name + (owner==null?"":'('+owner+')');
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setOwner(Player owner) {
        setOwner(owner.getName());
    }

    public Location getHome() {
        return home;
    }

    public void setHome(Location home) {
        this.home = home;
    }

    public List<Area> getAreas() {
        return areas;
    }

    public void setAreas(List<Area> areas) {
        this.areas = areas;
    }

    public List<String> getVisitors() {
        return visitors;
    }

    public void setVisitors(List<String> visitors) {
        this.visitors = visitors;
    }

    public int getStartDepth() {
        return startDepth;
    }

    public void setStartDepth(int startDepth) {
        this.startDepth = startDepth;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public boolean isNoExplode() {
        return noExplode;
    }

    public void setNoExplode(boolean noExplode) {
        this.noExplode = noExplode;
    }

    public boolean isNoSpawn() {
        return noSpawn;
    }

    public void setNoSpawn(boolean noSpawn) {
        this.noSpawn = noSpawn;
    }

    public boolean isNoTarget() {
        return noTarget;
    }

    public void setNoTarget(boolean noTarget) {
        this.noTarget = noTarget;
    }

    public boolean isNoTeleport() {
        return noTeleport;
    }

    public void setNoTeleport(boolean noTeleport) {
        this.noTeleport = noTeleport;
    }

    public boolean isNoChat() {
        return noChat;
    }

    public void setNoChat(boolean noChat) {
        this.noChat = noChat;
    }

    public boolean isNoLightning() {
        return noLightning;
    }

    public void setNoLightning(boolean noLightning) {
        this.noLightning = noLightning;
    }

    public boolean isNoIgnite() {
        return noIgnite;
    }

    public void setNoIgnite(boolean noIgnite) {
        this.noIgnite = noIgnite;
    }

    public boolean isNoDamage() {
        return noDamage;
    }

    public void setNoDamage(boolean noDamage) {
        this.noDamage = noDamage;
    }

    public Set<Integer> getPlaceable() {
        return intArrToSet(placeableArr);
    }

    public void setPlaceable(Set<Integer> value) {
        placeableArr = setToIntArr(value);
    }

    public Set<Integer> getBreakable() {
        return intArrToSet(breakableArr);
    }

    public void setBreakable(Set<Integer> value) {
        breakableArr = setToIntArr(value);
    }

    public Set<Integer> getInteractable() {
        return intArrToSet(interactableArr);
    }

    public void setInteractable(Set<Integer> value) {
        interactableArr = setToIntArr(value);
    }


    public Set<Integer> getUnplaceable() {
        return intArrToSet(unplaceableArr);
    }

    public void setUnplaceable(Set<Integer> value) {
        unplaceableArr = setToIntArr(value);
    }

    public Set<Integer> getUnbreakable() {
        return intArrToSet(unbreakableArr);
    }

    public void setUnbreakable(Set<Integer> value) {
        unbreakableArr = setToIntArr(value);
    }

    public Set<Integer> getUninteractable() {
        return intArrToSet(uninteractableArr);
    }

    public void setUninteractable(Set<Integer> value) {
        uninteractableArr = setToIntArr(value);
    }


    public boolean isPublic() {
        return false;
    }

    public int getSize() {
        int result = 0;
        for (Area a : areas) {
            result += a.getSize();
        }
        return result;
    }

    public void addArea(Area area) {
        this.areas.add(area);
    }

    public void addVisitor(String visitor) {
        this.visitors.add(visitor);
    }

    public void addVisitor(Player player) {
        addVisitor(player.getName());
    }

    public void removeVisitor(String visitor) {
        this.visitors.remove(visitor);
    }

    public void removeVisitor(Player player) {
        removeVisitor(player.getName());
    }

    public boolean allowed(String name) {
        return open || name.equals(owner) || visitors.contains(name);
    }

    public boolean allowed(Player player) {
        return open || allowed(player.getName());
    }

    public boolean contains(int x, int z) {
        for (Area area : areas) {
            if (area.contains(x, z)) { return true; }
        }
        return false;
    }

    public boolean allowDamage(Player player, Block block) {
        // assumes plot contains block.x and block.z
        int typeId = block.getTypeId();
        return (block.getY() < startDepth ||
                (allowed(player)) && Arrays.binarySearch(unbreakableArr, typeId) < 0) ||
                Arrays.binarySearch(breakableArr, typeId) >= 0;
    }

    public boolean allowInteract(Player player, Block block, ItemStack item) {
        // assumes plot contains block.x and block.z
        if (block == null || block.getY() < startDepth) { return true; }
        int typeId = item == null ? 0 : item.getTypeId();
        if (typeId > 0) {
            return (allowed(player) && Arrays.binarySearch(unplaceableArr, typeId) < 0) ||
                    (Arrays.binarySearch(placeableArr, typeId) >= 0);
        }
        typeId = block.getTypeId();
        return (allowed(player) && Arrays.binarySearch(uninteractableArr, typeId) < 0) ||
                (Arrays.binarySearch(interactableArr, typeId) >= 0);
    }

    public void processEvent(PlayerInteractEvent e) {
        if (!allowInteract(e.getPlayer(), e.getClickedBlock(), e.getItem())) {
            e.setUseInteractedBlock(Event.Result.DENY);
        }
    }

    public void processEvent(BlockDamageEvent e) {
        if (!allowDamage(e.getPlayer(), e.getBlock())) {
            e.setCancelled(true);
        }
    }


    static Set<Integer> intArrToSet(int[] arr) {
        Set<Integer> result = new HashSet<Integer>(arr.length);
        for (int i : arr) {
            result.add(i);
        }
        return result;
    }

    static int[] setToIntArr(Set<Integer> value) {
        int[] result = new int[value.size()];
        int idx = 0;
        for (int i : value) {
            result[idx++] = i;
        }
        Arrays.sort(result);
        return result;
    }

}
