package me.zonalyewhd.commandmobs;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.inventory.ItemStack;

public class EnchantGlow extends EnchantmentWrapper {

	public EnchantGlow(int id) {
		super(id);
	}

	public boolean canEnchantItem(ItemStack item) {
		return true;
	}

	public boolean conflictsWith(Enchantment other) {
		return false;
	}

	public EnchantmentTarget getItemTarget() {
		return null;
	}

	public int getMaxLevel() {
		return 10;
	}

	public String getName() {
		return "Glow";
	}

	public int getStartLevel() {
		return 1;
	}

}
