package me.zonalyewhd.commandmobs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;
import net.minecraft.util.com.google.common.collect.Maps;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@SuppressWarnings("deprecation")
public class CommandMobs extends JavaPlugin implements Listener {

	public static final Logger LOGGER = Logger.getLogger("Minecraft");

	private static CommandMobs inst;

	public static final String CURRENT_VERSION = "2.0.0";
	public static final String PREFIX = "§e§l[§2§lCommandMobs§e§l]§r ";

	// Player ID, Mob
	public HashMap<UUID, Mob> RENAMING = Maps.newHashMap();
	// Player ID, Mob
	public HashMap<UUID, Mob> COMMANDS = Maps.newHashMap();
	// Player ID, Old Command
	public HashMap<UUID, String> OLD_CMD = Maps.newHashMap();
	// Player ID, Mob
	public HashMap<UUID, Mob> MESSAGES = Maps.newHashMap();
	// Player ID, Old Message
	public HashMap<UUID, String> OLD_MSG = Maps.newHashMap();

	public Economy econ = null;
	public boolean priceEnabled;

	public static ItemStack SELECTOR;

	public String noPerms = ChatColor.translateAlternateColorCodes('&',
			getConfig().getString("messages.no-perms"));

	private Map<UUID, Inventory[]> menus = Maps.newLinkedHashMap();

	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			LOGGER.severe("Can not find Vault plugin!");
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer()
				.getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			LOGGER.severe("Can not find RSP!");
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}

	public void onEnable() {
		saveDefaultConfig();

		inst = this;

		priceEnabled = getConfig().getBoolean("settings.use-price");

		if ((!setupEconomy()) && (priceEnabled)) {
			LOGGER.severe("Vault is not currently installed on this server! "
					+ "Install Vault, or disable the price feature in the config!");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		getCommand("commandmobs").setExecutor(this);
		Bukkit.getPluginManager().registerEvents(this, this);

		try {
			EnchantmentManager.setup();
		} catch (Exception e) {
			if (!(Enchantment.getByName("Glow") != null && Enchantment
					.getByName("Glow").getId() == 150)) {
				LOGGER.severe("Cannot register fake enchantment! Change the enchantment ID in the config!");
				LOGGER.severe("Disabling CommandMobs...");
				Bukkit.getPluginManager().disablePlugin(this);
				return;
			}
		}

		Mob.loadAll();

		try {
			SELECTOR = new ItemStack(Material.valueOf(getConfig().getString(
					"settings.customizer").toUpperCase()));
			ItemMeta im = SELECTOR.getItemMeta();
			im.setDisplayName("§1§lMob Customizer");
			SELECTOR.setItemMeta(im);
		} catch (Exception e) {
			LOGGER.severe("Invalid item type for customizer: "
					+ getConfig().getString("settings.customizer"));
			for (Player p : Bukkit.getOnlinePlayers()) {
				if (p.isOp()) {
					p.sendMessage(PREFIX
							+ "§rAn error occurred while enabling CommandMobs."
							+ " Please consult the console for a full diagnostic.");
				}
			}
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		// if (getConfig().getDouble("settings.mobs.scroll-speed") <= 0) {
		// LOGGER.severe("Invalid scrolling speed: "
		// + getConfig().getString("settings.mobs.scroll-speed")
		// + "\nMust be greater than zero.");
		// Bukkit.getPluginManager().disablePlugin(this);
		// }
		//
		// Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable()
		// {
		//
		// public void run() {
		// for (Mob mob : Mob.MOBS.values()) {
		// if (mob.scrolls())
		// mob.setDisplayName(Mob.c(mob.getDisplayName())
		// + mob.getScroller().next());
		// }
		// }
		//
		// }, 0, 20 * getConfig().getInt("settings.mobs.scroll-speed"));

		LOGGER.info("CommandMobs v" + CURRENT_VERSION
				+ " enabled successfully!");
	}

	public void onDisable() {
		for (Mob mob : Mob.MOBS.values()) {
			mob.serialize();
		}

		LOGGER.info("CommandMobs v" + CURRENT_VERSION
				+ " disabled successfully!");
	}

	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent e) {
		if (RENAMING.containsKey(e.getPlayer().getUniqueId())) {
			e.setCancelled(true);
			if (!e.getMessage().equals("CANCEL")
					&& !e.getMessage().equals("%x"))
				RENAMING.get(e.getPlayer().getUniqueId()).setDisplayName(
						e.getMessage());
			else if (e.getMessage().equals("%x"))
				RENAMING.get(e.getPlayer().getUniqueId()).setDisplayName(null);
			e.getPlayer().openInventory(
					menus.get(RENAMING.get(e.getPlayer().getUniqueId())
							.getUUID())[0]);
			RENAMING.remove(e.getPlayer().getUniqueId());
			return;
		} else if (COMMANDS.containsKey(e.getPlayer().getUniqueId())) {
			e.setCancelled(true);
			if (OLD_CMD.containsKey(e.getPlayer().getUniqueId())) {
				if (!e.getMessage().equals("CANCEL"))
					COMMANDS.get(e.getPlayer().getUniqueId()).getCommands()
							.add(e.getMessage());
				else
					COMMANDS.get(e.getPlayer().getUniqueId()).getCommands()
							.add(OLD_CMD.get(e.getPlayer().getUniqueId()));
				e.getPlayer().openInventory(
						commandsMenu(COMMANDS.get(e.getPlayer().getUniqueId()),
								e.getPlayer(), true));
				return;
			} else {
				if (!e.getMessage().equals("CANCEL"))
					COMMANDS.get(e.getPlayer().getUniqueId()).getCommands()
							.add(e.getMessage());
				e.getPlayer().openInventory(
						commandsMenu(COMMANDS.get(e.getPlayer().getUniqueId()),
								e.getPlayer(), true));
				COMMANDS.remove(e.getPlayer().getUniqueId());
				return;
			}
		} else if (MESSAGES.containsKey(e.getPlayer().getUniqueId())) {
			e.setCancelled(true);
			if (OLD_MSG.containsKey(e.getPlayer().getUniqueId())) {
				if (!e.getMessage().equals("CANCEL"))
					MESSAGES.get(e.getPlayer().getUniqueId()).getMessages()
							.add(e.getMessage());
				else
					MESSAGES.get(e.getPlayer().getUniqueId()).getMessages()
							.add(OLD_MSG.get(e.getPlayer().getUniqueId()));
				e.getPlayer().openInventory(
						messagesMenu(MESSAGES.get(e.getPlayer().getUniqueId()),
								e.getPlayer(), true));
				MESSAGES.remove(e.getPlayer().getUniqueId());
				return;
			} else {
				if (!e.getMessage().equals("CANCEL"))
					MESSAGES.get(e.getPlayer().getUniqueId()).getMessages()
							.add(e.getMessage());
				e.getPlayer().openInventory(
						messagesMenu(MESSAGES.get(e.getPlayer().getUniqueId()),
								e.getPlayer(), true));
				MESSAGES.remove(e.getPlayer().getUniqueId());
				return;
			}
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		if (getConfig().getBoolean("settings.announce-dev-join")
				&& e.getPlayer().getUniqueId().toString()
						.equals("20f2017a-566d-4109-b937-ba03c7cd7041")) {
			Bukkit.broadcastMessage(PREFIX
					+ "§6The developer, §c"
					+ NameFetcher.singleName(
							UUID.fromString("20f2017a-566d-4109-b937-ba03c7cd7041"),
							"ZonalYewHD") + "§6, has joined the game!");
		}
	}

	@EventHandler
	public void onPlayerDisconnect(PlayerQuitEvent e) {
		RENAMING.remove(e.getPlayer().getUniqueId());
		COMMANDS.get(e.getPlayer().getUniqueId()).getCommands()
				.add(OLD_CMD.get(e.getPlayer().getUniqueId()));
		COMMANDS.remove(e.getPlayer().getUniqueId());
		MESSAGES.get(e.getPlayer().getUniqueId()).getMessages()
				.add(OLD_MSG.get(e.getPlayer().getUniqueId()));
		MESSAGES.remove(e.getPlayer().getUniqueId());
		OLD_CMD.remove(e.getPlayer().getUniqueId());
		OLD_MSG.remove(e.getPlayer().getUniqueId());
	}

	@EventHandler
	public void onEntityDamage(EntityDamageEvent e) {
		if (Mob.isMob(e.getEntity().getUniqueId())
				&& !Mob.getMob(e.getEntity().getUniqueId()).killable())
			e.setCancelled(true);
	}

	@EventHandler
	public void onEntityDeath(EntityDeathEvent e) {
		if (Mob.isMob(e.getEntity().getUniqueId())) {
			if (getConfig().getBoolean("settings.mobs.death.remove")) {
				if (!getConfig().getBoolean("settings.mobs.death.exp"))
					e.setDroppedExp(0);
				if (!getConfig().getBoolean("settings.mobs.death.drops"))
					e.getDrops().clear();
				if (Bukkit.getPlayer(Mob.getMob(e.getEntity().getUniqueId())
						.getRegistrar()) != null) {
					sendMessage(Bukkit.getPlayer(Mob.getMob(
							e.getEntity().getUniqueId()).getRegistrar()),
							"§4Your CommandMob has died!");
				}
				Mob.getMob(e.getEntity().getUniqueId()).remove();
			}
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
	public void onCustomizerClick(InventoryClickEvent e) {
		if (e.getInventory().getTitle().equalsIgnoreCase("§1§lCustomizer")) {
			if (e.getInventory().getItem(0) != null
					&& e.getInventory().getItem(0).getType() == Material.BOOK
					&& e.getCurrentItem() != null
					&& e.getCurrentItem().getType() != null) {
				e.setCancelled(true);
				Player p = (Player) e.getWhoClicked();
				Mob mob = Mob.getMob(UUID.fromString(p
						.getMetadata("CUSTOMIZING").get(0).asString()));
				if (e.getCurrentItem().getType() == Material.BOOK) {
					if (e.getClick() == ClickType.SHIFT_LEFT) {
						p.closeInventory();
						sendMessage(p, "§4You have removed the "
								+ mob.getEntity().getType().getName()
										.toLowerCase().replace('_', ' ')
								+ " CommandMob.");
						mob.remove();
					} else {
						p.playSound(p.getLocation(), Sound.ITEM_BREAK, 1, 1);
					}
				} else if (e.getCurrentItem().getType() == Material.NAME_TAG) {
					if (e.getClick() == ClickType.LEFT) {
						p.closeInventory();
						sendMessage(p,
								"§6Please type the new name of the mob. Type §cCANCEL§6 to cancel.");
						RENAMING.put(p.getUniqueId(), mob);
					}
				} else if (e.getCurrentItem().getType() == Material.DIODE) {
					p.closeInventory();
					p.openInventory(commandsMenu(mob, p, false));
					COMMANDS.put(p.getUniqueId(), mob);
				} else if (e.getCurrentItem().getType() == Material.BOOK_AND_QUILL) {
					p.closeInventory();
					p.openInventory(messagesMenu(mob, p, false));
					MESSAGES.put(p.getUniqueId(), mob);
				} else if (e.getCurrentItem().getType() == Material.INK_SACK) {
					e.getCurrentItem().setDurability(
							(short) (mob.killable() ? 10 : 8));
					ItemMeta meta = e.getCurrentItem().getItemMeta();
					meta.setDisplayName(mob.killable() ? "§aNot Killable"
							: "§cKillable");
					e.getCurrentItem().setItemMeta(meta);
					mob.setKillable(!mob.killable());
				} else if (e.getCurrentItem().getType() == Material.DIAMOND) {
					ItemMeta meta = e.getCurrentItem().getItemMeta();
					int price = mob.getPrice();
					if (e.getClick() == ClickType.LEFT)
						price += 1;
					else if (e.getClick() == ClickType.SHIFT_LEFT)
						price += 5;
					else if (e.getClick() == ClickType.DROP)
						price += 25;
					else if (e.getClick() == ClickType.DOUBLE_CLICK)
						price += 998;
					else if (e.getClick() == ClickType.RIGHT)
						price -= 1;
					else if (e.getClick() == ClickType.SHIFT_RIGHT)
						price -= 5;
					else if (e.getClick() == ClickType.CONTROL_DROP)
						price -= 25;
					else if (e.getClick() == ClickType.NUMBER_KEY)
						price -= 1000;
					else if (e.getClick() == ClickType.MIDDLE)
						price = 0;
					else
						p.playSound(p.getLocation(), Sound.ITEM_BREAK, 1, 1);
					mob.setPrice(price);
					meta.setDisplayName("§bPrice: §3" + mob.getPrice());
					e.getCurrentItem().setItemMeta(meta);
				} else if (e.getCurrentItem().getType() == Material.PAPER) {
					p.closeInventory();
					p.openInventory(menus.get(mob.getUUID())[1]);
				}
			} else if (e.getInventory().getItem(0) != null
					&& e.getInventory().getItem(0).getType() == Material.PAPER
					&& e.getCurrentItem() != null
					&& e.getCurrentItem().getType() != null) {
				e.setCancelled(true);
				Player p = (Player) e.getWhoClicked();
				Mob mob = Mob.getMob(UUID.fromString(ChatColor.stripColor(
						e.getInventory().getContents()[e.getInventory().first(
								Material.BOOK)].getItemMeta().getLore().get(1))
						.replaceAll("  UUID: ", "")));
				if (e.getCurrentItem().getType() == Material.PAPER) {
					p.closeInventory();
					p.openInventory(menus.get(mob.getUUID())[0]);
				} else if (e.getCurrentItem().getType() == Material.INK_SACK) {
					e.getCurrentItem().setDurability(
							(short) (mob.canMove() ? 8 : 10));
					ItemMeta meta = e.getCurrentItem().getItemMeta();
					meta.setDisplayName(mob.canMove() ? "§cCannot Move"
							: "§aCan Move");
					mob.setCanMove(!mob.canMove());
				} else if (e.getCurrentItem().getType() == Material.REDSTONE_BLOCK) {
					p.closeInventory();
					RENAMING.remove(p.getUniqueId());
					COMMANDS.remove(p.getUniqueId());
					MESSAGES.remove(p.getUniqueId());
				} else if (e.getCurrentItem().getType() == Material.BOOK) {
					if (e.getClick() == ClickType.SHIFT_LEFT) {
						p.closeInventory();
						sendMessage(p, "§4You have removed a "
								+ mob.getEntity().getType().getName()
										.toLowerCase().replace('_', ' ')
								+ " CommandMob.");
						mob.remove();
					} else
						p.playSound(p.getLocation(), Sound.ITEM_BREAK, 1, 1);
				}
			}
		} else if (e.getInventory().getTitle().equalsIgnoreCase("§c§lCommands")) {
			e.setCancelled(true);
			Player p = (Player) e.getWhoClicked();
			Mob mob = Mob.getMob(UUID.fromString(p
					.getMetadata("CUSTOMIZING").get(0).asString()));
			if (e.getCurrentItem() != null) {
				if (e.getCurrentItem().getType() == Material.EMPTY_MAP) {
					if (e.getClick() == ClickType.LEFT) {
						OLD_CMD.put(p.getUniqueId(),
								mob.getCommands().get(e.getRawSlot()));
						COMMANDS.put(p.getUniqueId(), mob);
						p.closeInventory();
						sendMessage(p, "§aThe old command was §2"
								+ mob.getCommands().get(e.getRawSlot())
								+ "§a. Please enter a new command now.");
						mob.getCommands().remove(e.getRawSlot());
					} else if (e.getClick() == ClickType.RIGHT) {
						mob.getCommands().remove(e.getRawSlot());
						p.closeInventory();
						p.openInventory(commandsMenu(mob, p, false));
					} else
						p.playSound(p.getLocation(), Sound.ITEM_BREAK, 1, 1);
				} else if (e.getCurrentItem().getType() == Material.ENCHANTED_BOOK)
					p.playSound(p.getLocation(), Sound.ITEM_BREAK, 1, 1);
				else if (e.getCurrentItem().getType() == Material.EMERALD_BLOCK) {
					if (mob.getCommands().size() == 36) {
						p.playSound(p.getLocation(), Sound.ITEM_BREAK, 1, 1);
						return;
					}
					COMMANDS.put(p.getUniqueId(), mob);
					p.closeInventory();
					sendMessage(p,
							"§aEnter a new command into chat. Do §2§lnot§a include the slash.");
				} else if (e.getCurrentItem().getType() == Material.REDSTONE_BLOCK) {
					p.closeInventory();
					p.openInventory(menus.get(mob.getUUID())[0]);
					COMMANDS.remove(p.getUniqueId());
				}
			}
		} else if (e.getInventory().getTitle().equalsIgnoreCase("§9§lMessages")) {
			e.setCancelled(true);
			Player p = (Player) e.getWhoClicked();
			Mob mob = Mob.getMob(UUID.fromString(p
					.getMetadata("CUSTOMIZING").get(0).asString()));
			if (e.getCurrentItem() != null) {
				if (e.getCurrentItem().getType() == Material.EMPTY_MAP) {
					if (e.getClick() == ClickType.LEFT) {
						OLD_MSG.put(p.getUniqueId(),
								mob.getMessages().get(e.getRawSlot()));
						MESSAGES.put(p.getUniqueId(), mob);
						p.closeInventory();
						sendMessage(p, "§aThe old message was §2"
								+ mob.getMessages().get(e.getRawSlot())
								+ "§a. Please enter a new message now.");
						mob.getMessages().remove(e.getRawSlot());
					} else if (e.getClick() == ClickType.RIGHT) {
						mob.getMessages().remove(e.getRawSlot());
						p.closeInventory();
						p.openInventory(messagesMenu(mob, p, false));
					} else
						p.playSound(p.getLocation(), Sound.ITEM_BREAK, 1, 1);
				} else if (e.getCurrentItem().getType() == Material.ENCHANTED_BOOK)
					p.playSound(p.getLocation(), Sound.ITEM_BREAK, 1, 1);
				else if (e.getCurrentItem().getType() == Material.EMERALD_BLOCK) {
					if (mob.getMessages().size() == 36) {
						p.playSound(p.getLocation(), Sound.ITEM_BREAK, 1, 1);
						return;
					}
					MESSAGES.put(p.getUniqueId(), mob);
					p.closeInventory();
					sendMessage(p, "§aEnter a new message into chat.");
				} else if (e.getCurrentItem().getType() == Material.REDSTONE_BLOCK) {
					p.closeInventory();
					p.openInventory(menus.get(mob.getUUID())[0]);
					MESSAGES.remove(p.getUniqueId());
				}
			}
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
	public void onMobClick(PlayerInteractEntityEvent e) {
		if (getConfig().contains(
				"mobs." + e.getRightClicked().getType().getName().toLowerCase()
						+ "." + e.getRightClicked().getUniqueId().toString())) {
			e.setCancelled(true);
			Mob mob = Mob.getMob(e.getRightClicked().getUniqueId());
			if (e.getPlayer().getItemInHand() != null
					&& e.getPlayer().getItemInHand().equals(SELECTOR)) {
				if (((mob.getRegistrar() == e.getPlayer().getUniqueId()) && (getConfig()
						.getBoolean("permissions.creator-modify")))
						|| (e.getPlayer().hasPermission("cmdmobs."
								+ getConfig().getString("permissions.modify")
										.replaceAll(
												"%d",
												e.getRightClicked()
														.getUniqueId()
														.toString())))) {
					if (menus.containsKey(e.getRightClicked().getUniqueId())) {
						e.getPlayer().closeInventory();
						e.getPlayer()
								.openInventory(
										menus.get(e.getRightClicked()
												.getUniqueId())[0]);
						e.getPlayer().setMetadata(
								"CUSTOMIZING",
								new FixedMetadataValue(this, e
										.getRightClicked().getUniqueId()
										.toString()));
					} else {
						Inventory[] a = new Inventory[2];

						Inventory menu = Bukkit.createInventory(null, 9,
								"§1§lCustomizer");

						// "Info" Book
						ItemStack item = new ItemStack(Material.BOOK);
						ItemMeta meta = item.getItemMeta();
						meta.setDisplayName("§eInfo");
						meta.setLore(Arrays.asList("", "  §6UUID: §c"
								+ mob.getUUID().toString(), "  §6Creator: §c"
								+ mob.getRegistrarName(), "",
								"§8Shift left click to remove this mob"));
						item.setItemMeta(meta);
						item.addEnchantment(
								EnchantmentManager.getBlankEnchantment(), 1);

						menu.setItem(0, item);

						// "Rename" Name Tag
						item = new ItemStack(Material.NAME_TAG);
						meta = item.getItemMeta();
						meta.setDisplayName("§aRename");
						meta.setLore(Arrays.asList("§8Left click to rename"));
						item.setItemMeta(meta);

						menu.setItem(2, item);

						// "Commands" Redstone Repeater
						item = new ItemStack(Material.DIODE);
						meta = item.getItemMeta();
						meta.setDisplayName("§cCommands");
						meta.setLore(Arrays
								.asList("§8Click to modify and view"));
						item.setItemMeta(meta);

						menu.setItem(3, item);

						// "Messages" Book and Quill
						item = new ItemStack(Material.BOOK_AND_QUILL);
						meta = item.getItemMeta();
						meta.setDisplayName("§9Messages");
						meta.setLore(Arrays
								.asList("§8Click to modify and view"));
						item.setItemMeta(meta);

						menu.setItem(4, item);

						// "Killablility" Dye
						item = new ItemStack(Material.INK_SACK, 1,
								(short) (mob.killable() ? 8 : 10));
						meta = item.getItemMeta();
						meta.setDisplayName("§"
								+ (mob.killable() ? "c" : "aNot ") + "Killable");
						meta.setLore(Arrays.asList("§8Click to change"));
						item.setItemMeta(meta);

						menu.setItem(5, item);

						// "Price" Diamond
						item = new ItemStack(Material.DIAMOND);
						meta = item.getItemMeta();
						meta.setDisplayName("§bPrice: §3" + mob.getPrice());
						meta.setLore(Arrays.asList("",
								"  §a+1 §8→ §7Left Click",
								"  §a+5 §8→ §7Shift Left Click",
								"  §a+25 §8→ §7Drop",
								"  §a+1,000 §8→ §7Double Click",
								"  §c-1 §8→ §7Right Click",
								"  §c-5 §8→ §7Shift Right Click",
								"  §c-25 §8→ §7Control Drop Click",
								"  §c-1,000 §8→ §7Number Keys (1-9)",
								"  §4Reset §8→ §7Select Block"));
						item.setItemMeta(meta);

						menu.setItem(6, item);

						// "Next Page" Paper
						item = new ItemStack(Material.PAPER);
						meta = item.getItemMeta();
						meta.setDisplayName("§7Next Page");
						item.setItemMeta(meta);

						menu.setItem(8, item);

						a[0] = menu;

						menu = Bukkit
								.createInventory(null, 9, "§1§lCustomizer");

						// "Previous Page" Paper
						item = new ItemStack(Material.PAPER);
						meta = item.getItemMeta();
						meta.setDisplayName("§7Previous Page");
						item.setItemMeta(meta);

						menu.setItem(0, item);

						// "Movement" Dye
						item = new ItemStack(Material.INK_SACK, 1,
								(short) (mob.canMove() ? 10 : 8));
						meta = item.getItemMeta();
						meta.setDisplayName("§"
								+ (mob.canMove() ? "aCan Move" : "cCannot Move"));
						meta.setLore(Arrays.asList("§8Click to change"));
						item.setItemMeta(meta);

						menu.setItem(2, item);

						// "Close" Redstone Block
						item = new ItemStack(Material.REDSTONE_BLOCK);
						meta = item.getItemMeta();
						meta.setDisplayName("§4§lClose");
						item.setItemMeta(meta);

						menu.setItem(6, item);

						// "Info" Book
						item = new ItemStack(Material.BOOK);
						meta = item.getItemMeta();
						meta.setDisplayName("§eInfo");
						meta.setLore(Arrays.asList("", "  §6UUID: §c"
								+ mob.getUUID().toString(), "  §6Creator: §c"
								+ mob.getRegistrarName(), "",
								"§8Shift left-click to remove this mob"));
						item.setItemMeta(meta);
						item.addEnchantment(
								EnchantmentManager.getBlankEnchantment(), 1);

						menu.setItem(8, item);

						a[1] = menu;

						menus.put(e.getRightClicked().getUniqueId(), a);

						e.getPlayer().closeInventory();
						e.getPlayer()
								.openInventory(
										menus.get(e.getRightClicked()
												.getUniqueId())[0]);

						e.getPlayer().setMetadata(
								"CUSTOMIZING",
								new FixedMetadataValue(this, e
										.getRightClicked().getUniqueId()
										.toString()));
					}
				} else {
					sendMessage(e.getPlayer(), noPerms);
				}
				return;
			} else {
				if (e.getPlayer().hasPermission(
						"cmdmobs."
								+ getConfig().getString("permissions.mob-use")
										.replaceAll("%d",
												mob.getUUID().toString()))) {
					if (priceEnabled) {
						if (econ.has(e.getPlayer(), mob.getPrice())) {
							econ.withdrawPlayer(e.getPlayer(), mob.getPrice());
						} else {
							sendMessage(
									e.getPlayer(),
									getConfig()
											.getString(
													"messages.not-enough-money")
											.replace(
													"%s",
													String.valueOf((int) (mob
															.getPrice() - econ.getBalance(e
															.getPlayer()))))
											.replace(
													"%p",
													String.valueOf(mob
															.getPrice())));
							return;
						}

					}
					for (String cmd : mob.getCommands()) {
						if (cmd.startsWith("c:")) {
							Bukkit.dispatchCommand(
									Bukkit.getConsoleSender(),
									cmd.replaceFirst("c:", "")
											.replaceAll("%n",
													e.getPlayer().getName())
											.replaceAll(
													"%b",
													String.valueOf(econ.getBalance(e
															.getPlayer())))
											.replaceAll(
													"%p",
													String.valueOf(mob
															.getPrice())));
						} else {
							e.getPlayer()
									.performCommand(
											cmd.replaceAll("%n",
													e.getPlayer().getName())
													.replaceAll(
															"%b",
															String.valueOf(econ
																	.getBalance(e
																			.getPlayer())))
													.replaceAll(
															"%p",
															String.valueOf(mob
																	.getPrice())));
						}
					}
					for (String msg : mob.getMessages()) {
						e.getPlayer()
								.sendMessage(
										ChatColor
												.translateAlternateColorCodes(
														'&',
														msg.replaceAll(
																"%n",
																e.getPlayer()
																		.getName())
																.replaceAll(
																		"%b",
																		String.valueOf(econ
																				.getBalance(e
																						.getPlayer())))
																.replaceAll(
																		"%p",
																		String.valueOf(mob
																				.getPrice()))));
					}
				} else {
					sendMessage(e.getPlayer(), noPerms);
					return;
				}
			}
		}
	}

	private Inventory commandsMenu(Mob mob, Player player, boolean remove) {
		Inventory i = Bukkit.createInventory(null,
				((mob.getCommands().size() <= 9) ? 27 : (mob.getCommands()
						.size() <= 18) ? 36
						: (mob.getCommands().size() <= 27) ? 45 : (mob
								.getCommands().size() <= 36) ? 54 : -1),
				"§c§lCommands");

		ItemStack item;
		ItemMeta meta;

		// "Back" Redstone Block
		item = new ItemStack(Material.REDSTONE_BLOCK);
		meta = item.getItemMeta();
		meta.setDisplayName("§4§lBack");
		item.setItemMeta(meta);

		i.setItem(i.getSize() - 6, item);

		// "Help Book" Enchanted Book
		item = new ItemStack(Material.ENCHANTED_BOOK);
		meta = item.getItemMeta();
		meta.setDisplayName("§eHelp Book");
		meta.setLore(Arrays
				.asList("",
						"  §aLeft click§8 a command to edit it",
						"  §aRight click§8 a command to remove it",
						"  §aLeft click§8 \"§aNew Command§8\" to add an additional command.",
						"  §3Available Variables:",
						"    §e%n§7 - the player's name",
						"    §e%b§7 - the player's balance",
						"    §e%p§7 - the price of the mob"));
		item.setItemMeta(meta);

		i.setItem(i.getSize() - 5, item);

		// "New Command" Emerald Block
		item = new ItemStack(Material.EMERALD_BLOCK);
		meta = item.getItemMeta();
		meta.setDisplayName("§aNew Command");
		item.setItemMeta(meta);

		i.setItem(i.getSize() - 4, item);

		// "Command"s Empty Map
		for (int k = 0; k < mob.getCommands().size(); k++) {
			item = new ItemStack(Material.EMPTY_MAP, k + 1);
			meta = item.getItemMeta();
			meta.setDisplayName("§cCommand");
			meta.setLore(Arrays.asList("§bCurrent Command:", "  §3"
					+ mob.getCommands().get(k)));
			item.setItemMeta(meta);

			i.setItem(k, item);
		}

		if (remove)
			COMMANDS.remove(player.getUniqueId());

		return i;
	}

	private Inventory messagesMenu(Mob mob, Player player, boolean remove) {
		Inventory i = Bukkit.createInventory(null,
				((mob.getMessages().size() <= 9) ? 27 : (mob.getMessages()
						.size() <= 18) ? 36
						: (mob.getMessages().size() <= 27) ? 45 : (mob
								.getMessages().size() <= 36) ? 54 : -1),
				"§9§lMessages");

		ItemStack item;
		ItemMeta meta;

		// "Back" Redstone Block
		item = new ItemStack(Material.REDSTONE_BLOCK);
		meta = item.getItemMeta();
		meta.setDisplayName("§4§lBack");
		item.setItemMeta(meta);

		i.setItem(i.getSize() - 6, item);

		// "Help Book" Enchanted Book
		item = new ItemStack(Material.ENCHANTED_BOOK);
		meta = item.getItemMeta();
		meta.setDisplayName("§eHelp Book");
		meta.setLore(Arrays
				.asList("",
						"  §aLeft click§8 a message to edit it",
						"  §aRight click§8 a message to remove it",
						"  §aLeft click§8 \"§aNew Message§8\" to add an additional message",
						"  §3Available Variables:",
						"    §e%n§7 - the player's name",
						"    §e%b§7 - the player's balance",
						"    §e%p§7 - the price of the mob"));
		item.setItemMeta(meta);

		i.setItem(i.getSize() - 5, item);

		// "New Message" Emerald Block
		item = new ItemStack(Material.EMERALD_BLOCK);
		meta = item.getItemMeta();
		meta.setDisplayName("§aNew Message");
		item.setItemMeta(meta);

		i.setItem(i.getSize() - 4, item);

		// "Command"s Empty Map
		for (int k = 0; k < mob.getMessages().size(); k++) {
			item = new ItemStack(Material.EMPTY_MAP, k + 1);
			meta = item.getItemMeta();
			meta.setDisplayName("§9Message");
			meta.setLore(Arrays.asList("§bCurrent Message:", "  §3"
					+ mob.getMessages().get(k)));
			item.setItemMeta(meta);

			i.setItem(k, item);
		}

		if (remove)
			MESSAGES.remove(player.getUniqueId());

		return i;
	}

	public boolean onCommand(CommandSender cs, Command cmd, String s,
			String[] args) {
		if (!(cs instanceof Player)) {
			cs.sendMessage("You must be a player to use these commands!");
			return true;
		}
		Player p = (Player) cs;
		if (args.length == 0) {
			p.sendMessage(new String[] {
					"§e§m§l--------§e§l[§2§l CommandMobs §e§l]§e§l§m--------§r",
					"  §6Developer: §aZonalYewHD",
					"  §6Version: §a" + CURRENT_VERSION,
					"  §6Bukkit: §ahttp://bit.ly/cmdMobs" });
			return true;
		} else if (args.length == 1) {
			if (args[0].equalsIgnoreCase("customizer")) {
				if (p.hasPermission("cmdmobs."
						+ getConfig().getString("permissions.customizer.self"))) {
					p.getInventory().addItem(SELECTOR);
					sendMessage(p,
							"&aYou have received a &eCustomizer&a. To use it, right-click on a CommandMob.");
					return true;
				} else {
					sendMessage(p, noPerms);
					return true;
				}
			} else if (args[0].equalsIgnoreCase("new")) {
				if (p.hasPermission("cmdmobs."
						+ getConfig().getString("permissions.create"))) {
					LivingEntity e = ((LivingEntity) p.getWorld().spawnEntity(
							p.getLocation(), EntityType.VILLAGER));
					e.setCustomName(ChatColor.translateAlternateColorCodes('&',
							getConfig().getString("settings.defaults.name")));
					if (!getConfig().getBoolean("settings.defaults.movement"))
						e.addPotionEffect(new PotionEffect(
								PotionEffectType.SLOW, Integer.MAX_VALUE, 15),
								true);
					Mob.create(e, p.getUniqueId());
					if (getConfig().getBoolean("settings.customizer-on-create")) {
						p.getInventory().addItem(SELECTOR);
						sendMessage(
								p,
								"&aYou have created a CommandMob. "
										+ "To customize it, right-click on it with your &eCustomizer&a.");
						return true;
					} else {
						sendMessage(
								p,
								"&aYou have created a CommandMob. "
										+ "To customize it, right-click on it with a &eCustomizer&a"
										+ " obtained through &e/cmdmobs customizer&a.");
						return true;
					}
				} else {
					sendMessage(p, noPerms);
					return true;
				}
			}
		} else if (args.length == 2) {
			if (args[0].equalsIgnoreCase("customizer")) {
				if (p.hasPermission("cmdmobs."
						+ getConfig().getString("permissions.customizer.other"))) {
					Player t = Bukkit.getPlayer(args[1]);
					if (t == null) {
						sendMessage(p, "&4" + args[1] + "&c is not online!");
						return true;
					}
					t.getInventory().addItem(SELECTOR);
					sendMessage(p, "&aYou have given &e" + args[1]
							+ "&a a Customizer.");
					sendMessage(
							t,
							"&e"
									+ p.getName()
									+ "&a gave you a &eCustomizer&a. To use it, right-click on a CommandMob.");
					return true;
				} else {
					sendMessage(p, noPerms);
					return true;
				}
			}
		}
		sendMessage(
				p,
				"§cThe arguments you entered are invalid. Either check your "
						+ "spelling, or use §4/cm§c to see the Bukkit page for help.");
		return true;

	}

	protected static FileConfiguration config() {
		return inst.getConfig();
	}

	public static void save() {
		inst.saveConfig();
	}

	public static void reload() {
		inst.reloadConfig();
	}

	private void sendMessage(Player player, String message) {
		if (player != null && message != null)
			player.sendMessage(PREFIX
					+ ChatColor.translateAlternateColorCodes('&', message));
	}

}
