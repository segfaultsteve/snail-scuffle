package com.snailscuffle.game;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
import com.snailscuffle.game.battle.EquipmentInfo;

public class InfoServlet extends HttpServlet {

	private static final String SNAILS_PATH = "/snails";
	private static final String WEAPONS_PATH = "/weapons";
	private static final String SHELLS_PATH = "/shells";
	private static final String ACCESSORIES_PATH = "/accessories";
	private static final String ITEMS_PATH = "/items";
	
	private static final Logger logger = LoggerFactory.getLogger(InfoServlet.class);

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("application/json; charset=UTF-8");
		
		List<EquipmentInfo> info = null;
		switch (request.getPathInfo()) {
		case SNAILS_PATH:
			info = describeSnails();
			break;

		case WEAPONS_PATH:
			info = describeWeapons();
			break;

		case SHELLS_PATH:
			info = describeShells();
			break;

		case ACCESSORIES_PATH:
			info = describeAccessories();
			break;

		case ITEMS_PATH:
			info = describeItems();
			break;

		default:
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		try {
			JsonUtil.serialize((Serializable) info, response.getWriter());
		} catch (IOException e) {
			logger.error("Unexpected error", e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().print(ErrorResponse.unexpectedError().because(e.getMessage()));
		}

		ServletUtil.markHandled(request);
	}

	private static List<EquipmentInfo> describeSnails() {
		List<EquipmentInfo> snails = new ArrayList<>();
		for (Snail snail : Snail.values()) {
			EquipmentInfo info = new EquipmentInfo(snail.name().toLowerCase(), snail.displayName, snail.description,
					snail.attack, snail.defense, snail.speed);
			snails.add(info);
		}
		return snails;
	}

	private static List<EquipmentInfo> describeWeapons() {
		List<EquipmentInfo> weapons = new ArrayList<>();
		for (Weapon weapon : Weapon.values()) {
			EquipmentInfo info = new EquipmentInfo(weapon.name().toLowerCase(), weapon.displayName, "", weapon.attack, 0, 0);
			info.other.put("apCost", weapon.apCost);
			weapons.add(info);
		}
		return weapons;
	}

	private static List<EquipmentInfo> describeShells() {
		List<EquipmentInfo> shells = new ArrayList<>();
		for (Shell shell: Shell.values()) {
			EquipmentInfo info = new EquipmentInfo(shell.name().toLowerCase(), shell.displayName, "", 0, shell.defense, shell.speed);
			shells.add(info);
		}
		return shells;
	}

	private static List<EquipmentInfo> describeAccessories() {
		List<EquipmentInfo> accessories = new ArrayList<>();
		for (Accessory accessory : Accessory.values()) {
			EquipmentInfo info = new EquipmentInfo(accessory.name().toLowerCase(), accessory.displayName, accessory.description,
					accessory.attack, accessory.defense, accessory.speed);
			accessories.add(info);
		}
		return accessories;
	}

	private static List<EquipmentInfo> describeItems() {
		List<EquipmentInfo> items = new ArrayList<>();
		for (Item item : Item.values()) {
			EquipmentInfo info = new EquipmentInfo(item.name().toLowerCase(), item.displayName, item.description, 0, 0, 0);
			items.add(info);
		}
		return items;
	}

}
