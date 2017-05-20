package me.zonalyewhd.commandmobs;

import java.lang.reflect.Field;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;

@SuppressWarnings("deprecation")
public class EnchantmentManager {

	private static Enchantment e;

	public static void setup() {
		if (Enchantment.getById(CommandMobs.config().getInt("settings.enchantment-id")) == null) {
			try {
				Field f = Enchantment.class.getDeclaredField("acceptingNew");
				f.setAccessible(true);
				f.set(null, true);
			} catch (Exception e) {
				e.printStackTrace();
			}
			e = new EnchantGlow(CommandMobs.config().getInt("settings.enchantment-id"));
			try {
				EnchantmentWrapper.registerEnchantment(e);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
	}

	public static Enchantment getBlankEnchantment() {
		return Enchantment.getById(CommandMobs.config().getInt("settings.enchantment-id"));
	}

}
