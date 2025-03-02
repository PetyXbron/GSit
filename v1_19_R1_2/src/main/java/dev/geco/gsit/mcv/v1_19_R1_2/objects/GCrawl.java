package dev.geco.gsit.mcv.v1_19_R1_2.objects;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.scheduler.*;
import org.bukkit.craftbukkit.v1_19_R1.entity.*;

import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.*;

import dev.geco.gsit.GSitMain;
import dev.geco.gsit.objects.*;

public class GCrawl implements IGCrawl {

    private final GSitMain GPM = GSitMain.getInstance();

    private final Player player;

    private final ServerPlayer serverPlayer;

    protected final BoxEntity boxEntity;

    private Location blockLocation;

    private boolean blockPresent;

    private boolean boxPresent;

    protected final BlockData blockData = Material.BARRIER.createBlockData();

    private final Listener listener;
    private final Listener moveListener;
    private final Listener stopListener;

    public GCrawl(Player Player) {

        player = Player;

        serverPlayer = ((CraftPlayer) player).getHandle();

        boxEntity = new BoxEntity(player.getLocation());

        listener = new Listener() {

            @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
            public void ETogSE(EntityToggleSwimEvent Event) { if(Event.getEntity() == player) Event.setCancelled(true); }

            @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
            public void PIntE(PlayerInteractEvent Event) {

                if(Event.getPlayer() == player && blockPresent && Event.getClickedBlock().equals(blockLocation.getBlock()) && Event.getHand() == EquipmentSlot.HAND) {

                    Event.setCancelled(true);

                    new BukkitRunnable() {

                        @Override
                        public void run() {

                            buildBlock();
                        }
                    }.runTaskAsynchronously(GPM);
                }
            }
        };

        moveListener = new Listener() {

            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void PMovE(PlayerMoveEvent Event) {

                if(Event.getPlayer() == player) {

                    Location locationFrom = Event.getFrom(), locationTo = Event.getTo();

                    if(locationFrom.getX() != locationTo.getX() || locationFrom.getZ() != locationTo.getZ() || locationFrom.getY() != locationTo.getY()) tick(locationFrom);
                }
            }
        };

        stopListener = new Listener() {

            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void PTogSE(PlayerToggleSneakEvent Event) { if(Event.getPlayer() == player && Event.isSneaking()) GPM.getCrawlManager().stopCrawl(player, GetUpReason.GET_UP); }
        };
    }

    public void start() {

        player.setSwimming(true);

        Bukkit.getPluginManager().registerEvents(listener, GPM);

        new BukkitRunnable() {

            @Override
            public void run() {

                Bukkit.getPluginManager().registerEvents(moveListener, GPM);

                if(GPM.getCManager().C_GET_UP_SNEAK) Bukkit.getPluginManager().registerEvents(stopListener, GPM);

                tick(player.getLocation());
            }
        }.runTaskLaterAsynchronously(GPM, 1);
    }

    private void tick(Location Location) {

        if(!checkCrawlValid()) return;

        Location location = Location.clone();

        Block locationBlock = location.getBlock();

        int blockSize = (int) ((location.getY() - location.getBlockY()) * 100.0);

        location.setY(location.getBlockY() + (blockSize >= 40 ? 2.49 : 1.49));

        Block aboveBlock = location.getBlock();

        boolean aboveBlockSolid = aboveBlock.getBoundingBox().contains(location.toVector()) && aboveBlock.getCollisionShape().getBoundingBoxes().size() > 0;
        boolean canPlaceBlock = isValidArea(locationBlock.getRelative(BlockFace.UP), aboveBlock, blockLocation != null ? blockLocation.getBlock() : null);
        boolean canSetBarrier = canPlaceBlock && (aboveBlock.getType().isAir() || aboveBlockSolid);

        if(blockLocation == null || !aboveBlock.equals(blockLocation.getBlock())) {

            destoryBlock();

            blockLocation = location;

            if(canSetBarrier && !aboveBlockSolid) buildBlock();
        }

        if(!canSetBarrier && !aboveBlockSolid) {

            new BukkitRunnable() {

                @Override
                public void run() {

                    Location playerLocation = Location.clone();

                    int height = locationBlock.getBoundingBox().getHeight() >= 0.4 || playerLocation.getY() % 0.015625 == 0.0 ? (player.getFallDistance() > 0.7 ? 0 : blockSize) : 0;

                    playerLocation.setY(playerLocation.getY() + (height >= 40 ? 1.5 : 0.5));

                    boxEntity.setRawPeekAmount(height >= 40 ? 100 - height : 0);

                    if(boxPresent) {

                        serverPlayer.connection.send(new ClientboundSetEntityDataPacket(boxEntity.getId(), boxEntity.getEntityData(), true));

                        boxEntity.teleportToWithTicket(playerLocation.getX(), playerLocation.getY(), playerLocation.getZ());

                        serverPlayer.connection.send(new ClientboundTeleportEntityPacket(boxEntity));
                    } else {

                        boxEntity.setPos(playerLocation.getX(), playerLocation.getY(), playerLocation.getZ());

                        serverPlayer.connection.send(new ClientboundAddEntityPacket(boxEntity));

                        boxPresent = true;

                        serverPlayer.connection.send(new ClientboundSetEntityDataPacket(boxEntity.getId(), boxEntity.getEntityData(), true));
                    }
                }
            }.runTask(GPM);
        } else destoryEntity();
    }

    public void stop() {

        HandlerList.unregisterAll(listener);
        HandlerList.unregisterAll(moveListener);
        HandlerList.unregisterAll(stopListener);

        player.setSwimming(false);

        if(blockLocation != null) player.sendBlockChange(blockLocation, blockLocation.getBlock().getBlockData());

        serverPlayer.connection.send(new ClientboundRemoveEntitiesPacket(boxEntity.getId()));
    }

    private void buildBlock() {

        player.sendBlockChange(blockLocation, blockData);

        blockPresent = true;
    }

    private void destoryBlock() {

        if(blockPresent) {

            player.sendBlockChange(blockLocation, blockLocation.getBlock().getBlockData());

            blockPresent = false;
        }
    }

    private void destoryEntity() {

        if(boxPresent) {

            serverPlayer.connection.send(new ClientboundRemoveEntitiesPacket(boxEntity.getId()));

            boxPresent = false;
        }
    }

    private boolean checkCrawlValid() {

        if(serverPlayer.isInWater() || player.isFlying()) {

            new BukkitRunnable() {

                @Override
                public void run() {

                    GPM.getCrawlManager().stopCrawl(player, GetUpReason.ACTION);
                }
            }.runTask(GPM);

            return false;
        }

        return true;
    }

    private boolean isValidArea(Block BlockUp, Block AboveBlock, Block LocationBlock) { return BlockUp.equals(AboveBlock) || BlockUp.equals(LocationBlock); }

    public Player getPlayer() { return player; }

    public String toString() { return boxEntity.getUUID().toString(); }

}