package com.snailscuffle.simulator;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.snailscuffle.common.battle.BattlePlan;
import com.snailscuffle.common.battle.Instruction;
import com.snailscuffle.common.util.JsonUtil;

public class DatabaseUtil {

	private static final Logger logger = LoggerFactory.getLogger(Main.class);
	private static SimulatorSettings simulatorSettings = new SimulatorSettings();
	private static String url = "jdbc:sqlite:" + simulatorSettings.databaseFilePath;
	private static int insertsInCurrentTransaction = 0;
	private static Connection connection;
	private static PreparedStatement statement;
	String sql = "INSERT INTO " + simulatorSettings.tableName + "VALUES (?,?,?,?,?,?,?,?,?,?)";

	private static final int MAX_BATTLEPLANS_IN_TRANSACTION = 100000;

	public DatabaseUtil() {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			logger.error("Error loading JDBC lib needed for SQL operations. Error: " + e.getMessage());
		}

		connection = connect();
	}

	private static Connection connect() {
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(url);
			CreateNewTableIfNecessary(conn);
			conn.setAutoCommit(false); // transaction block start
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
		return conn;
	}

	public static void StoreResult(List<BattlePlan> battlePlans, BattleResultMetaData metaData, int battleId) {
		String sql = "INSERT INTO " + simulatorSettings.tableName + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
		try {
			if (statement == null) {
				statement = connection.prepareStatement(sql);
			}
			
			int battlePlanIterator = 0;
			
			if (metaData.wonGame == -1) {
				logger.info("-1 for winner");
			}
			
			for (BattlePlan battlePlan : battlePlans) {
				statement.setString(1, battlePlan.snail.toString());
				statement.setString(2, battlePlan.weapon.toString());
				statement.setString(3, battlePlan.shell.toString());
				statement.setString(4, battlePlan.accessory.toString());
				statement.setString(5, battlePlan.items[0].toString());
				statement.setString(6, battlePlan.items[1].toString());
				// statement.setString(7, battlePlan.item1Rule.toString());
				// statement.setString(8, battlePlan.item2Rule.toString());
				statement.setString(9, convertInstructionsArrayToString(battlePlan));
				statement.setInt(10, (metaData.wonGame == battlePlanIterator % 2) ? 1 : 0);
				statement.setInt(11, (metaData.wonGame == battlePlanIterator % 2) ? metaData.marginOfVictory : (0 - metaData.marginOfVictory));
				statement.setInt(12, (metaData.wonGame == battlePlanIterator % 2) ? metaData.timeToVictory : (0 - metaData.timeToVictory));
				statement.setInt(13, battleId);
				statement.executeUpdate();
				battlePlanIterator++;
			}

			if (insertsInCurrentTransaction >= MAX_BATTLEPLANS_IN_TRANSACTION) {
				connection.commit();
				statement.close();
				statement = null;
				logger.debug("Pushing " + insertsInCurrentTransaction + " transactions.");
				insertsInCurrentTransaction = 0;
			} else {
				insertsInCurrentTransaction++;
			}
		} catch (SQLException e) {
			logger.error("Error when trying to write result to database: " + e.getMessage());
		}
	}

	private static String convertInstructionsArrayToString(BattlePlan battlePlan) {

		StringBuilder stringOfInstructions = new StringBuilder();
		int instructionNumber = 1;

		for (Instruction instruction : battlePlan.instructions) {
			stringOfInstructions.append(instructionNumber + ". ");
			stringOfInstructions.append(JsonUtil.serialize(instruction));
			stringOfInstructions.append("\n");
			instructionNumber++;
		}

		return stringOfInstructions.toString();
	}

	private static void CreateNewTableIfNecessary(Connection conn) throws SQLException {
		DatabaseMetaData dbm = conn.getMetaData();
		ResultSet resultSet = dbm.getTables(null, null, simulatorSettings.tableName, null);
		if (!resultSet.next()) {
			String newTableSql = "CREATE TABLE IF NOT EXISTS " + simulatorSettings.tableName + " (\n"
					+ "	snail string,\n" + "	weapon string,\n" + "	shell string,\n" + "	accessory string,\n"
					+ "	item1 string,\n" + "	item2 string,\n" + "	item1Rule string,\n" + "	item2Rule string,\n"
					+ "	instructions string,\n" + "	win integer,\n" + "	marginOfVictory integer,\n"
					+ "	timeToVictory integer,\n" + "	id int\n" + ");";
			Statement statement = conn.createStatement();
			statement.execute(newTableSql);
		}
	}

}
