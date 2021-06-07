package metrics;

import org.apache.commons.rng.RandomProviderState;

import game.Game;
import other.context.Context;
import other.trial.Trial;
import search.mcts.MCTS;
import search.minimax.AlphaBetaSearch;

/**
 * Helpful functions for metric analysis.
 * 
 * @author Matthew.Stephenson
 */
public class Utils 
{
	
	//-------------------------------------------------------------------------

	/**
	 * @param game
	 * @param rngState
	 * @return A new context for a given game and RNG.
	 */
	public static Context setupNewContext(final Game game, final RandomProviderState rngState)
	{
		final Context context = new Context(game, new Trial(game));
		context.rng().restoreState(rngState);
		context.reset();
		context.state().initialise(context.currentInstanceContext().game());
		game.start(context);
		context.trial().setStatus(null);
		return context;
	}
	
	//-------------------------------------------------------------------------
	
	public static double UCTEvaluateState(final Context context, final int mover)
	{
		final MCTS agent = MCTS.createUCT();
		agent.initAI(context.game(), mover);
		agent.setAutoPlaySeconds(-1);
		agent.selectAction(context.game(), context, 0.1, -1, -1);		
		return agent.estimateValue();
	}
	
	//-------------------------------------------------------------------------
	
	public static double ABEvaluateState(final Context context, final int mover)
	{
		final AlphaBetaSearch agent = new AlphaBetaSearch(false);
		agent.initAI(context.game(), mover);
		return agent.alphaBeta(context, 0, -1, -1, mover, -1);
	}
	
	//-------------------------------------------------------------------------
	
	public static double HeuristicEvaluateState(final Context context, final int mover)
	{
		final AlphaBetaSearch agent = new AlphaBetaSearch(false);
		agent.initAI(context.game(), mover);
		final float heuristicScore = agent.heuristicValueFunction().computeValue(context, mover, AlphaBetaSearch.ABS_HEURISTIC_WEIGHT_THRESHOLD);
		return heuristicScore;
	}
	
	//-------------------------------------------------------------------------
	
}
