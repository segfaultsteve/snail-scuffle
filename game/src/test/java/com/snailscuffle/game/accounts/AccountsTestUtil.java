package com.snailscuffle.game.accounts;

import com.snailscuffle.game.blockchain.StateChangeFromBattle;
import com.snailscuffle.game.blockchain.StateChangeFromBattle.PlayerChange;
import com.snailscuffle.game.ratings.RatingPair;
import com.snailscuffle.game.ratings.Ratings;

public class AccountsTestUtil {
	
	public static StateChangeFromBattle changesFromBattle(Account winner, Account loser, int height, long blockId) {
		RatingPair newRatings = Ratings.compute(winner.rating, loser.rating);
		int newWinnerStreak = (winner.streak > 0) ? (winner.streak + 1) : 1;
		int newLoserStreak = (loser.streak < 0) ? (loser.streak - 1) : -1;
		
		return new StateChangeFromBattle(
			height,
			blockId,
			new PlayerChange(winner.numericId(), winner.rating, newRatings.winner, winner.streak, newWinnerStreak),
			new PlayerChange(loser.numericId(), loser.rating, newRatings.loser, loser.streak, newLoserStreak)
		);
	}
	
}
