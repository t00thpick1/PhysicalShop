package com.wolvereness.physicalshop;

import static com.wolvereness.physicalshop.config.ConfigOptions.*;

import java.util.HashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.yi.acru.bukkit.Lockette.Lockette;

import com.griefcraft.lwc.LWCPlugin;
import com.griefcraft.model.Protection;
import com.wolvereness.physicalshop.config.Localized;
import com.wolvereness.physicalshop.config.MaterialConfig;
import com.wolvereness.physicalshop.config.StandardConfig;
import com.wolvereness.util.CommandHandler;
import com.wolvereness.util.CommandHandler.Reload;
import com.wolvereness.util.CommandHandler.Verbose;
import com.wolvereness.util.CommandHandler.Verbose.Verbosable;
import com.wolvereness.util.CommandHandler.Version;
import com.wolvereness.util.NameCollection;

import de.diddiz.LogBlock.Consumer;
import de.diddiz.LogBlock.LogBlock;

/**
 *
 */
public class PhysicalShop extends JavaPlugin implements Verbosable {

	/**
	 * Command to reload PhysicalShop
	 */
	public static final String RELOAD_COMMAND = "RELOAD";
	/**
	 * Command to print verbose
	 */
	public static final String VERBOSE_COMMAND = "VERBOSE";
	/**
	 * Command to get version
	 */
	public static final String VERSION_COMMAND = "VERSION";
	private final HashMap<String,CommandHandler> commands = new HashMap<String,CommandHandler>();
	private StandardConfig configuration;
	private Consumer consumer = null;
	private final PhysicalShopListener listener = new PhysicalShopListener(this);
	private Localized locale;
	private Lockette lockette = null;
	private LWCPlugin lwc = null;
	private MaterialConfig materialConfig;
	private Permissions permissions;
	/**
	 * @return the locale
	 */
	public Localized getLocale() {
		return locale;
	}
	/**
	 * This function checks for LogBlock if not already found after plugin
	 * enabled
	 *
	 * @return LogBlock consumer
	 */
	public Consumer getLogBlock() {
		return consumer;
	}
	/**
	 * @return the MaterialConfig being used
	 */
	public MaterialConfig getMaterialConfig() {
		return materialConfig;
	}
	public Permissions getPermissionHandler() {
		return permissions;
	}
	/**
	 * Grabs the current StandardConfig being used.
	 *
	 * @return the configuration being used
	 */
	public StandardConfig getPluginConfig() {
		return configuration;
	}
	/**
	 * Method used to hook into lockette
	 * @param relative the block to consider
	 * @param player player to consider
	 * @return true if and only if lockette is enabled and player owns said block
	 */
	public boolean locketteCheck(final Block relative, final Player player) {
		return lockette != null && player.getName().equals(Lockette.getProtectedOwner(relative));
	}

	/**
	 * This function checks for LWC, thus letting player create shop over
	 * existing chest
	 *
	 * @param block
	 *            Block representing chest
	 * @param player
	 *            Player creating sign
	 * @return Returns true if LWC enabled and protection exists and player is admin of chest
	 */
	public boolean lwcCheck(final Block block, final Player player) {
		if (lwc == null) return false;
		final Protection protection = lwc.getLWC().findProtection(block);
		if (protection == null)	return false; // no protection here
		return lwc.getLWC().canAdminProtection(player, protection);
	}

	/**
	 * This will capture the only command, /physicalshop. It will send version information to the sender, and it checks permissions and reloads config if there is proper permission to.
	 * @param sender Player / Console sending command
	 * @param command ignored
	 * @param label ignored
	 * @param args ignored
	 * @return true, errors are handled manually.
	 */
	@Override
	public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
		final String subCommand = args.length == 0 ? VERSION_COMMAND : args[0].toUpperCase();
		if(commands.containsKey(subCommand))
			return commands.get(subCommand).onCommand(sender, args, getPermissionHandler());
		return false;
	}
	/**
	 * Does nothing as of yet.
	 */
	@Override
	public void onDisable() {
		if(configuration.isExtendedNames()) {
			NameCollection.unregisterPlugin(this);
		}
	}
	/**
	 * Initialization routine
	 */
	@Override
	public void onEnable() {
		try
		{
			saveConfig();
			permissions = new Permissions(this);
			//Events
			final PluginManager pm = getServer().getPluginManager();
			pm.registerEvents(listener, this);
			//Commands
			commands.put(RELOAD_COMMAND, new Reload(this));
			commands.put(VERSION_COMMAND, new Version(this,"%2$s version %1$s by Wolvereness, original by yli"));
			commands.put(VERBOSE_COMMAND, new Verbose(this));
			//Hooks
			Plugin temp = getServer().getPluginManager().getPlugin("LWC");
			if(temp != null && temp instanceof LWCPlugin) {
				lwc = (LWCPlugin) temp;
			}
			temp = getServer().getPluginManager().getPlugin("Lockette");
			if(temp != null && temp instanceof Lockette) {
				lockette = (Lockette) temp;
			}
			getLogger().info(getDescription().getFullName() + " enabled.");
		} catch (final Throwable t) {
			getLogger().log(Level.SEVERE, getDescription().getFullName() + " failed to enable", t);
			getServer().getPluginManager().disablePlugin(this);
		}
	}
	@Override
	public void onLoad() {
	}
	@Override
	@SuppressWarnings("deprecation")
	public void reloadConfig() {
		if(configuration != null && configuration.isExtendedNames()) {
			NameCollection.unregisterPlugin(this);
		}
		super.reloadConfig();
		getConfig().options().copyDefaults(true);
		if(getConfig().isSet(BUY_PATTERN)) {
			getConfig().getConfigurationSection(BUY_SECTION).set(PATTERN, getConfig().getString(BUY_PATTERN));
			getConfig().set(BUY_PATTERN, null);
		}
		if(getConfig().isSet(SELL_PATTERN)) {
			getConfig().getConfigurationSection(SELL_SECTION).set(PATTERN, getConfig().getString(SELL_PATTERN));
			getConfig().set(SELL_PATTERN, null);
		}
		configuration = new StandardConfig(this);
		if(configuration.isExtendedNames()) {
			NameCollection.registerPlugin(this);
		}
		locale = new Localized(this);
		materialConfig = new MaterialConfig(this);
		if (getConfig().getBoolean(LOG_BLOCK)) {
			final Plugin logblockPlugin = Bukkit.getServer().getPluginManager().getPlugin("LogBlock");
			if (logblockPlugin == null || !(logblockPlugin instanceof LogBlock)) {
				getLogger().warning("Failed to find LogBlock");
				consumer = null;
			} else {
				consumer = ((LogBlock) logblockPlugin).getConsumer();
				if (consumer == null) {
					getLogger().warning("Error getting LogBlock consumer");
				} else {
					getLogger().info("Sucessfully hooked into LogBlock");
				}
			}
		} else {
			consumer = null;
			getLogger().info("Did not hook into LogBlock");
		}
	}
	public void verbose(final CommandSender sender) {
		materialConfig.verbose(sender);
	}

}
