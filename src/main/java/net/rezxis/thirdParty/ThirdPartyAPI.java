package net.rezxis.thirdParty;

import org.bukkit.Material;

import com.google.gson.Gson;

import net.rezxis.thirdParty.packet.TServerUpdatePacket;

public class ThirdPartyAPI {

	public void updateMotd(String motd) {
		post(null, motd, null);
	}
	
	public void updateName(String name) {
		post(name, null, null);
	}
	
	public void updateIcon(Material mat) {
		post(null, null, mat.name());
	}
	
	private void post(String name, String motd, String icon) {
		if (ThirdParty.getInstance().getClient().isClosed()) {
			return;
		}
		ThirdParty.getInstance().getClient().send(new Gson().toJson(new TServerUpdatePacket(ThirdParty.getInstance().getCfg().getString("token"), name, motd, icon)));
	}
}
