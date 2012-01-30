package com.wolvereness.physicalshop.listeners;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

import com.wolvereness.physicalshop.PhysicalShop;
import com.wolvereness.physicalshop.ShopHelpers;

/**
 *
 */
public class PhysicalShopEntityListener implements Listener {

	/**
	 * Entity Explode event
	 * @param e Event
	 */
	@EventHandler
	public void onEntityExplode(final EntityExplodeEvent e) {
		if (e.isCancelled()) return;

		// Messaging.save(null);

		if (!PhysicalShop.getPluginConfig().isProtectExplode()) return;

		for (final Block block : e.blockList()) {
			if (!ShopHelpers.isBlockDestroyable(block, null)) {
				e.setCancelled(true);
				return;
			}
		}
	}

}
