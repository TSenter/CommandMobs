package me.zonalyewhd.commandmobs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@SuppressWarnings("deprecation")
public class Mob {

	public static final int MAX_COMMANDS = 36;
	public static final int MAX_MESSAGES = 36;

	public static final Set<String> VALID_MOB_TYPES = ImmutableSet
			.copyOf(Arrays.asList("BAT", "CHICKEN", "COW", "DONKEY", "HORSE", "MULE", "MUSHROOM_COW", "OCELOT", "PIG",
					"SHEEP", "SKELETON_HORSE", "VILLAGER", "ZOMBIE_HORSE"));

	protected static Map<UUID, Mob> MOBS = Maps.newHashMap();
	protected static List<Mob> SCROLLING_MOBS = Lists.newArrayList();

	private static String defaultName = CommandMobs.config().getString("settings.defaults.name");
	private static boolean defaultNameDisplays = CommandMobs.config().getBoolean("settings.defaults.name-displays");
	private static boolean defaultMovement = CommandMobs.config().getBoolean("settings.defaults.movement");
	private static int defaultPrice = CommandMobs.config().getInt("settings.defaults.price");
	private static boolean defaultKillable = CommandMobs.config().getBoolean("settings.defaults.killable");

	private UUID id;
	private Map<String, Object> properties;

	private Mob(UUID id, UUID registrar) {
		this.id = id;

		MOBS.put(id, this);

		properties = new HashMap<String, Object>();
		properties.put("REGISTRAR", registrar);
		properties.put("COMMANDS", e("say Hello! I am a CommandMob executing a command for %n!"));
		properties.put("MESSAGES", e("&bI am a CommandMob!"));
		properties.put("TYPE", ((LivingEntity) a(id)).getType());
		setDisplayName(defaultName);
		setNameDisplays(defaultNameDisplays);
		setPrice(defaultPrice);
		setCanMove(defaultMovement);
		setKillable(defaultKillable);
	}

	private Mob(UUID id, UUID registrar, String displayName, int price, ArrayList<String> commands,
			ArrayList<String> messages, boolean canMove, boolean killable, boolean nameDisplays, boolean scrolls) {
		this.id = id;
		properties = new HashMap<String, Object>();
		properties.put("REGISTRAR", registrar);
		if (scrolls)
			properties.put("SCROLL",
					new Scroller(b(displayName), CommandMobs.config().getInt("settings.mobs.scroll-length"),
							(int) Math.ceil(.25 * CommandMobs.config().getInt("settings.mobs.scroll-length")), '&'));
		if (scrolls)
			SCROLLING_MOBS.add(this);
		properties.put("COMMANDS", commands);
		properties.put("MESSAGES", messages);
		properties.put("TYPE", ((LivingEntity) a(id)).getType());
		setDisplayName(displayName);
		setPrice(price);
		setCanMove(canMove);
		setKillable(killable);
		setNameDisplays(nameDisplays);

		MOBS.put(id, this);
	}

	public static Mob getMob(UUID uuid) {
		if (MOBS.containsKey(uuid)) {
			return MOBS.get(uuid);
		} else if (uuid == null) {
			return null;
		} else if (a(uuid) == null) {
			return null;
		} else if (CommandMobs.config()
				.contains("mobs." + a(uuid).getType().getName().toLowerCase() + "." + uuid.toString())) {
			return load(a(uuid));
		}
		return null;
	}

	public static boolean isMob(UUID uuid) {
		for (UUID mob : MOBS.keySet())
			if (mob == uuid)
				return true;
		return false;
	}

	public static EntityType getEntityType(String name) {
		return EntityType.valueOf(name.toUpperCase().replace(' ', '_'));
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
		if (getDisplayName() != null && getDisplayName().contains("%s") && !name.contains("%s")) {
			SCROLLING_MOBS.remove(this);
			properties.remove("SCROLL");
		}
		if (getDisplayName() != null && !getDisplayName().contains("%s") && name.contains("%s")) {
			SCROLLING_MOBS.add(this);
			properties.put("SCROLL", new Scroller(b(name), CommandMobs.config().getInt("settings.mobs.scroll-length"),
					(int) Math.ceil(.25 * CommandMobs.config().getInt("settings.mobs.scroll-length")), '&'));
		}
		if (getDisplayName() != null && getDisplayName().contains("%s") && name.contains("%s")) {
			properties.put("SCROLL", new Scroller(b(name), CommandMobs.config().getInt("settings.mobs.scroll-length"),
					(int) Math.ceil(0.25 * CommandMobs.config().getInt("settings.mobs.scroll-length")), '&'));
			if (!SCROLLING_MOBS.contains(this))
				SCROLLING_MOBS.add(this);
		}
		properties.put("NAME", name);
		setName(name);
	}

	public void setName(String name) {
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

	public EntityType getEntityType() {
		return (EntityType) properties.get("TYPE");
	}

	public void setEntityType(EntityType type) {
		if (getEntityType().equals(type))
			return;
		LivingEntity ent = (LivingEntity) getEntity().getWorld().spawnEntity(getEntity().getLocation(), type);
		MOBS.remove(id);
		SCROLLING_MOBS.remove(this);
		MOBS.put(ent.getUniqueId(), this);
		UUID old = this.id;
		this.id = ent.getUniqueId();
		Bukkit.getPlayer(getRegistrar()).removeMetadata("CUSTOMIZING", CommandMobs.getInstance());
		Bukkit.getPlayer(getRegistrar()).setMetadata("CUSTOMIZING",
				new FixedMetadataValue(CommandMobs.getInstance(), id.toString()));
		properties.put("TYPE", type);
		applyProperties();
		CommandMobs.config().set("mobs." + a(old).getType().getName().toLowerCase() + "." + old.toString(), null);
		CommandMobs.save();
		CommandMobs.reload();
		serialize();
		a(old).remove();
		CommandMobs.menus.put(ent.getUniqueId(), CommandMobs.createMenus(this));
	}

	public boolean canMove() {
		return (Boolean) properties.get("MOVE");
	}

	public void setCanMove(boolean canMove) {
		properties.put("MOVE", canMove);
		
		if (canMove)
			a(id).removePotionEffect(PotionEffectType.SLOW);
		else
			a(id).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 25, true, true));
		
