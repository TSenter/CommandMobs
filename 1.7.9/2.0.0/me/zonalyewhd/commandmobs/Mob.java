package me.zonalyewhd.commandmobs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.util.com.google.common.collect.Maps;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.google.common.collect.Lists;

@SuppressWarnings("deprecation")
public class Mob {

	public static final int MAX_COMMANDS = 36;
	public static final int MAX_MESSAGES = 36;

	protected static Map<UUID, Mob> MOBS = Maps.newHashMap();
	protected static List<Mob> SCROLLING_MOBS = Lists.newArrayList();

	private static String defaultName = CommandMobs.config().getString(
			"settings.defaults.name");
	private static boolean defaultNameDisplays = CommandMobs.config()
			.getBoolean("settings.defaults.name-displays");
	private static boolean defaultMovement = CommandMobs.config().getBoolean(
			"settings.defaults.movement");
	private static int defaultPrice = CommandMobs.config().getInt(
			"settings.defaults.price");
	private static boolean defaultKillable = CommandMobs.config().getBoolean(
			"settings.defaults.killable");

	private UUID id;
	private Map<String, Object> properties;

	private Mob(UUID id, UUID registrar) {
		this.id = id;
		properties = new HashMap<String, Object>();
		properties.put("REGISTRAR", registrar);
		properties.put("COMMANDS",
				e("say Hello! I am a CommandMob executing a command for %n!"));
		properties.put("MESSAGES", e("&bI am a CommandMob!"));
		setDisplayName(defaultName);
		setNameDisplays(defaultNameDisplays);
		setPrice(defaultPrice);
		setCanMove(defaultMovement);
		setKillable(defaultKillable);

		MOBS.put(id, this);
	}

	private Mob(UUID id, UUID registrar, String displayName, int price,
			ArrayList<String> commands, ArrayList<String> messages,
			boolean canMove, boolean killable, boolean nameDisplays,
			boolean scrolls) {
		this.id = id;
		properties = new HashMap<String, Object>();
		properties.put("REGISTRAR", registrar);
		if (scrolls)
			properties.put(
					"SCROLL",
					new Scroller(b(displayName), CommandMobs.config().getInt(
							"settings.mobs.scroll-length"), (int) Math
							.ceil(.25 * CommandMobs.config().getInt(
									"settings.mobs.scroll-length")), '&'));
		if (scrolls)
			SCROLLING_MOBS.add(this);
		properties.put("COMMANDS", commands);
		properties.put("MESSAGES", messages);
		setDisplayName(displayName);
		setPrice(price);
		setCanMove(canMove);
		setKillable(killable);
		setNameDisplays(nameDisplays);

		MOBS.put(id, this);
	}

	public static Mob getMob(UUID uuid) {
		if (MOBS.containsKey(uuid)) {
			// System.out.println("Found a mob in the Map!");
			return MOBS.get(uuid);
		} else if (uuid == null) {
			// System.out.println("UUID is null!");
			return null;
		} else if (a(uuid) == null) {
			// System.out.println("A mob with that UUID is null!");
			return null;
		} else if (CommandMobs.config().contains(
				"mobs." + a(uuid).getType().getName().toLowerCase() + "."
						+ uuid.toString())) {
			// System.out.println("Loading that mob!");
			return load(a(uuid));
		}
		// System.out.println("Nothin'");
		return null;
	}

	public static boolean isMob(UUID uuid) {
		for (UUID mob : MOBS.keySet())
			if (mob == uuid)
				return true;
		return false;
	}

	private static LivingEntity a(UUID id) {
		for (World w : Bukkit.getWorlds())
			for (Entity e : w.getEntities()) {
				if ((e.getUniqueId() == id))
					return (LivingEntity) e;
			}
		return null;
	}

	private static String b(String displayName) {
		int k = -1;
		char[] a = displayName.toCharArray();
		for (int i = 0; i < a.length; i++) {
			if (a[i] == '%' && a[i + 1] == 's') {
				k = i + 2;
			}
		}
		return displayName.substring(k);
	}

	protected static String c(String displayName) {
		int k = -1;
		char[] a = displayName.toCharArray();
		for (int i = 0; i < a.length; i++) {
			if (a[i] == '%' && a[i + 1] == 's') {
				k = i;
			}
		}
		return displayName.substring(0, k);
	}

