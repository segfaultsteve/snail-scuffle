package com.snailscuffle.game.info;

import static com.snailscuffle.common.battle.Constants.*;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.snailscuffle.common.ErrorResponse;
import com.snailscuffle.common.battle.Accessory;
import com.snailscuffle.common.battle.Item;
import com.snailscuffle.common.battle.Shell;
import com.snailscuffle.common.battle.Snail;
import com.snailscuffle.common.battle.Weapon;
import com.snailscuffle.common.util.JsonUtil;
import com.snailscuffle.common.util.ServletUtil;
import com.snailscuffle.game.GameSettings;
import com.snailscuffle.game.battle.EquipmentInfo;

public class InfoServlet extends HttpServlet {

	private static final String SNAILS_PATH = "/snails";
	private static final String WEAPONS_PATH = "/weapons";
	private static final String SHELLS_PATH = "/shells";
	private static final String ACCESSORIES_PATH = "/accessories";
	private static final String ITEMS_PATH = "/items";
	private static final String SERVERS_PATH = "/servers";
	
	private static final Logger logger = LoggerFactory.getLogger(InfoServlet.class);
	
	private final URI matchmakerUrl;
	private final URI apiUrl;
	
	public InfoServlet(GameSettings settings) {
		matchmakerUrl = settings.matchmakerUrl;
		apiUrl = settings.apiUrl;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("application/json; charset=UTF-8");
		
		if (request.getPathInfo() == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			ServletUtil.markHandled(request);
			return;
		}
		
		try {
			switch (request.getPathInfo()) {
			case SNAILS_PATH:
				response.getWriter().write(describeSnails());
				break;
			case WEAPONS_PATH:
				response.getWriter().write(describeWeapons());
				break;
			case SHELLS_PATH:
				response.getWriter().write(describeShells());
				break;
			case ACCESSORIES_PATH:
				response.getWriter().write(describeAccessories());
				break;
			case ITEMS_PATH:
				response.getWriter().write(describeItems());
				break;
			case SERVERS_PATH:
				response.getWriter().write(describeServers());
				break;
			default:
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				break;
			}
		} catch (Exception e) {
			logger.error("Unexpected error", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().print(ErrorResponse.unexpectedError().because(e.getMessage()));
		}
		
		ServletUtil.markHandled(request);
	}

	private static String describeSnails() throws IOException {
		List<EquipmentInfo> snails = new ArrayList<>();
		for (Snail snail : Snail.values()) {
			EquipmentInfo info = new EquipmentInfo(snail.name().toLowerCase(), snail.displayName, snail.description,
					snail.attack, snail.defense, snail.speed);
			snails.add(info);
		}
		return JsonUtil.serialize((Serializable) snails);
	}

	private static String describeWeapons() throws IOException {
		List<EquipmentInfo> weapons = new ArrayList<>();
		for (Weapon weapon : Weapon.values()) {
			EquipmentInfo info = new EquipmentInfo(weapon.name().toLowerCase(), weapon.displayName, "", weapon.attack, 0, 0);
			info.other.put("apCost", weapon.apCost);
			weapons.add(info);
		}
		return JsonUtil.serialize((Serializable) weapons);
	}

	private static String describeShells() throws IOException {
		List<EquipmentInfo> shells = new ArrayList<>();
		for (Shell shell: Shell.values()) {
			EquipmentInfo info = new EquipmentInfo(shell.name().toLowerCase(), shell.displayName, "", 0, shell.defense, shell.speed);
			shells.add(info);
		}
		return JsonUtil.serialize((Serializable) shells);
	}

	private static String describeAccessories() throws IOException {
		List<EquipmentInfo> accessories = new ArrayList<>();
		for (Accessory accessory : Accessory.values()) {
			EquipmentInfo info = new EquipmentInfo(accessory.name().toLowerCase(), accessory.displayName, accessory.description,
					accessory.attack, accessory.defense, accessory.speed);
			if (accessory == Accessory.CHARGED_ATTACK) {
				info.other.put("divisor", CHARGED_ATTACK_AP_DIVISOR);
			} else if (accessory == Accessory.ADRENALINE) {
				info.other.put("crossover", ADRENALINE_CROSSOVER);
				info.other.put("divisor", ADRENALINE_DIVISOR);
			}
			accessories.add(info);
		}
		return JsonUtil.serialize((Serializable) accessories);
	}

	private static String describeItems() throws IOException {
		List<EquipmentInfo> items = new ArrayList<>();
		for (Item item : Item.values()) {
			EquipmentInfo info = new EquipmentInfo(item.name().toLowerCase(), item.displayName, item.description, 0, 0, 0);
			items.add(info);
		}
		return JsonUtil.serialize((Serializable) items);
	}
	
	private String describeServers() throws IOException {
		Map<String, String> servers = new HashMap<>();
		servers.put("matchmaker", matchmakerUrl.toString());
		servers.put("api", apiUrl.toString());
		return JsonUtil.serialize((Serializable) servers);
	}

}