		a(id).setCollidable(canMove);
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
				properties.put("REGISTRARN", NameFetcher.singleName(getRegistrar()));
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

	private void applyProperties() {
		setDisplayName(getDisplayName());
		setNameDisplays(nameDisplays());
		setPrice(getPrice());
		setKillable(killable());
		setCanMove(canMove());
	}

	protected void serialize() {
		String path = "mobs." + a(id).getType().getName().toLowerCase() + "." + id.toString() + ".";

		CommandMobs.config().set(path + "name", getDisplayName());
		CommandMobs.config().set(path + "name-displays", nameDisplays());
		CommandMobs.config().set(path + "price", getPrice());
		CommandMobs.config().set(path + "commands", getCommands());
		CommandMobs.config().set(path + "messages", getMessages());
		CommandMobs.config().set(path + "registrar", getRegistrar().toString());
		CommandMobs.config().set(path + "killable", killable());
		CommandMobs.config().set(path + "move", canMove());

		CommandMobs.save();
		CommandMobs.reload();
	}

	protected void remove(boolean msg) {
		if (a(id) != null && !a(id).isDead()) {
			a(id).remove();
		}
		Entity entity = a(id);
		CommandMobs.config()
				.set("mobs." + entity.getType().getName().toLowerCase() + "." + entity.getUniqueId().toString(), null);
		if (msg)
			if (Bukkit.getPlayer(getRegistrar()) != null)
				Bukkit.getPlayer(getRegistrar())
						.sendMessage(CommandMobs.PREFIX + "§cYour CommandMob has been removed!");
		CommandMobs.save();
		CommandMobs.reload();
		properties.clear();
		SCROLLING_MOBS.remove(this);
		MOBS.remove(id);
		id = null;
	}

	public boolean equals(Mob mob) {
		return (this.id == mob.id);
	}

	public static void loadAll() {
		for (EntityType type : EntityType.values()) {
			if (type.isAlive() && type.getName() != null)
				if (CommandMobs.config().contains("mobs." + type.getName().toLowerCase()))
					for (String s : CommandMobs.config().getConfigurationSection("mobs." + type.getName().toLowerCase())
							.getKeys(false)) {
						load(a(UUID.fromString(s)));
					}
		}
	}

	protected static Mob create(LivingEntity entity, UUID creator) {
		Mob a = new Mob(entity.getUniqueId(), creator);

		CommandMobs.config().set(
				"mobs." + entity.getType().getName().toLowerCase() + "." + entity.getUniqueId().toString() + ".name",
				defaultName);
		CommandMobs.config().set("mobs." + entity.getType().getName().toLowerCase() + "."
				+ entity.getUniqueId().toString() + ".name-displays", defaultNameDisplays);
		CommandMobs.config().set(
				"mobs." + entity.getType().getName().toLowerCase() + "." + entity.getUniqueId().toString() + ".move",
				defaultMovement);
		CommandMobs.config().set(
				"mobs." + entity.getType().getName().toLowerCase() + "." + entity.getUniqueId().toString() + ".price",
				defaultPrice);
		CommandMobs.config().set("mobs." + entity.getType().getName().toLowerCase() + "."
				+ entity.getUniqueId().toString() + ".killable", defaultKillable);
		CommandMobs.config().set("mobs." + entity.getType().getName().toLowerCase() + "."
				+ entity.getUniqueId().toString() + ".commands",
				e("say Hello! I am a CommandMob executing a command for %n!"));
		CommandMobs.config().set("mobs." + entity.getType().getName().toLowerCase() + "."
				+ entity.getUniqueId().toString() + ".messages", e("&bI am a CommandMob!"));
		CommandMobs.config().set("mobs." + entity.getType().getName().toLowerCase() + "."
				+ entity.getUniqueId().toString() + ".registrar", creator.toString());
		CommandMobs.save();
		CommandMobs.reload();

		return a;
	}

	protected static Mob load(LivingEntity entity) {
		if (entity != null) {
			if (!entity.isDead()) {
				if (entity.getType() != null) {
					if (entity.getType().getName() != null) {
						if (!CommandMobs.config().contains("mobs." + entity.getType().getName().toLowerCase() + "."
								+ entity.getUniqueId().toString())) {
							System.out.println("Config doesn't contain the info!");
							return null;
						}
						String path = "mobs." + entity.getType().getName().toLowerCase() + "."
								+ entity.getUniqueId().toString() + ".";
						Mob mob = new Mob(entity.getUniqueId(),
								UUID.fromString(CommandMobs.config().getString(path + "registrar")),
								CommandMobs.config().getString(path + "name"),
								CommandMobs.config().getInt(path + "price"),
								d(CommandMobs.config().getStringList(path + "commands")),
								d(CommandMobs.config().getStringList(path + "messages")),
								CommandMobs.config().getBoolean(path + "move"),
								CommandMobs.config().getBoolean(path + "killable"),
								CommandMobs.config().getBoolean(path + "name-displays"),
								CommandMobs.config().getString(path + "name").contains("%s"));
						return mob;
					}
				}
			} 
		}
		return null;
	}
}
