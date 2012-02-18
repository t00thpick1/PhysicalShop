package com.wolvereness.physicalshop.config;

import static com.wolvereness.physicalshop.config.ConfigOptions.*;

import java.util.regex.Pattern;

import org.bukkit.plugin.Plugin;

//"Buy (?=\\d{1,4})|(?<=\\d{1,4}) for (?=\\d{1,4})|(?<= for \\d{1,4})(?=\\D)"
//Inferior
//"(?<=Buy )\\d{1,4}(?= for \\d{1,4}\\D)|(?<=Buy \\d{1,4} for )\\d{1,4}(?=\\D)|(?<=Buy \\d{1,4} for \\d{1,4})\\D"

/**
 *
 */
public class StandardConfig {
	private final Pattern buyPattern;
	private final Pattern materialPattern;
	private final Plugin plugin;
	private final Pattern sellPattern;
	/**
	 * makes a new standard config, loading up defaults
	 * @param plugin Used to get the config
	 */
	public StandardConfig(final Plugin plugin) {
		this.plugin = plugin;
		buyPattern = Pattern.compile(plugin.getConfig().getString(BUY_PATTERN));
		materialPattern = Pattern.compile(plugin.getConfig().getString(MATERIAL_PATTERN));
		sellPattern = Pattern.compile(plugin.getConfig().getString(SELL_PATTERN));
		if(!plugin.getConfig().isConfigurationSection(CURRENCIES)) {
			plugin.getConfig().set(CURRENCIES + ".g", "Gold Ingot");;
		}
	}
	/**
	 * Pattern for 'buy-from-shop' (second line on signs).
	 * "buy-pattern"
	 *
	 * @return the pattern splitting the Buy line
	 */
	public Pattern getBuyPattern() {
		return buyPattern;
	}
	/**
	 * Pattern for material match (first line on signs)
	 *
	 * @return the pattern matching the Material line
	 */
	public Pattern getMaterialPattern() {
		return materialPattern;
	}
	/**
	 * Pattern for 'sell-to-shop' (third line on signs).
	 * "sell-pattern"
	 *
	 * @return the pattern splitting the Sell line
	 */
	public Pattern getSellPattern() {
		return sellPattern;
	}
	/**
	 * Checks config to get the 'auto-fill-name' setting.
	 *
	 * @return if names should be auto-set
	 */
	public boolean isAutoFillName() {
		return plugin.getConfig().getBoolean(AUTO_FILL_NAME, true);
	}
	/**
	 * Checks config to see if 'protect-existing-chest' is set
	 *
	 * @return true
	 */
	public boolean isExistingChestProtected() {
		return plugin.getConfig().getBoolean(PROTECT_EXISTING_CHEST, true);
	}
	/**
	 * Checks config to see if extended names are enabled (only matters if auto-fill is on)
	 * @return true if service is enabled
	 */
	public boolean isExtendedNames() {
		return plugin.getConfig().getBoolean(AUTO_FILL_NAME, true)
			&& plugin.getConfig().getBoolean(EXTENDED_NAMES);
	}
	/**
	 * Checks config to get the 'protect-break' setting.
	 *
	 * @return the config option for protection chest breaking
	 */
	public boolean isProtectBreak() {
		return plugin.getConfig().getBoolean(PROTECT_BREAK, true);
	}
	/**
	 * Checks config to get the 'protect-chest-access' setting.
	 *
	 * @return the config option for protecting chest access
	 */
	public boolean isProtectChestAccess() {
		return plugin.getConfig().getBoolean(PROTECT_CHEST_ACCESS, true);
	}
	/**
	 * Checks config to get the 'protect-explode' setting.
	 *
	 * @return the config option for protecting chests from explosions
	 */
	public boolean isProtectExplode() {
		return plugin.getConfig().getBoolean(PROTECT_EXPLODE, true);
	}
}
