package com.snailscuffle.game.ratings;

import com.snailscuffle.game.Constants;

public class Ratings {
	
	public static RatingPair compute(int initialRatingOfWinner, int initialRatingOfLoser) {
		return compute(new RatingPair(initialRatingOfWinner, initialRatingOfLoser));
	}
	
	public static RatingPair compute(RatingPair initialRatings) {
		double winnerProbability = probabilityOfWinning(initialRatings.winner, initialRatings.loser);
		double loserProbability = 1 - winnerProbability;
		
		double revisedWinnerRating = initialRatings.winner + (1 - winnerProbability) * Constants.MAX_RATING_CHANGE;
		double revisedLoserRating = initialRatings.loser - loserProbability * Constants.MAX_RATING_CHANGE;
		
		return new RatingPair((int)revisedWinnerRating, (int)revisedLoserRating);
	}
	
	private static double probabilityOfWinning(int rating, int opponentRating) {
		int diff = rating - opponentRating;
		return 1 / (1 + Math.pow(2, -1.0*diff/Constants.LOGISTIC_CURVE_DIVISOR));
	}
	
}
