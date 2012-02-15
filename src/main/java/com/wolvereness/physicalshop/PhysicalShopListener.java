package com.wolvereness.physicalshop;

import static com.wolvereness.physicalshop.config.ConfigOptions.SERVER_SHOP;
import static com.wolvereness.physicalshop.config.Localized.Message.CANT_BUILD;
import static com.wolvereness.physicalshop.config.Localized.Message.CANT_BUILD_SERVER;
import static com.wolvereness.physicalshop.config.Localized.Message.CANT_PLACE_CHEST;
import static com.wolvereness.physicalshop.config.Localized.Message.CANT_USE;
import static com.wolvereness.physicalshop.config.Localized.Message.CANT_USE_CHEST;
import static com.wolvereness.physicalshop.config.Localized.Message.EXISTING_CHEST;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.wolvereness.physicalshop.exception.InvalidSignException;
import com.wolvereness.physicalshop.exception.InvalidSignOwnerException;
import com.wolvereness.util.NameCollection;
import com.wolvereness.util.NameCollection.OutOfEntriesException;

/**
 * @author Wolfe
 * Licensed under GNU GPL v3
 */
public class PhysicalShopListener implements Listener {
	private final PhysicalShop plugin;
	/**
	 * Default constructor
	 * @param plugin PhysicalShop plugin to consider
	 */
	public PhysicalShopListener(final PhysicalShop plugin) {
		this.plugin = plugin;
	}
	/**
	 * Block Break event
	 * @param e Event
	 */
	@EventHandler
	public void onBlockBreak(final BlockBreakEvent e) {
		if (e.isCancelled() || ! plugin.getPluginConfig().isProtectBreak()) return;

		if (!ShopHelpers.isBlockDestroyable(e.getBlock(), e.getPlayer(), plugin)) {
			//PhysicalShop.sendMessage(e.getPlayer(),"CANT_DESTROY");
			e.setCancelled(true);
		}
	}
	/**
	 * Block Burn event
	 * @param e Event
	 */
	@EventHandler
	public void onBlockBurn(final BlockBurnEvent e) {
		if (e.isCancelled() || !plugin.getPluginConfig().isProtectBreak()) return;

		// Messaging.save(e.getPlayer());

		if (!ShopHelpers.isBlockDestroyable(e.getBlock(), null, plugin)) {
			e.setCancelled(true);
		}
	}
	/**
	 * Block Place event
	 * @param e Event
	 */
	@EventHandler
	public void onBlockPlace(final BlockPlaceEvent e) {
		if (e.isCancelled()) return;

		if (!plugin.getPluginConfig().isProtectChestAccess()) return;

		if (plugin.getPermissionHandler().hasAdmin(e.getPlayer())) return;

		if (e.getBlock().getType() != Material.CHEST) return;

		// Messaging.save(e.getPlayer());

		final Block block = e.getBlock();

		final Block[] blocks = new Block[] {
				block.getRelative(BlockFace.NORTH),
				block.getRelative(BlockFace.EAST),
				block.getRelative(BlockFace.SOUTH),
				block.getRelative(BlockFace.WEST), };

		for (final Block b : blocks) {
			if (b.getType() == Material.CHEST) {
				final Shop shop = ShopHelpers.getShop(b.getRelative(BlockFace.UP), plugin);

				if ((shop != null) && shop.isShopBlock(b)) {
					plugin.getLocale().sendMessage(e.getPlayer(), CANT_PLACE_CHEST);
					e.setCancelled(true);
					break;
				}
			}
		}
	}
	/**
	 * Block the BlockPistonExtendEvent if it will move the block
	 * a store sign is on.
	 * @param e Event
	 */
	@EventHandler
	public void onPistonExtend(final BlockPistonExtendEvent e) {
		if (e.isCancelled() || !plugin.getPluginConfig().isProtectBreak()) return;

		for (final Block block : e.getBlocks()) {
			if (!ShopHelpers.isBlockDestroyable(block, null, plugin)) {
				e.setCancelled(true);
			}
		}
	}
	/**
	 * Block the BlockPistonRetractEvent if it will move the block
	 * a store sign is on.
	 * @param e Event
	 */
	@EventHandler
	public void onPistonRetract(final BlockPistonRetractEvent e) {
		if (e.isCancelled() || !plugin.getPluginConfig().isProtectBreak()) return;

		// We only care about sticky pistons retracting.
		if (!e.isSticky()) return;

		BlockFace direction = e.getDirection();
		Block pulledBlock = e.getBlock().getRelative(direction, 2);

		if (!ShopHelpers.isBlockDestroyable(pulledBlock, null, plugin)) {
			e.setCancelled(true);
		}
	}
	/**
	 * Entity Explode event
	 * @param e Event
	 */
	@EventHandler
	public void onEntityExplode(final EntityExplodeEvent e) {
		if (e.isCancelled()) return;

		if (!plugin.getPluginConfig().isProtectExplode()) return;

		for (final Block block : e.blockList()) {
			if (!ShopHelpers.isBlockDestroyable(block, null, plugin)) {
				e.setCancelled(true);
				return;
			}
		}
	}
	/**
	 * Stop EntityChangeBlockEvents from affecting stores.
	 * Prevents Endermen from breaking stores, etc.
	 * @param e Event
	 */
	@EventHandler
	public void onEntityChangeBlock(final EntityChangeBlockEvent e) {
		if (e.isCancelled() || !plugin.getPluginConfig().isProtectBreak()) return;

		if (!ShopHelpers.isBlockDestroyable(e.getBlock(), null, plugin)) {
			e.setCancelled(true);
		}
	}
	/**
	 * Player Interact event
	 * @param e Event
	 */
	@EventHandler
	public void onPlayerInteract(final PlayerInteractEvent e) {
		if (e.isCancelled()) return;

		final Block block = e.getClickedBlock();

		if (
				plugin.getPluginConfig().isProtectChestAccess()
				&& (e.getAction() == Action.RIGHT_CLICK_BLOCK)
				&& (block.getType() == Material.CHEST)) {
			final Shop shop = ShopHelpers.getShop(block.getRelative(BlockFace.UP), plugin);
			if (
					(shop != null)
					&& shop.isShopBlock(block)
					&& !shop.canDestroy(e.getPlayer(), plugin)
					&& !plugin.getPermissionHandler().hasAdmin(e.getPlayer())) {
				plugin.getLocale().sendMessage(e.getPlayer(), CANT_USE_CHEST);
				e.setCancelled(true);
				return;
			}
		}

		final Shop shop = ShopHelpers.getShop(block, plugin);

		if (shop == null) return;

		if (!plugin.getPermissionHandler().hasUse(e.getPlayer())) {
			plugin.getLocale().sendMessage(e.getPlayer(), CANT_USE);
			return;
		}

		if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
			shop.status(e.getPlayer(), plugin);
		} else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			shop.interact(e.getPlayer(), plugin);
			e.setCancelled(true);
		}
	}
	/**
	 * Sign Change event
	 * @param e Event
	 */
	@EventHandler
	public void onSignChange(final SignChangeEvent e) {
		if (e.isCancelled()) return;

		//final String playerName = ShopHelpers.truncateName(e.getPlayer().getName());
		try {
			new Shop(e.getLines(), plugin);
		} catch (final InvalidSignOwnerException ex) {
		} catch (final InvalidSignException ex) {
			return;
		}

		if (!plugin.getPermissionHandler().hasBuild(e.getPlayer())) {
			plugin.getLocale().sendMessage(e.getPlayer(), CANT_BUILD);
			e.setCancelled(true);
			return;
		}

		if (e.getLine(3).equalsIgnoreCase(plugin.getConfig().getString(SERVER_SHOP))) {
			if (!plugin.getPermissionHandler().hasAdmin(e.getPlayer())) {
				plugin.getLocale().sendMessage(e.getPlayer(), CANT_BUILD_SERVER);
				e.setCancelled(true);
				return;
			}
		} else {
			if (
					e.getBlock().getRelative(BlockFace.DOWN).getType() == Material.CHEST
					&& plugin.getPluginConfig().isExistingChestProtected()
					&& !plugin.getPermissionHandler().hasAdmin(e.getPlayer())
					&& !plugin.lwcCheck(e.getBlock().getRelative(BlockFace.DOWN), e.getPlayer())
					&& !plugin.locketteCheck(e.getBlock().getRelative(BlockFace.DOWN), e.getPlayer())) {
				plugin.getLocale().sendMessage(e.getPlayer(), EXISTING_CHEST);
				e.setCancelled(true);
				return;
			}
			if (plugin.getPluginConfig().isAutoFillName()) {
				if(plugin.getPluginConfig().isExtendedNames()) {
					try {
						e.setLine(3, NameCollection.getSignName(e.getPlayer().getName()));
					} catch (final OutOfEntriesException ex) {
						plugin.getLogger().severe("Player " + e.getPlayer() + " cannot register extended name!");
						e.getPlayer().sendMessage("Name overflow, notify server administrator!");
						e.setCancelled(true);
					}
				} else {
					e.setLine(3, ShopHelpers.truncateName(e.getPlayer().getName()));
				}
			}
		}
	}
}
