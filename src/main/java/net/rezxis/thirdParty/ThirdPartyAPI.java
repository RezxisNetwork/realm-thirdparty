package net.rezxis.thirdParty;

import org.bukkit.Material;

import com.google.gson.Gson;

import net.rezxis.thirdParty.packet.TServerUpdatePacket;

public class ThirdPartyAPI {

	public static void updateMotd(String motd) {
		post(null, motd, null);
	}
	
	public static void updateName(String name) {
		post(name, null, null);
	}
	
	public static void updateIcon(Material icon) {
		post(null, null, icon.name());
	}
	
	public static void post(String name, String motd, String icon) {
		if (ThirdParty.getInstance().getClient().isClosed()) {
			return;
		}
		ThirdParty.getInstance().getClient().send(new Gson().toJson(new TServerUpdatePacket(ThirdParty.getInstance().getCfg().getString("token"), name, motd, icon)));
	}
}