	private static ArrayList<String> d(List<String> list) {
		ArrayList<String> a = new ArrayList<String>();

		a.addAll(list);

		return a;
	}

	private static ArrayList<String> e(String... strings) {
		ArrayList<String> a = new ArrayList<String>(strings.length);
		for (String s : strings)
			a.add(s);
		return a;
	}

	public UUID getUUID() {
		return id;
	}

	public LivingEntity getEntity() {
		return a(id);
	}

	public String getDisplayName() {
		return (String) properties.get("NAME");
	}

	public void setDisplayName(String name) {
		if (name == null || name.equalsIgnoreCase("%x")) {
			properties.put("NAME", "%x");
			// a(id).setCustomNameVisible(false);
			a(id).setCustomName(null);
			return;
		}
		// if (getDisplayName() != null && getDisplayName().contains("%s")
		// && !name.contains("%s")) {
		// SCROLLING_MOBS.remove(this);
		// properties.remove("SCROLL");
		// }
		// if (getDisplayName() != null && !getDisplayName().contains("%s")
		// && name.contains("%s")) {
		// SCROLLING_MOBS.add(this);
		// properties.put(
		// "SCROLL",
		// new Scroller(b(name), CommandMobs.config().getInt(
		// "settings.mobs.scroll-length"), (int) Math
		// .ceil(.25 * CommandMobs.config().getInt(
		// "settings.mobs.scroll-length")), '&'));
		// }
		properties.put("NAME", name);
		a(id).setCustomName(ChatColor.translateAlternateColorCodes('&', name));
	}

	public boolean nameDisplays() {
		return (Boolean) properties.get("NAMED");
	}

	public void setNameDisplays(boolean displays) {
		properties.put("NAMED", displays);
		a(id).setCustomNameVisible(displays);
	}

	public int getPrice() {
		return (Integer) properties.get("PRICE");
	}

	public void setPrice(int price) {
		if (price < 0) {
			setPrice(0);
			return;
		}
		properties.put("PRICE", price);
	}

