package com.wolvereness.physicalshop;

import java.util.Map;

import org.bukkit.CoalType;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.TreeSpecies;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Coal;
import org.bukkit.material.Dye;
import org.bukkit.material.Leaves;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Step;
import org.bukkit.material.Tree;
import org.bukkit.material.Wool;

import com.wolvereness.physicalshop.config.MaterialConfig;
import com.wolvereness.physicalshop.exception.InvalidMaterialException;
/**
 *
 */
public class ShopMaterial {
	private static String toHumanReadableString(final Object object) {
		final StringBuilder sb = new StringBuilder();

		for (final String word : object.toString().split("_")) {
			sb.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase()).append(' ');
		}

		sb.deleteCharAt(sb.length() - 1);

		return sb.toString();
	}
	private final short durability;
	private final Map<Enchantment, Integer> enchantment;
	private int hash = 0;
	private final Material material;
	/**
	 * @param itemStack items to derive this material from
	 */
	public ShopMaterial(final ItemStack itemStack) {
		this(itemStack.getType(), itemStack.getDurability(), itemStack.getEnchantments());
	}
	/**
	 * @param material bukkit material to reference
	 * @param durability durability to reference
	 * @param enchantment enchantment to reference
	 */
	public ShopMaterial(
	                    final Material material,
	                    final short durability,
	                    final Map<Enchantment,Integer> enchantment) {
		this.material = material;
		this.durability = durability;
		this.enchantment = enchantment == null ? null : enchantment.isEmpty() ? null : enchantment;
	}
	/**
	 * @param string input string
	 * @throws InvalidMaterialException if material invalid
	 */
	public ShopMaterial(final String string) throws InvalidMaterialException {
		enchantment = null;
		final String[] strings = string.split(":");

		if (strings.length == 2) {
			material = Material.matchMaterial(strings[0]);
			durability = Short.parseShort(strings[1]);
			return;
		}

		Material material = null;

		for (int i = 0; i < string.length(); ++i) {
			if ((i == 0) || (string.charAt(i) == ' ')) {
				material = Material.matchMaterial(string.substring(i).trim());

				if (material != null) {
					this.material = material;
					durability = parseDurability(string.substring(0, i).trim(), material);
					return;
				}
			}
		}

		throw new InvalidMaterialException();
	}
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof ShopMaterial)) return false;
		final ShopMaterial other = (ShopMaterial) obj;
		if (durability != other.durability) return false;
		if (material != other.material) return false;
		if (enchantment == other.enchantment) return true;
		if (enchantment == null) return false;
		if (!enchantment.equals(other.enchantment)) return false;
		return true;
	}
	/**
	 * @return the durability for this material
	 */
	public short getDurability() {
		return durability;
	}
	/**
	 * @return the bukkit material for this material
	 */
	public Material getMaterial() {
		return material;
	}
	/**
	 * @param amount size to set the stack to
	 * @return an item stack representing this material
	 */
	public ItemStack getStack(final int amount) {
		if(amount == 0) return null;
		final ItemStack stack = new ItemStack(getMaterial(), amount,getDurability());
		if(enchantment == null) return stack;
		stack.addUnsafeEnchantments(enchantment);
		return stack;
	}
	@Override
	public int hashCode() {
		if(hash == 0) return hash  = new Integer(material.getId() << 16 + durability).hashCode();
		return hash;
	}

	@SuppressWarnings("javadoc")
	@Deprecated
	public Short parseDurability(final String string,final Material material) {
		try {
			return Short.parseShort(string);
		} catch (final NumberFormatException e) {
		}

		final String s = string.replace(' ', '_').toUpperCase();
		MaterialData data = null;

		try {
			switch (material) {
			case COAL:
				data = new Coal(CoalType.valueOf(s));
				break;
			case LOG:
				data = new Tree(TreeSpecies.valueOf(s));
				break;
			case LEAVES:
				data = new Leaves(TreeSpecies.valueOf(s));
				break;
			case STEP:
			case DOUBLE_STEP:
				data = new Step(Material.valueOf(s));
				break;
			case INK_SACK:
				data = new Dye();
				((Dye) data).setColor(DyeColor.valueOf(s));
				break;
			case WOOL:
				data = new Wool(DyeColor.valueOf(s));
				break;
			}
		} catch (final IllegalArgumentException e) {
		}

		return data == null ? 0 : (short) data.getData();
	}//*/

	@Override
	public String toString() {
		return ShopMaterial.toHumanReadableString(toStringDefault(new StringBuilder()).toString());
	}

	/**
	 * @param materialConfig the material config to consider
	 * @return an appropriate string representing this shop material
	 */
	public String toString(final MaterialConfig materialConfig) {
		return materialConfig.isConfigured(this) ? materialConfig.toString(this) : toString();
	}
	/**
	 * Adds information to
	 * @param sb StringBuilder being used
	 * @return the StringBuilder used
	 */
	public StringBuilder toStringDefault(final StringBuilder sb) {
		switch (material) {
		case COAL:
			sb.append(new Coal(material, (byte) durability).getType().toString());
			return sb;
		case LOG:
			sb.append(new Tree(material, (byte) durability).getSpecies().toString()).append('_');
			break;
		case LEAVES:
			sb.append(new Leaves(material, (byte) durability).getSpecies().toString()).append('_');
			break;
		case STEP:
		case DOUBLE_STEP:
			sb.append(new Step(material, (byte) durability).getMaterial().toString()).append('_');
			break;
		case INK_SACK:
			sb.append(new Dye(material, (byte) durability).getColor().toString()).append('_');
			break;
		case WOOL:
			sb.append(new Wool(material, (byte) durability).getColor().toString()).append('_');
			break;
		}
		return sb.append(material.toString());
	}
}
