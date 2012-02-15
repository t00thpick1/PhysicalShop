package com.wolvereness.physicalshop;


import static com.wolvereness.physicalshop.config.ConfigOptions.TRIGGER_REDSTONE;
import static com.wolvereness.physicalshop.config.Localized.Message.BUY;
import static com.wolvereness.physicalshop.config.Localized.Message.BUY_RATE;
import static com.wolvereness.physicalshop.config.Localized.Message.NOT_ENOUGH_PLAYER_ITEMS;
import static com.wolvereness.physicalshop.config.Localized.Message.NOT_ENOUGH_PLAYER_MONEY;
import static com.wolvereness.physicalshop.config.Localized.Message.NO_BUY;
import static com.wolvereness.physicalshop.config.Localized.Message.NO_SELL;
import static com.wolvereness.physicalshop.config.Localized.Message.PLAYER_INVENTORY_FULL;
import static com.wolvereness.physicalshop.config.Localized.Message.SELL;
import static com.wolvereness.physicalshop.config.Localized.Message.SELL_RATE;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import com.wolvereness.physicalshop.config.MaterialConfig;
import com.wolvereness.physicalshop.exception.InvalidExchangeException;
import com.wolvereness.physicalshop.exception.InvalidMaterialException;
import com.wolvereness.physicalshop.exception.InvalidSignException;
import com.wolvereness.physicalshop.exception.InvalidSignOwnerException;

/**
 *
 */
public class Shop {
	/**
	 * Figures out the current material the shop uses.
	 * @param lines text from sign
	 * @param config The MaterialConfig to use
	 * @return an associated shop material, or null if failed to decypher
	 */
	public static ShopMaterial getMaterial(final String[] lines, final MaterialConfig config) {
		try {
			return config.getShopMaterial(lines[0]);
		} catch (final InvalidMaterialException e) {
			return null;
		}
	}
	/**
	 * Owner is found on fourth line of sign. This will NOT cross-check for extended player names!
	 * @param lines The set of lines associated with the sign for a shop.
	 * @return name of the owner of said shop
	 */
	public static String getOwnerName(final String[] lines) {
		return lines[3];
	}
	private static Rate getRate(final String amount, final String price) {
		try
		{
			return new Rate(Integer.parseInt(amount), Integer.parseInt(price));
		} catch (final NumberFormatException e) {
			return null;
		}
	}
	@SuppressWarnings("deprecation")
	private static void updateInventory(final Player player) {
		player.updateInventory();
	}
	private final ShopMaterial buyCurrency;
	private final Rate buyRate;
	private final ShopMaterial material;
	private final String ownerName;
	private final ShopMaterial sellCurrency;
	private final Rate sellRate;
	private Sign sign = null;
	/**
	 * Initializes Shop based off of sign.
	 * @param sign the sign to consider
	 * @param plugin The active PhysicalShop plugin
	 * @throws InvalidSignException If sign does not match correct pattern.
	 */
	public Shop(final Sign sign, final PhysicalShop plugin) throws InvalidSignException {
		this(sign.getLines(), plugin);
		this.sign = sign;
	}
	/**
	 * Initializes a shop based off the lines from a sign. Used to check validity.
	 * @param lines the text from the sign to consider
	 * @param plugin The active PhysicalShop plugin
	 * @throws InvalidSignException If the sign text does not match correct pattern.
	 */
	public Shop(final String[] lines, final PhysicalShop plugin) throws InvalidSignException {
		//String[] lines = sign.getLines();
		material = Shop.getMaterial(lines, plugin.getMaterialConfig());

		if (material == null) throw new InvalidSignException();

		final String[] buySet = plugin.getPluginConfig().getBuyPattern().split(lines[1]);
		final String[] sellSet = plugin.getPluginConfig().getSellPattern().split(lines[2]);

		if (buySet.length != 4 && sellSet.length != 4) throw new InvalidSignException();

		Rate buyRate = null, sellRate = null;
		ShopMaterial buyCurrency = null, sellCurrency = null;

		try
		{
			if(buySet.length == 4)
			{
				buyCurrency = plugin.getMaterialConfig().getCurrency(buySet[3].charAt(0));
				buyRate = getRate(buySet[1], buySet[2]);
			}
		} catch (final InvalidSignException e) {}

		try
		{
			if(sellSet.length == 4)
			{
				sellCurrency = plugin.getMaterialConfig().getCurrency(sellSet[3].charAt(0));
				sellRate =  getRate(sellSet[1], sellSet[2]);
			}
		} catch (final InvalidSignException e) {}

		if (sellCurrency == null && buyCurrency == null) throw new InvalidSignException();

		this.buyCurrency = buyCurrency;
		this.sellCurrency = sellCurrency;
		this.buyRate = buyRate;
		this.sellRate = sellRate;

		if (((this.ownerName = lines[3]) == null) || ownerName.length() == 0) throw new InvalidSignOwnerException();
	}
	/**
	 * Invokes the buy routine for player.
	 * @param player player purchasing
	 * @param plugin The active PhysicalShop plugin
	 * @return true if success
	 */
	protected boolean buy(final Player player, final PhysicalShop plugin) {
		if (!canBuy()) {
			plugin.getLocale().sendMessage(player, NO_BUY);
			return false;
		}

		final Inventory inventory = player.getInventory();

		final int price = getBuyRate().getPrice();
		final int amount = getBuyRate().getAmount();

		try {
			InventoryHelpers.exchange(inventory, material.getStack(amount), getBuyCurrency().getStack(price));
		} catch (final InvalidExchangeException e) {
			switch (e.getType()) {
			case ADD:
				plugin.getLocale().sendMessage(player, PLAYER_INVENTORY_FULL);
				break;
			case REMOVE:
				plugin.getLocale().sendMessage(player, NOT_ENOUGH_PLAYER_MONEY, getBuyCurrency().toString(plugin.getMaterialConfig()));
				break;
			}

			return false;
		}

		plugin.getLocale().sendMessage(
			player,
			BUY,
			amount,
			material.toString(plugin.getMaterialConfig()),
			price,
			getBuyCurrency().toString(plugin.getMaterialConfig())
			);
		updateInventory(player);

		queryLogBlock(player, false, plugin);
		return true;
	}
	/**
	 * @return true if and only if this shop supports buying
	 */
	public boolean canBuy() {
		return buyRate != null;
	}
	/**
	 * This checks player for permission to destroy this shop
	 * @param player player to consider
	 * @param plugin The active PhysicalShop plugin
	 * @return true if and only if player may destroy this shop
	 */
	public boolean canDestroy(final Player player, final PhysicalShop plugin) {
		return (player != null)
				&& plugin.getPermissionHandler().hasAdmin(player);
	}
	/**
	 * @return true if and only if this shop supports selling
	 */
	public boolean canSell() {
		return sellRate != null;
	}
	/**
	 * @return the currency associated with buying
	 */
	public ShopMaterial getBuyCurrency() {
		return buyCurrency;
	}
	/**
	 * @return the rate associated with buying
	 */
	public Rate getBuyRate() {
		return buyRate;
	}
	/**
	 * @return the material associated with this shop
	 */
	public ShopMaterial getMaterial() {
		return material;
	}