	/**
	 * 
	 * Used to view, add, and remove messages.
	 * 
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<String> getMessages() {
		return (ArrayList<String>) properties.get("MESSAGES");
	}

	/**
	 * 
	 * Used to view, add, and remove commands.
	 * 
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<String> getCommands() {
		return (ArrayList<String>) properties.get("COMMANDS");
	}

	public boolean canMove() {
		return (Boolean) properties.get("MOVE");
	}

	public void setCanMove(boolean canMove) {
		properties.put("MOVE", canMove);
		if (canMove)
			a(id).removePotionEffect(PotionEffectType.SLOW);
		else
			a(id).addPotionEffect(
					new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE,
							15, true), true);
	}

	public boolean killable() {
		return (Boolean) properties.get("KILL");
	}

	public void setKillable(boolean killable) {
		properties.put("KILL", killable);
	}

	public UUID getRegistrar() {
		return (UUID) properties.get("REGISTRAR");
	}

	public String getRegistrarName() {
		if (properties.containsKey("REGISTRARN"))
			return (String) properties.get("REGISTRARN");
		else {
			try {
				properties.put("REGISTRARN",
						NameFetcher.singleName(getRegistrar()));
			} catch (Exception e) {
				properties.put("REGISTRARN", "Unknown");
			}
			return getRegistrarName();
		}
	}

	protected boolean scrolls() {
		return getScroller() != null;
	}

	protected Scroller getScroller() {
		return (Scroller) properties.get("SCROLL");
	}

	protected void serialize() {
		String path = "mobs." + a(id).getType().getName().toLowerCase() + "."
				+ id.toString() + ".";

		CommandMobs.config().set(path + "name", getDisplayName());
		CommandMobs.config().set(path + "name-displays", nameDisplays());
		CommandMobs.config().set(path + "price", getPrice());
		CommandMobs.config().set(path + "commands", getCommands());
		CommandMobs.config().set(path + "messages", getMessages());
		CommandMobs.config().set(path + "registrar", getRegistrar().toString());

		CommandMobs.save();
		CommandMobs.reload();
	}

	protected void remove() {
		if (a(id) != null && !a(id).isDead()) {
			a(id).remove();
		}
		Entity entity = a(id);
		CommandMobs.config().set(
				"mobs." + entity.getType().getName().toLowerCase() + "."
						+ entity.getUniqueId().toString(), null);
		if (Bukkit.getPlayer(getRegistrar()) != null)
			Bukkit.getPlayer(getRegistrar()).sendMessage(
					CommandMobs.PREFIX + "§cYour CommandMob has been removed!");
		CommandMobs.save();
		CommandMobs.reload();
		properties.clear();
		MOBS.remove(id);
		id = null;
	}

	public boolean equals(Mob mob) {
		return (this.id == mob.id);
	}

	public static void loadAll() {
		for (EntityType type : EntityType.values()) {
			if (type.isAlive() && type.getName() != null)
				if (CommandMobs.config().contains(
						"mobs." + type.getName().toLowerCase()))
					for (String s : CommandMobs
							.config()
							.getConfigurationSection(
									"mobs." + type.getName().toLowerCase())
							.getKeys(false)) {
						load(a(UUID.fromString(s)));
					}
		}
	}

	protected static Mob create(LivingEntity entity, UUID creator) {
		Mob a = new Mob(entity.getUniqueId(), creator);

		CommandMobs.config().set(
				"mobs." + entity.getType().getName().toLowerCase() + "."
						+ entity.getUniqueId().toString() + ".name",
				defaultName);
		CommandMobs.config().set(
				"mobs." + entity.getType().getName().toLowerCase() + "."
						+ entity.getUniqueId().toString() + ".name-displays",
				defaultNameDisplays);
		CommandMobs.config().set(
				"mobs." + entity.getType().getName().toLowerCase() + "."
						+ entity.getUniqueId().toString() + ".move",
				defaultMovement);
		CommandMobs.config().set(
				"mobs." + entity.getType().getName().toLowerCase() + "."
						+ entity.getUniqueId().toString() + ".price",
				defaultPrice);
		CommandMobs.config().set(
				"mobs." + entity.getType().getName().toLowerCase() + "."
						+ entity.getUniqueId().toString() + ".killable",
				defaultKillable);
		CommandMobs.config().set(
				"mobs." + entity.getType().getName().toLowerCase() + "."
						+ entity.getUniqueId().toString() + ".commands",
				e("say Hello! I am a CommandMob executing a command for %n!"));
		CommandMobs.config().set(
				"mobs." + entity.getType().getName().toLowerCase() + "."
						+ entity.getUniqueId().toString() + ".messages",
				e("&bI am a CommandMob!"));
		CommandMobs.config().set(
				"mobs." + entity.getType().getName().toLowerCase() + "."
						+ entity.getUniqueId().toString() + ".registrar",
				creator.toString());
		CommandMobs.save();
		CommandMobs.reload();

		return a;
	}

	protected static Mob load(LivingEntity entity) {
		if (entity != null) {
			if (!entity.isDead()) {
				if (entity.getType() != null) {
					if (entity.getType().getName() != null) {
						if (!CommandMobs.config().contains(
								"mobs."
										+ entity.getType().getName()
												.toLowerCase() + "."
										+ entity.getUniqueId().toString())) {
							System.out
									.println("Config doesn't contain the info!");
							return null;
						}
						String path = "mobs."
								+ entity.getType().getName().toLowerCase()
								+ "." + entity.getUniqueId().toString() + ".";
						Mob mob = new Mob(entity.getUniqueId(),
								UUID.fromString(CommandMobs.config().getString(
										path + "registrar")), CommandMobs
										.config().getString(path + "name"),
								CommandMobs.config().getInt(path + "price"),
								d(CommandMobs.config().getStringList(
										path + "commands")), d(CommandMobs
										.config().getStringList(
												path + "messages")),
								CommandMobs.config().getBoolean(path + "move"),
								CommandMobs.config().getBoolean(
										path + "killable"), CommandMobs
										.config().getBoolean(
												path + "name-displays"),
								CommandMobs.config().getString(path + "name")
										.contains("%s"));
						System.out.println("Returning mob...");
						return mob;
					} else
						return null;
					// System.out.println("Entity type name is null!");
				} else
					return null;
				// System.out.println("EntityType is null!");
			} else
				return null;
			// System.out.println("Entity is dead");
		}
		// System.out.println("Err");
		return null;
	}
}
