package com.wolvereness.physicalshop.config;

import static com.wolvereness.physicalshop.config.ConfigOptions.LANGUAGE;
import static java.util.logging.Level.SEVERE;
import static org.bukkit.configuration.file.YamlConfiguration.loadConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * @author Wolfe
 * Licensed under GNU GPL v3
 */
public class Localized {
	/**
	 * @author Wolfe
	 * Represents all the messages that can be sent
	 */
	@SuppressWarnings("javadoc")
	public enum Message {
		/**
		 * Amount<br>
		 * Material<br>
		 * Price<br>
		 * Currency<br>
		 */
		BUY,
		/**
		 * Amount<br>
		 * Material<br>
		 * Price<br>
		 * Currency
		 */
		BUY_RATE,
		CANT_BUILD,
		CANT_BUILD_SERVER,
		CANT_DESTROY,
		CANT_PLACE_CHEST,
		CANT_USE,
		CANT_USE_CHEST,
		CHEST_INVENTORY_FULL,
		EXISTING_CHEST,
		NO_BUY,
		NO_SELL,
		/**
		 * Material
		 */
		NOT_ENOUGH_PLAYER_ITEMS,
		/**
		 * Currency
		 */
		NOT_ENOUGH_PLAYER_MONEY,
		/**
		 * Item type
		 */
		NOT_ENOUGH_SHOP_ITEMS,
		/**
		 * Shop currency type
		 */
		NOT_ENOUGH_SHOP_MONEY,
		PLAYER_INVENTORY_FULL,
		/**
		 * Amount<br>
		 * Material<br>
		 * Price<br>
		 * Currency<br>
		 */
		SELL,
		SELL_RATE,
		/**
		 * Shop Buy currency amount<br>
		 * Shop Buy currency type<br>
		 * Shop Sell currency amount<br>
		 * Shop Sell currency type<br>
		 * Shop item amount<br>
		 * Shop item type
		 */
		STATUS,
		/**
		 * Shop currency amount<br>
		 * Shop currency type<br>
		 * Shop item amount<br>
		 * Shop item type
		 */
		STATUS_ONE_CURRENCY
	}
	/**
	 * Regex to find the & symbols to be replaced
	 */
	public static Pattern colorReplace = Pattern.compile("&(?=[0-9a-f])");
	private final YamlConfiguration config;
	private final Logger logger;
	/**
	 * @param plugin plugin to consider for getting resources
	 */
	public Localized(final Plugin plugin) {
		this.logger = plugin.getLogger();
		final String language = String.valueOf(plugin.getConfig().get(LANGUAGE)).toUpperCase();
		final File file = new File(plugin.getDataFolder(),"Locales" + File.separatorChar +  language + ".yml");
		if(file.exists()) {
			config = loadConfiguration(file);
		} else {
			config = new YamlConfiguration();
		}
		final InputStream resource = plugin.getResource("Locales/" + language + ".yml");
		if(resource == null) {
			config.addDefaults(loadConfiguration(plugin.getResource("Locales/ENGLISH.yml")));
		} else {
			config.addDefaults(loadConfiguration(resource));
		}
		config.options().copyDefaults(true);
		try {
			config.save(file);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * @param message message to get
	 * @return message stored
	 */
	public String getMessage(final Message message) {
		final Object string = config.get(message.name());
		return string == null ? null : colorReplace.matcher(String.valueOf(string)).replaceAll("\u00A7");
	}
	/**
	 * Sends the recipient a formatted message located at the node defined
	 * @param recipient Player to receive the message
	 * @param message Message to send
	 * @param args The arguments to String.format
	 */
	public void sendMessage(final CommandSender recipient, final Message message, final Object...args) {
		final Object string = config.get(message.name());
		if(string == null) {
			recipient.sendMessage("ERROR_"+message);
			logger.log(SEVERE,"Unknown message:" + message + " name:" + message.name(), new Exception());
		} else {
			recipient.sendMessage(colorReplace.matcher(String.format(String.valueOf(string), args)).replaceAll("\u00A7"));
		}
	}
}
