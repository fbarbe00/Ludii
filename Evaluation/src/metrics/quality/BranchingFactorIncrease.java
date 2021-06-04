package metrics.quality;

import org.apache.commons.rng.RandomProviderState;

import game.Game;
import metrics.Metric;
import metrics.Utils;
import other.context.Context;
import other.trial.Trial;

/**
 * Metric that measures the increase in average number of moves per turn
 * 
 * @author matthew.stephenson
 */
public class BranchingFactorIncrease extends Metric
{

	//-------------------------------------------------------------------------

	/**
	 * Constructor
	 */
	public BranchingFactorIncrease()
	{
		super
		(
			"Branching Factor Increase", 
			"Increase in average branching factor over all trials.", 
			"Core Ludii metric.", 
			MetricType.OUTCOMES,
			-1, 
			-1,
			0.0,
			null
		);
	}
	
	//-------------------------------------------------------------------------
	
	@Override
	public double apply
	(
			final Game game,
			final String args, 
			final Trial[] trials,
			final RandomProviderState[] randomProviderStates
	)
	{
		if (trials.length == 0)
			return 0;
		
		double avgBranchingFactor = 0;
		for (int trialIndex = 0; trialIndex < trials.length; trialIndex++)
		{
			// Get trial and RNG information
			final Trial trial = trials[trialIndex];
			final RandomProviderState rngState = randomProviderStates[trialIndex];
			
			// Setup a new instance of the game
			final Context context = Utils.setupNewContext(game, rngState);
			
			// Record the number of possible options for each move.
			double legalMovesSizes = 0;

			int lastMovesSize = context.game().moves(context).moves().size();
			for (int i = trial.numInitialPlacementMoves(); i < trial.numMoves()-1; i++)
			{
				context.game().apply(context, trial.getMove(i));
				legalMovesSizes += context.game().moves(context).moves().size() - lastMovesSize;
				lastMovesSize = context.game().moves(context).moves().size();
			}
			
			final int numMoves = trial.numMoves() - trial.numInitialPlacementMoves();
			avgBranchingFactor += legalMovesSizes / numMoves;
		}

		return avgBranchingFactor / trials.length;
	}

	//-------------------------------------------------------------------------

}