	/**
	 * @return the owner of this shop
	 */
	public String getOwnerName() {
		return ownerName;
	}

	/**
	 * @return the currency associated with selling
	 */
	public ShopMaterial getSellCurrency() {
		return sellCurrency;
	}

	/**
	 * @return the rate associated with selling
	 */
	public Rate getSellRate() {
		return sellRate;
	}
	/**
	 * Gets the current amount of shop's buying currency in the chest.
	 *
	 * @return an amount of currency in this shop from purchases
	 */
	public int getShopBuyCapital() {
		return Integer.MAX_VALUE;
	}
	/**
	 * @return the amount of the shop's material currently stored
	 */
	public int getShopItems() {
		return Integer.MAX_VALUE;
	}

	/**
	 * Gets the current amount of shop's selling currency in the chest.
	 *
	 * @return the amount of currency in this shop for selling
	 */
	public int getShopSellCapital() {
		return Integer.MAX_VALUE;
	}
	/**
	 * @return the associated sign
	 */
	public Sign getSign() {
		return sign;
	}
	/**
	 * This method is called when a player right-clicks the sign. It considers the item in player's hand, and will act accordingly.
	 * @param player the player to consider
	 * @param plugin The active PhysicalShop plugin
	 */
	public void interact(final Player player, final PhysicalShop plugin) {
		final ShopMaterial item = new ShopMaterial(player.getItemInHand());
		try {
		if (item.equals(getBuyCurrency())) {
			if(buy(player, plugin)) {
				triggerRedstone(plugin);
			}
		} else if (item.equals(material)) {
			if(sell(player, plugin)) {
				triggerRedstone(plugin);
			}
		}
		//*
		} catch (final RuntimeException t) {
			t.printStackTrace();
			throw t;
		} catch (final Error t) {
			t.printStackTrace();
			throw t;
		}
		//*/
	}
	/**
	 * @param block block to consider
	 * @return true if said block is the sign for this chest or the sign for this shop is attached to said block
	 */
	public boolean isShopBlock(final Block block) {
		final Block signBlock = sign.getBlock();

		if (block.equals(signBlock)) return true;

		final org.bukkit.material.Sign signData = (org.bukkit.material.Sign) sign
				.getData();

		return block.equals(signBlock.getRelative(signData.getAttachedFace()));
	}
	private void queryLogBlock(final Player player, final boolean selling, final PhysicalShop plugin) {
		if (plugin.getLogBlock() == null) return;
		final Location chestLocation = sign.getBlock().getRelative(BlockFace.DOWN).getLocation();
		final short currencyDeposited = (short) (selling ? -getSellRate().getPrice() : getBuyRate().getPrice());
		final short materialDeposited = (short) (selling ? getSellRate().getAmount() : -getBuyRate().getAmount());
		if(currencyDeposited != 0) {
			plugin
				.getLogBlock()
				.queueChestAccess(
					player
						.getName(),
					chestLocation,
					54,
					(short) (
						selling
							? getSellCurrency()
							: getBuyCurrency()
						)
						.getMaterial()
						.getId(),
					currencyDeposited,
					(byte) 0
					);
		}
		if (materialDeposited != 0 ) {
			plugin
				.getLogBlock()
				.queueChestAccess(
					player
						.getName(),
					chestLocation,
					54,
					(short) getMaterial()
						.getMaterial()
						.getId(),
					materialDeposited,
					(byte) 0
					);
		}
	}
	/**
	 * performs sell operation for player
	 * @param player player to sell something to shop
	 * @param plugin The active PhysicalShop plugin
	 * @return true if successful
	 */
	protected boolean sell(final Player player, final PhysicalShop plugin) {
		if (!canSell()) {
			plugin.getLocale().sendMessage(player, NO_SELL);
			return false;
		}

		final Inventory inventory = player.getInventory();

		final int price = getSellRate().getPrice();
		final int amount = getSellRate().getAmount();

		try {
			InventoryHelpers.exchange(inventory, getSellCurrency()
					.getStack(price), material.getStack(amount));
		} catch (final InvalidExchangeException e) {
			switch (e.getType()) {
			case ADD:
				plugin.getLocale().sendMessage(player, PLAYER_INVENTORY_FULL);
				break;
			case REMOVE:
				plugin.getLocale().sendMessage(player, NOT_ENOUGH_PLAYER_ITEMS, material.toString(plugin.getMaterialConfig()));
				break;
			}

			return false;
		}

		updateInventory(player); // player.updateInventory();

		plugin.getLocale().sendMessage(
			player,
			SELL,
			amount,
			material,
			price,
			getSellCurrency()
			);

		queryLogBlock(player, true, plugin);
		return true;
	}
	/**
	 * Messages player p the rates for current Shop.
	 * @param p the player to message
	 * @param plugin The active PhysicalShop plugin
	 */
	public void status(final Player p, final PhysicalShop plugin) {
		if (canBuy() && (getShopItems() >= buyRate.getAmount())) {
			plugin.getLocale().sendMessage(
				p,
				BUY_RATE,
				buyRate.getAmount(),
				material.toString(plugin.getMaterialConfig()),
				buyRate.getPrice(),
				getBuyCurrency().toString(plugin.getMaterialConfig())
				);
		}

		if (canSell() && (getShopSellCapital() >= sellRate.getPrice())) {
			plugin.getLocale().sendMessage(
				p,
				SELL_RATE,
				sellRate.getAmount(),
				material.toString(plugin.getMaterialConfig()),
				sellRate.getPrice(),
				getSellCurrency().toString(plugin.getMaterialConfig())
				);
		}
	}
	private void triggerRedstone(final PhysicalShop plugin) {
		if(!plugin.getConfig().getBoolean(TRIGGER_REDSTONE)) return;
		final BlockFace face = ShopHelpers.getBack(sign);
		switch(face) {
			case NORTH:
			case SOUTH:
			case WEST:
			case EAST:
				break;
			default:
				return;
		}
		final Block signBlock = sign.getBlock().getRelative(face);
		final Block activatedBlock = signBlock.getRelative(face);
		final Material type = activatedBlock.getType();

		if(type != Material.LEVER && type != Material.STONE_BUTTON) return;
		if(ShopHelpers.getFace(activatedBlock.getData()) != face.getOppositeFace()) return;
		if(!(activatedBlock.getChunk() instanceof CraftChunk)) return;

		final CraftChunk chunk = (CraftChunk) activatedBlock.getChunk();

		net.minecraft.server.Block
			.byId[chunk.getHandle().world.getTypeId(
				activatedBlock.getX(),
				activatedBlock.getY(),
				activatedBlock.getZ()
				)
			].interact(
				chunk.getHandle().world,
				activatedBlock.getX(),
				activatedBlock.getY(),
				activatedBlock.getZ(),
				null
				);
		// This is Notch code for toggling something.
		// This means I wont need to toggle the button back myself!
	}
}
