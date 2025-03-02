package dev.geco.gsit.mcv.v1_19_R1_2.objects;

import org.bukkit.*;
import org.bukkit.craftbukkit.v1_19_R1.*;

import net.minecraft.world.entity.*;

public class PlayerSeatEntity extends AreaEffectCloud {

    public PlayerSeatEntity(Location Location) {

        super(((CraftWorld) Location.getWorld()).getHandle(), Location.getX(), Location.getY(), Location.getZ());

        persist = false;

        setRadius(0);
        setNoGravity(true);
        setInvulnerable(true);
        setDuration(Integer.MAX_VALUE);
        setParticle(CraftParticle.toNMS(Particle.BLOCK_CRACK, Material.AIR.createBlockData()));
        setWaitTime(0);
    }

    public void tick() { }

    public boolean canChangeDimensions() { return false; }

    public boolean rideableUnderWater() { return true; }

}