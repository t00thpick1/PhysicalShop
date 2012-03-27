package com.wolvereness.physicalshop;

import static com.wolvereness.physicalshop.config.ConfigOptions.SERVER_SHOP;
import static org.bukkit.Material.*;
import static org.bukkit.block.BlockFace.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import com.google.common.collect.ImmutableList;
import com.wolvereness.physicalshop.exception.InvalidSignException;

/**
 *
 */
public class ShopHelpers {
	/**
	 * A list of block faces, North East South West, and self
	 */
	public static final List<BlockFace> CARDINAL_DIRECTIONS = ImmutableList.of(
			SELF,
			NORTH,
			SOUTH,
			EAST,
			WEST);
	/**
	 * A list of block faces including the cardinal directions and up, down
	 */
	private static final List<BlockFace> EXTENDED_DIRECTIONS = ImmutableList.of(
			SELF,
			NORTH,
			SOUTH,
			EAST,
			WEST,
			DOWN,
			UP);
	/**
	 * Copy & Pasted from GNU GPL Licensed craftbook
	 * com.sk89q.craftbook.util.SignUtil
	 * @param sign
	 *            treated as sign post if it is such, or else assumed to be a
	 *            wall sign (i.e., if you ask about a stone block, it's
	 *            considered a wall sign).
	 * @return the blank side of the sign opposite the text. In the case of a
	 *         wall sign, the block in this direction is the block to which the
	 *         sign is attached. This is also the direction a player would be
	 *         facing when reading the sign.
	 *
	 */
	public static BlockFace getBack(final Sign sign) {
		if (sign.getType() == SIGN_POST) {
			switch (sign.getRawData()) {
			case 0x0:
				return EAST;
			case 0x1:
			case 0x2:
			case 0x3:
				return SOUTH_EAST;
			case 0x4:
				return SOUTH;
			case 0x5:
			case 0x6:
			case 0x7:
				return SOUTH_WEST;
			case 0x8:
				return WEST;
			case 0x9:
			case 0xA:
			case 0xB:
				return NORTH_WEST;
			case 0xC:
				return NORTH;
			case 0xD:
			case 0xE:
			case 0xF:
				return NORTH_EAST;
			default:
				return SELF;
			}
		}
		switch (sign.getRawData()) {
		case 0x2:
			return WEST;
		case 0x3:
			return EAST;
		case 0x4:
			return SOUTH;
		case 0x5:
			return NORTH;
		default:
			return SELF;
		}
	}
	/**
	 * Copy & Pasted from Bukkit source
	 * Gets the face that this lever or button is attached on
	 * @param data Data of the lever or button
	 * @return BlockFace attached to
	 */
	public static BlockFace getFace(final byte data) {
		switch (data & 0x7) {
		case 0x1:
			return NORTH;
		case 0x2:
			return SOUTH;
		case 0x3:
			return EAST;
		case 0x4:
			return WEST;
		case 0x5:
		case 0x6:
			return DOWN;
		}
		return null;
	}
	/**
	 * Attempts to create a new shop object based on this block
	 * @param block the block to consider
	 * @param plugin The active PhysicalShop plugin
	 * @return null if block is not sign or said sign is invalid, otherwise a new associated {@link Shop} for this block
	 */
	public static Shop getShop(final Block block, final PhysicalShop plugin) {
		if (block == null) return null;

		if (block.getType() != SIGN_POST && block.getType() != WALL_SIGN) return null;

		final Sign sign = (Sign) block.getState();

		if (sign == null) return null;

		final String ownerName = Shop.getOwnerName(sign.getLines());

		try {
			if (block.getRelative(DOWN).getType() == CHEST) return new ChestShop(sign, plugin);
			else if (ownerName.equalsIgnoreCase(plugin.getConfig().getString(SERVER_SHOP))) return new Shop(sign, plugin);
			else return null;
		} catch (final InvalidSignException e) {
			return null;
		}
	}
	/**
	 * Adds the shops associated with the specified block to the provided collection
	 * @param block The block to check around
	 * @param plugin The currently active PhysicalShop plugin
	 * @param shops The collection to add to
	 * @return the provided collection
	 */
	public static Collection<Shop> getShops(final Block block, final PhysicalShop plugin, final Collection<Shop> shops) {
		Validate.notNull(shops, "Must provide a collection to add result to");
		for (final BlockFace face : EXTENDED_DIRECTIONS) {
			final Shop shop = getShop(block.getRelative(face), plugin);

			if (shop != null && shop.isShopBlock(block)) {
				shops.add(shop);
			}
		}
		return shops;
	}
	/**
	 * Adds the shops associated with the specified blocks to the provided collection
	 * @param blocks The blocks to check around
	 * @param plugin The currently active PhysicalShop plugin
	 * @param shops The set to add to
	 * @return the provided collection
	 */
	public static Collection<Shop> getShops(final Collection<Block> blocks, final PhysicalShop plugin, final Set<Shop> shops) {
		for (final Block block : blocks) {
			getShops(block, plugin, shops);
		}
		return shops;
	}
	/**
	 * This assumes player does NOT have admin access
	 * @param player Player to check for access
	 * @param block Block to check for access
	 * @param plugin The current instance of PhysicalShop
	 * @return true if the player has permission for the shop
	 */
	public static boolean hasAccess(final Player player, final Block block, final PhysicalShop plugin) {
		return hasAccess(player.getName(), getShop(block, plugin), plugin);
	}
	/**
	 * This assumes player does NOT have admin access
	 * @param player Player to check for access
	 * @param shop Shop to check for access
	 * @param plugin The current instance of PhysicalShop
	 * @return true if the player has permission for the shop
	 */
	public static boolean hasAccess(final String player, final Shop shop, final PhysicalShop plugin) {
		return shop == null || (
			!plugin.getConfig().getString(SERVER_SHOP).equals(shop.getOwnerName())
			&& shop.isSmartOwner(player, plugin)
			);
	}
    /**
	 * This method checks a block for shop protection for other chests near or that chest
	 * @param block Block to chest, intended to be a chest
	 * @param player Player to cross-check for permissions
	 * @param plugin currently active PhysicalShop to consider
	 * @return true if the player should be blocked
	 */
	public static boolean isProtectedChestsAround(final Block block, final Player player, final PhysicalShop plugin) {
		for (final BlockFace blockFace : CARDINAL_DIRECTIONS) {
			final Block checkBlock = block.getRelative(blockFace);
			if(checkBlock.getType() == CHEST && !hasAccess(player, checkBlock.getRelative(UP), plugin)) return true;
		}
		return false;
	}
	/**
	 * Checks a list of shops against player for ownership.<br>
	 * Assumes block ARE protected.
	 * @param shops The shops being destroyed
	 * @param player The player destroying block, can be null (as in, no destroyer)
	 * @param plugin The active PhysicalShop plugin
	 * @return false if there are shops and player is null or not admin and not owner
	 */
	public static boolean isShopsDestroyable(
			final Collection<Shop> shops,
			final Player player,
			final PhysicalShop plugin) {
		if(shops.isEmpty()) return true;
		if(player == null) return false;
		if(plugin.getPermissionHandler().hasAdmin(player)) return true;
		for (final Shop shop : shops) {
			if (!hasAccess(player.getName(), shop, plugin)) {
				shop.getSign().update();
				return false;
			}
		}
		return true;
	}
	/**
	 * Cuts the name to 15 characters
	 * @param name name to truncate
	 * @return the first 15 characters of the name
	 */
	public static String truncateName(final String name) {
		if(name == null) return null;
		if(name.length()<=15) return name;
		return name.substring(0, 15);
	}
}
