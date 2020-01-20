package net.rezxis.thirdParty;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.google.gson.Gson;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ChatColor;
import net.rezxis.thirdParty.packet.TAuthServerPacket;
import net.rezxis.thirdParty.packet.TAuthServerResponse;
import net.rezxis.thirdParty.packet.TPacket;
import net.rezxis.thirdParty.packet.TServerStoppedPacket;

public class ThirdParty extends JavaPlugin {

	@Getter(AccessLevel.PACKAGE)
	private static ThirdParty instance;
	private static final Gson gson = new Gson();
	@Getter(AccessLevel.PACKAGE)
	private FileConfiguration cfg;
	@Getter(AccessLevel.PACKAGE)
	private WSClient client;
	private double version = 0.4;
	private boolean authed = false;
	
	public void onEnable() {
		instance = this;
		boolean disable = false;
		System.out.println("ThirdParty version : "+version);
		if (getServer().getOnlineMode()) {
			System.out.println(ChatColor.RED+"To use ThirdParty plugin, online-mode must be false");
			disable = true;
		}
		if (!getServer().spigot().getConfig().getBoolean("settings.bungeecord")) {
			System.out.println(ChatColor.RED+"To use ThirdParty plugin, spigot.yml settings/bungeecord must be true");
			disable = true;
		}
		loadConfig();
		if (cfg.getString("token") == null) {
			System.out.println(ChatColor.RED+"Config : token isn't nullable.");
			disable = true;
		}
		if (disable) {
			getServer().getPluginManager().disablePlugin(this);
		}
		try {
			client = new WSClient(new URI(cfg.getString("gateway")));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		System.out.println("connecting to "+cfg.getString("gateway"));
		client.connect();
		Runtime.getRuntime().addShutdownHook(new Thread(()-> {
			instance.client.setStop(true);
			if (!instance.client.isClosed()) {
				if (authed)
					client.send(gson.toJson(new TServerStoppedPacket(cfg.getString("token"))));
				instance.client.close();
			}
		}));
		getServer().getMessenger().registerIncomingPluginChannel(this,"rezxis",new PMessageListener());
	}
	
	public void onDisable() {
		if (authed) 
			client.send(gson.toJson(new TServerStoppedPacket(cfg.getString("token"))));
		client.setStop(true);
		client.close();
	}
	
	private void loadConfig() {
		this.saveDefaultConfig();
		if (cfg != null)
			this.reloadConfig();
		cfg = this.getConfig();
	}
	
	private class PMessageListener implements PluginMessageListener {

		@Override
		public void onPluginMessageReceived(String ch, Player player, byte[] body) {
			if (ch.equalsIgnoreCase("rezxis")) {
				DataInputStream in = new DataInputStream(new ByteArrayInputStream(body));
				String arg0 = null;
				String arg1 = null;
				try {
					arg0 = in.readUTF();
					arg1 = in.readUTF();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (arg0.equalsIgnoreCase("vote")) {
					if (!cfg.getString("vote").isEmpty()) {
						Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cfg.getString("vote").replace("[player]", arg1));
					}
				}
			}
		}
	}
	
	class WSClient extends WebSocketClient {

		@Getter@Setter
		private boolean stop;
		
		public WSClient(URI uri) {
			super(uri);
		}

		@Override
		public void onOpen(ServerHandshake handshakedata) {
			System.out.println("Connection to Rezxis was established!");
			this.send(gson.toJson(new TAuthServerPacket(cfg.getString("token"), version, Bukkit.getServerName(), Bukkit.getMotd(), Bukkit.getPort(), Bukkit.getMaxPlayers(), Bukkit.getOnlinePlayers().size(), cfg.getBoolean("visible"), Material.valueOf(cfg.getString("icon")).name())));
		}

		@Override
		public void onMessage(String message) {
			TPacket packet = gson.fromJson(message, TPacket.class);
			if (packet.getId() == 2) {
				TAuthServerResponse response = gson.fromJson(message, TAuthServerResponse.class);
				System.out.println("==============================");
				if (response.getCode() == -1) {
					System.out.println(ChatColor.RED+"Authentication was failed.");
					System.out.println(ChatColor.RED+"Reason : "+response.getReason());
					System.out.println(ChatColor.RED+"Please Contact in Discord Ticket.");
					getServer().getPluginManager().disablePlugin(instance);
				} else {
					System.out.println("Authentication was successful.");
					System.out.println("Port : "+Bukkit.getPort());
					System.out.println("Max players : "+Bukkit.getMaxPlayers());
					authed = true;
				}
				System.out.println("==============================");
			}
		}

		@Override
		public void onClose(int code, String reason, boolean remote) {
			if (!this.stop) {
				System.out.println("reconnect to rezxis after 5 seconds.");
				Bukkit.getScheduler().runTaskLaterAsynchronously(instance, new Runnable() {
					public void run() {
						try {
							instance.client = new WSClient(new URI(cfg.getString("gateway")));
							instance.client.connect();
						} catch (Exception e) {
							System.out.println("failed to connect.");
							e.printStackTrace();
						}
					}
				}, 20*5);
			}
		}

		@Override
		public void onError(Exception ex) {
			ex.printStackTrace();
		}
	}
}
