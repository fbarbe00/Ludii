package metadata.graphics;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import annotations.Or;
import game.Game;
import game.equipment.container.Container;
import game.equipment.other.Regions;
import game.functions.ints.board.Id;
import game.types.board.RelationType;
import game.types.board.ShapeType;
import game.types.board.SiteType;
import game.types.component.SuitType;
import game.types.play.RoleType;
import main.StringRoutines;
import metadata.graphics.board.Boolean.BoardCheckered;
import metadata.graphics.board.colour.BoardColour;
import metadata.graphics.board.ground.BoardBackground;
import metadata.graphics.board.ground.BoardForeground;
import metadata.graphics.board.placement.BoardPlacement;
import metadata.graphics.board.shape.BoardShape;
import metadata.graphics.board.style.BoardStyle;
import metadata.graphics.board.styleThickness.BoardStyleThickness;
import metadata.graphics.hand.placement.HandPlacement;
import metadata.graphics.no.Boolean.NoAnimation;
import metadata.graphics.no.Boolean.NoBoard;
import metadata.graphics.no.Boolean.NoCurves;
import metadata.graphics.no.Boolean.NoDicePips;
import metadata.graphics.no.Boolean.NoHandScale;
import metadata.graphics.no.Boolean.NoSunken;
import metadata.graphics.others.AutoPass;
import metadata.graphics.others.HiddenImage;
import metadata.graphics.others.StackType;
import metadata.graphics.others.SuitRanking;
import metadata.graphics.piece.colour.PieceColour;
import metadata.graphics.piece.families.PieceFamilies;
import metadata.graphics.piece.ground.PieceBackground;
import metadata.graphics.piece.ground.PieceForeground;
import metadata.graphics.piece.name.PieceAddStateToName;
import metadata.graphics.piece.name.PieceExtendName;
import metadata.graphics.piece.name.PieceRename;
import metadata.graphics.piece.rotate.PieceRotate;
import metadata.graphics.piece.scale.PieceScale;
import metadata.graphics.piece.style.PieceStyle;
import metadata.graphics.player.colour.PlayerColour;
import metadata.graphics.player.name.PlayerName;
import metadata.graphics.puzzle.AdversarialPuzzle;
import metadata.graphics.puzzle.DrawHint;
import metadata.graphics.puzzle.HintLocation;
import metadata.graphics.region.colour.RegionColour;
import metadata.graphics.show.Boolean.ShowCost;
import metadata.graphics.show.Boolean.ShowCurvedEdges;
import metadata.graphics.show.Boolean.ShowEdgeDirections;
import metadata.graphics.show.Boolean.ShowLocalStateHoles;
import metadata.graphics.show.Boolean.ShowPits;
import metadata.graphics.show.Boolean.ShowPlayerHoles;
import metadata.graphics.show.Boolean.ShowPossibleMoves;
import metadata.graphics.show.Boolean.ShowRegionOwner;
import metadata.graphics.show.Boolean.ShowStraightEdges;
import metadata.graphics.show.check.ShowCheck;
import metadata.graphics.show.component.ShowPieceState;
import metadata.graphics.show.component.ShowPieceValue;
import metadata.graphics.show.edges.ShowEdges;
import metadata.graphics.show.line.ShowLine;
import metadata.graphics.show.score.ShowScore;
import metadata.graphics.show.sites.ShowSitesAsHoles;
import metadata.graphics.show.sites.ShowSitesShape;
import metadata.graphics.show.symbol.ShowSymbol;
import metadata.graphics.util.BoardGraphicsType;
import metadata.graphics.util.ComponentStyleType;
import metadata.graphics.util.ContainerStyleType;
import metadata.graphics.util.EdgeInfoGUI;
import metadata.graphics.util.EdgeType;
import metadata.graphics.util.HoleType;
import metadata.graphics.util.MetadataFunctions;
import metadata.graphics.util.MetadataImageInfo;
import metadata.graphics.util.PieceStackType;
import metadata.graphics.util.PuzzleDrawHintType;
import metadata.graphics.util.PuzzleHintLocationType;
import metadata.graphics.util.ScoreDisplayInfo;
import metadata.graphics.util.ValueDisplayInfo;
import metadata.graphics.util.colour.Colour;
import other.context.Context;

/**
 * Graphics hints for rendering the game.
 * 
 * @author matthew.stephenson and cambolbro
 */
public class Graphics implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	//-------------------------------------------------------------------------

	final List<GraphicsItem> items = new ArrayList<GraphicsItem>();
	
	String errorReport = "";
	
	/** The graphics need to be redraw after each move. */
	private boolean needRedraw = false;
	
	final float EPSILON = 0.00001f; 

	//-------------------------------------------------------------------------

	/**
	 * Graphics hints for rendering the game.
	 * 
	 * @param item  The graphic item of the game.
	 * @param items The graphic items of the game.
	 */
	public Graphics
	(
		@Or	final GraphicsItem item,
		@Or	final GraphicsItem[] items
	)
	{
		int numNonNull = 0;
		if (item != null)
			numNonNull++;
		if (items != null)
			numNonNull++;

		if (numNonNull > 1)
			throw new IllegalArgumentException("Only one of @Or should be different to null");

		if(items != null)
		for (final GraphicsItem i : items)
			this.items.add(i);
		else
			this.items.add(item);
	}

	//-------------------------------------------------------------------------
	
	/**
	 * @param playerIndex The index of the player.
	 * @param pieceName   The name of the piece
	 * @param context     The context.
	 * @param state		  The state
	 * @return True if done.
	 */
	public boolean addStateToName(final int playerIndex, final String pieceName, final Context context, final int state)
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof PieceAddStateToName)
				if (((PieceAddStateToName) graphicsItem).roleType() == null || MetadataFunctions.getRealOwner(context, ((PieceAddStateToName) graphicsItem).roleType()) == playerIndex)
					if (((PieceAddStateToName) graphicsItem).state() == null || ((PieceAddStateToName) graphicsItem).state().intValue() == state)
						if (((PieceAddStateToName) graphicsItem).piece() == null || ((PieceAddStateToName) graphicsItem).piece().equals(pieceName) || ((PieceAddStateToName) graphicsItem).piece().equals(StringRoutines.removeTrailingNumbers(pieceName)))
							return true;

		return false;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param playerIndex The index of the player.
	 * @param pieceName   The name of the piece.
	 * @param context     The context.
	 * @return The component style type.
	 */
	public ComponentStyleType componentStyle(final int playerIndex, final String pieceName, final Context context)
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof PieceStyle)
				if (((PieceStyle) graphicsItem).roleType() == null || MetadataFunctions.getRealOwner(context, ((PieceStyle) graphicsItem).roleType()) == playerIndex)
					if (((PieceStyle) graphicsItem).pieceName() == null || ((PieceStyle) graphicsItem).pieceName().equals(pieceName) || ((PieceStyle) graphicsItem).pieceName().equals(StringRoutines.removeTrailingNumbers(pieceName)))
						return ((PieceStyle) graphicsItem).componentStyleType();

		return null;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param playerIndex The index of the player.
	 * @param pieceName   The name of the piece.
	 * @param context     The context.
	 * @return The ValueDisplayInfo for the local state.
	 */
	public ValueDisplayInfo displayPieceState(final int playerIndex, final String pieceName, final Context context)
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof ShowPieceState)
				if (((ShowPieceState) graphicsItem).roleType() == null || MetadataFunctions.getRealOwner(context, ((ShowPieceState) graphicsItem).roleType()) == playerIndex)
					if (((ShowPieceState) graphicsItem).pieceName() == null || ((ShowPieceState) graphicsItem).pieceName().equals(pieceName) || ((ShowPieceState) graphicsItem).pieceName().equals(StringRoutines.removeTrailingNumbers(pieceName)))
						return new ValueDisplayInfo(((ShowPieceState) graphicsItem).location(), ((ShowPieceState) graphicsItem).offsetImage(), ((ShowPieceState) graphicsItem).valueOutline());

		return new ValueDisplayInfo();
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param playerIndex The index of the player.
	 * @param pieceName   The name of the piece.
	 * @param context     The context.
	 * @return The ValueDisplayInfo for the value.
	 */
	public ValueDisplayInfo displayPieceValue(final int playerIndex, final String pieceName, final Context context)
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof ShowPieceValue)
				if (((ShowPieceValue) graphicsItem).roleType() == null || MetadataFunctions.getRealOwner(context, ((ShowPieceValue) graphicsItem).roleType()) == playerIndex)
					if (((ShowPieceValue) graphicsItem).pieceName() == null || ((ShowPieceValue) graphicsItem).pieceName().equals(pieceName) || ((ShowPieceValue) graphicsItem).pieceName().equals(StringRoutines.removeTrailingNumbers(pieceName)))
						return new ValueDisplayInfo(((ShowPieceValue) graphicsItem).location(), ((ShowPieceValue) graphicsItem).offsetImage(), ((ShowPieceValue) graphicsItem).valueOutline());

		return new ValueDisplayInfo();
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param context     The context.
	 * @return The metadataImageInfo.
	 */
	public ArrayList<MetadataImageInfo> boardBackground(final Context context)
	{
		final float MAX_SCALE  = 100f;  // absolute scale in world coordinates
		final int MAX_ROTATION = 360;  // degrees
		final float MIN_OFFSET = -1f; // world coordinates 
		final float MAX_OFFSET = 1f; // world coordinates
		
		final ArrayList<MetadataImageInfo> allBackgrounds = new ArrayList<>();
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof BoardBackground)
			{
				final BoardBackground    boardBackground = (BoardBackground)graphicsItem;
				final Colour fillColourMeta = boardBackground.fillColour();
				final Colour edgeColourMeta = boardBackground.edgeColour();
				final Color  fillColour     = (fillColourMeta == null) ? null : fillColourMeta.colour();
				final Color  edgeColour     = (edgeColourMeta == null) ? null : edgeColourMeta.colour();
				
				if (boardBackground.scale() >= 0 && boardBackground.scale() <= MAX_SCALE)
				{
					if (boardBackground.rotation() >= 0 && boardBackground.rotation() <= MAX_ROTATION)
					{
						if (boardBackground.offsetX() >= MIN_OFFSET && boardBackground.offsetX() <= MAX_OFFSET)
						{
							if (boardBackground.offsetY() >= MIN_OFFSET && boardBackground.offsetY() <= MAX_OFFSET)
							{
								if (Math.abs(boardBackground.scale() - 1.0) > EPSILON)
									allBackgrounds.add
									(
										new MetadataImageInfo
										(
											-1, null, 
											boardBackground.background(), boardBackground.scale(), 
											fillColour, edgeColour,
											boardBackground.rotation(), boardBackground.offsetX(), boardBackground.offsetY()
										)
									);
								else
									allBackgrounds.add
									(
										new MetadataImageInfo
										(
											-1, null, 
											boardBackground.background(), boardBackground.scaleX(), boardBackground.scaleY(), 
											fillColour,	edgeColour,
											boardBackground.rotation(), boardBackground.offsetX(), boardBackground.offsetY()
										)
									);
							}
							else
							{
								addError("Offset Y for board background was equal to " + boardBackground.offsetY() + ", offset must be between -1 and 1");
							}
						}
						else
						{
							addError("Offset X for board background was equal to " + boardBackground.offsetX() + ", offset must be between -1 and 1");
						}
					}
					else
					{
						addError("Rotation for board background was equal to " + boardBackground.rotation() + ", rotation must be between 0 and 360");
					}
				}
				else 
				{
					addError("Scale for board background was equal to " + boardBackground.scale() + ", scale must be between 0 and 100");
				}
			}
		return allBackgrounds;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param context     The context.
	 * @return The metadataImageInfo.
	 */
	public ArrayList<MetadataImageInfo> boardForeground(final Context context)
	{
		final ArrayList<MetadataImageInfo> allForegrounds = new ArrayList<>();
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof BoardForeground)
			{
				final Colour fillColourMeta = ((BoardForeground) graphicsItem).fillColour();
				final Color fillColour = (fillColourMeta == null) ? null : fillColourMeta.colour();
				final Colour edgeColourMeta = ((BoardForeground) graphicsItem).edgeColour();
				final Color edgeColour = (edgeColourMeta == null) ? null : edgeColourMeta.colour();
				
				if (((BoardForeground) graphicsItem).scale() >= 0 && ((BoardForeground) graphicsItem).scale() <= 100)
					if (((BoardForeground) graphicsItem).rotation() >= 0 && ((BoardForeground) graphicsItem).rotation() <= 360)
						if (((BoardForeground) graphicsItem).offsetX() >= -1 && ((BoardForeground) graphicsItem).offsetX() <= 1)
							if (((BoardForeground) graphicsItem).offsetY() >= -1 && ((BoardForeground) graphicsItem).offsetY() <= 1)
								if (((BoardForeground) graphicsItem).scale() != 1.0)
									allForegrounds.add(new MetadataImageInfo(-1, null, 
											((BoardForeground) graphicsItem).foreground(), 
											((BoardForeground) graphicsItem).scale(), 
											fillColour,
											edgeColour,
											((BoardForeground) graphicsItem).rotation(),
											((BoardForeground) graphicsItem).offsetX(),
											((BoardForeground) graphicsItem).offsetY()
											));
								else
									allForegrounds.add(new MetadataImageInfo(-1, null, 
											((BoardForeground) graphicsItem).foreground(), 
											((BoardForeground) graphicsItem).scaleX(), 
											((BoardForeground) graphicsItem).scaleY(), 
											fillColour,
											edgeColour,
											((BoardForeground) graphicsItem).rotation(),
											((BoardForeground) graphicsItem).offsetX(),
											((BoardForeground) graphicsItem).offsetY()
											));
							else
								addError("Offset Y for board foreground was equal to " + ((BoardForeground) graphicsItem).offsetY() + ", offset must be between -1 and 1");
						else
							addError("Offset X for board foreground was equal to " + ((BoardForeground) graphicsItem).offsetX() + ", offset must be between -1 and 1");
					else
						addError("Rotation for board foreground was equal to " + ((BoardForeground) graphicsItem).rotation() + ", rotation must be between 0 and 360");
				else
					addError("Scale for board foreground was equal to " + ((BoardForeground) graphicsItem).scale() + ", scale must be between 0 and 100");
			}
		return allForegrounds;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param playerIndex The index of the player.
	 * @param pieceName   The name of the piece.
	 * @param context     The context.
	 * @param state       The state.
	 * @param value       The value.
	 * @return The metadataImageInfo.
	 */
	public ArrayList<MetadataImageInfo> pieceBackground(final int playerIndex, final String pieceName, final Context context, final int state, final int value)
	{
		final ArrayList<MetadataImageInfo> allBackground = new ArrayList<>();
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof PieceBackground)
				if (((PieceBackground) graphicsItem).roleType() == null || MetadataFunctions.getRealOwner(context, ((PieceBackground) graphicsItem).roleType()) == playerIndex)
					if (((PieceBackground) graphicsItem).state() == null || ((PieceBackground) graphicsItem).state().intValue() == state)
						if (((PieceBackground) graphicsItem).value() == null || ((PieceBackground) graphicsItem).value().intValue() == value)
							if (((PieceBackground) graphicsItem).pieceName() == null || ((PieceBackground) graphicsItem).pieceName().equals(pieceName) || ((PieceBackground) graphicsItem).pieceName().equals(StringRoutines.removeTrailingNumbers(pieceName)))
							{
								final Colour fillColourMeta = ((PieceBackground) graphicsItem).fillColour();
								final Color fillColour = (fillColourMeta == null) ? null : fillColourMeta.colour();
								final Colour edgeColourMeta = ((PieceBackground) graphicsItem).edgeColour();
								final Color edgeColour = (edgeColourMeta == null) ? null : edgeColourMeta.colour();
	
								if (((PieceBackground) graphicsItem).scale() >= 0 && ((PieceBackground) graphicsItem).scale() <= 100)
									if (((PieceBackground) graphicsItem).rotation() >= 0 && ((PieceBackground) graphicsItem).rotation() <= 360)
										allBackground.add(new MetadataImageInfo(-1, null, 
												((PieceBackground) graphicsItem).background(), 
												((PieceBackground) graphicsItem).scale(), 
												fillColour,
												edgeColour,
												((PieceBackground) graphicsItem).rotation()));
									else
										addError("Rotation for background of piece " + pieceName + " was equal to " + ((PieceBackground) graphicsItem).scale() + ", rotation must be between 0 and 360");
								else
									addError("Scale for background of piece " + pieceName + " was equal to " + ((PieceBackground) graphicsItem).scale() + ", scale must be between 0 and 100");
							}

		return allBackground;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param playerIndex The index of the player.
	 * @param pieceName   The name of the piece.
	 * @param context     The context.
	 * @param state       The state.
	 * @param value       The value.
	 * @return The MetadataImageInfo
	 */
	public ArrayList<MetadataImageInfo> pieceForeground(final int playerIndex, final String pieceName, final Context context, final int state, final int value)
	{
		final ArrayList<MetadataImageInfo> allForeground = new ArrayList<>();
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof PieceForeground)
				if (((PieceForeground) graphicsItem).roleType() == null || MetadataFunctions.getRealOwner(context, ((PieceForeground) graphicsItem).roleType()) == playerIndex)
					if (((PieceForeground) graphicsItem).state() == null || ((PieceForeground) graphicsItem).state().intValue() == state)
						if (((PieceForeground) graphicsItem).value() == null || ((PieceForeground) graphicsItem).value().intValue() == value)
							if (((PieceForeground) graphicsItem).pieceName() == null || ((PieceForeground) graphicsItem).pieceName().equals(pieceName) || ((PieceForeground) graphicsItem).pieceName().equals(StringRoutines.removeTrailingNumbers(pieceName)))
							{
								final Colour fillColourMeta = ((PieceForeground) graphicsItem).fillColour();
								final Color fillColour = (fillColourMeta == null) ? null : fillColourMeta.colour();
								final Colour edgeColourMeta = ((PieceForeground) graphicsItem).edgeColour();
								final Color edgeColour = (edgeColourMeta == null) ? null : edgeColourMeta.colour();
	
								if (((PieceForeground) graphicsItem).scale() >= 0 && ((PieceForeground) graphicsItem).scale() <= 100)
									if (((PieceForeground) graphicsItem).rotation() >= 0 && ((PieceForeground) graphicsItem).rotation() <= 360)
										allForeground.add(new MetadataImageInfo(-1, null,
												((PieceForeground) graphicsItem).foreground(), 
												((PieceForeground) graphicsItem).scale(), 
												fillColour,
												edgeColour,
												((PieceForeground) graphicsItem).rotation()));
									else
										addError("Rotation for foreground of piece " + pieceName + " was equal to " + ((PieceBackground) graphicsItem).scale() + ", rotation must be between 0 and 360");
								else
									addError("Scale for foreground of piece " + pieceName + " was equal to " + ((PieceForeground) graphicsItem).scale() + ", scale must be between 0 and 100");
							}
	
		return allForeground;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return The hidden image name.
	 */
	public String pieceHiddenImage()
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof HiddenImage)
				return ((HiddenImage) graphicsItem).hiddenImage();
	
		return null;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param playerIndex The index of the player.
	 * @param pieceName   The name of the piece.
	 * @param context     The context.
	 * @param state       The state.
	 * @param value       The value.
	 * @return The colour.
	 */
	public Color pieceFillColour(final int playerIndex, final String pieceName, final Context context, final int state, final int value)
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof PieceColour)
				if (((PieceColour) graphicsItem).roleType() == null || MetadataFunctions.getRealOwner(context, ((PieceColour) graphicsItem).roleType()) == playerIndex)
					if (((PieceColour) graphicsItem).state() == null || ((PieceColour) graphicsItem).state().intValue() == state)
						if (((PieceColour) graphicsItem).value() == null || ((PieceColour) graphicsItem).value().intValue() == value)
							if (((PieceColour) graphicsItem).pieceName() == null || ((PieceColour) graphicsItem).pieceName().equals(pieceName) || ((PieceColour) graphicsItem).pieceName().equals(StringRoutines.removeTrailingNumbers(pieceName)))
							{
								final Colour colourMeta = ((PieceColour) graphicsItem).fillColour();
								return (colourMeta == null) ? null : colourMeta.colour();
							}

		return null;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param playerIndex The index of the player.
	 * @param pieceName   The name of the piece.
	 * @param context     The context.
	 * @param state       The state.
	 * @param value       The value.
	 * @return The colour.
	 */
	public Color pieceSecondaryColour(final int playerIndex, final String pieceName, final Context context, final int state, final int value)
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof PieceColour)
				if (((PieceColour) graphicsItem).roleType() == null || MetadataFunctions.getRealOwner(context, ((PieceColour) graphicsItem).roleType()) == playerIndex)
					if (((PieceColour) graphicsItem).state() == null || ((PieceColour) graphicsItem).state().intValue() == state)
						if (((PieceColour) graphicsItem).value() == null || ((PieceColour) graphicsItem).value().intValue() == value)
							if (((PieceColour) graphicsItem).pieceName() == null || ((PieceColour) graphicsItem).pieceName().equals(pieceName) || ((PieceColour) graphicsItem).pieceName().equals(StringRoutines.removeTrailingNumbers(pieceName)))
							{
								final Colour colourMeta = ((PieceColour) graphicsItem).secondaryColour();
								return (colourMeta == null) ? null : colourMeta.colour();
							}

		return null;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param playerIndex The index of the player.
	 * @param pieceName   The name of the piece.
	 * @param context     The context.
	 * @param state       The state.
	 * @param value       The value.
	 * @return The colour.
	 */
	public Color pieceEdgeColour(final int playerIndex, final String pieceName, final Context context, final int state, final int value)
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof PieceColour)
				if (((PieceColour) graphicsItem).roleType() == null || MetadataFunctions.getRealOwner(context, ((PieceColour) graphicsItem).roleType()) == playerIndex)
					if (((PieceColour) graphicsItem).state() == null || ((PieceColour) graphicsItem).state().intValue() == state)
						if (((PieceColour) graphicsItem).value() == null || ((PieceColour) graphicsItem).value().intValue() == value)
							if (((PieceColour) graphicsItem).pieceName() == null || ((PieceColour) graphicsItem).pieceName().equals(pieceName) || ((PieceColour) graphicsItem).pieceName().equals(StringRoutines.removeTrailingNumbers(pieceName)))
							{
								final Colour colourMeta = ((PieceColour) graphicsItem).strokeColour();
								return (colourMeta == null) ? null : colourMeta.colour();
							}

		return null;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return The list of the families pieces.
	 */
	public String[] pieceFamilies()
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof PieceFamilies)
				return ((PieceFamilies) graphicsItem).pieceFamilies();

		return null;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param playerIndex The index of the player.
	 * @param pieceName   The name of the piece.
	 * @param context     The context.
	 * @param state       The state.
	 * @param value       The value.
	 * @return the degrees to rotate the piece image (clockwise).
	 */
	public int pieceRotate(final int playerIndex, final String pieceName, final Context context, final int state, final int value)
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof PieceRotate)
				if (((PieceRotate) graphicsItem).roleType() == null || MetadataFunctions.getRealOwner(context, ((PieceRotate) graphicsItem).roleType()) == playerIndex)
					if (((PieceRotate) graphicsItem).state() == null || ((PieceRotate) graphicsItem).state().intValue() == state)
						if (((PieceRotate) graphicsItem).value() == null || ((PieceRotate) graphicsItem).value().intValue() == value)
							if (((PieceRotate) graphicsItem).pieceName() == null || ((PieceRotate) graphicsItem).pieceName().equals(pieceName) || ((PieceRotate) graphicsItem).pieceName().equals(StringRoutines.removeTrailingNumbers(pieceName)))
								if (((PieceRotate) graphicsItem).degrees() >= 0 && ((PieceRotate) graphicsItem).degrees() <= 360)
									return ((PieceRotate) graphicsItem).degrees();
								else
									addError("Rotation for peice" + pieceName + "was equal to " + ((PieceRotate) graphicsItem).degrees() + ", rotation must be between 0 and 360");

		return 0;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param playerIndex The index of the player.
	 * @param pieceName   The name of the piece.
	 * @param context     The context
	 * @param state       The state value.
	 * @return The piece name extended.
	 */
	public String pieceNameExtension(final int playerIndex, final String pieceName, final Context context, final int state)
	{
//		for (final GraphicsItem graphicsItem : items)
//			if (graphicsItem instanceof PieceExtendName)
//				if (((PieceExtendName) graphicsItem).roleType() == null || MetadataFunctions.getRealOwner(context, ((PieceExtendName) graphicsItem).roleType()) == playerIndex)
//					if (((PieceExtendName) graphicsItem).state() == null || ((PieceExtendName) graphicsItem).state().intValue() == state)
//						if (((PieceExtendName) graphicsItem).piece() == null || ((PieceExtendName) graphicsItem).piece().equals(pieceName) || ((PieceExtendName) graphicsItem).piece().equals(StringRoutines.removeTrailingNumbers(pieceName)))
//							return ((PieceExtendName) graphicsItem).nameExtension();

		for (final GraphicsItem graphicsItem : items)
			if 
			(
				graphicsItem instanceof PieceExtendName
				&&
				(
					((PieceExtendName)graphicsItem).roleType() == null 
					|| 
					MetadataFunctions.getRealOwner(context, ((PieceExtendName)graphicsItem).roleType()) == playerIndex
				)
				&&
				(
					((PieceExtendName)graphicsItem).state() == null 
					|| 
					((PieceExtendName)graphicsItem).state().intValue() == state
				)
				&&
				(
					((PieceExtendName)graphicsItem).piece() == null 
					|| 
					((PieceExtendName)graphicsItem).piece().equals(pieceName) 
					|| 
					((PieceExtendName)graphicsItem).piece().equals(StringRoutines.removeTrailingNumbers(pieceName))
				)
			)
				return ((PieceExtendName)graphicsItem).nameExtension();
		
		return "";
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param playerIndex The index of the player.
	 * @param pieceName   The name of the piece.
	 * @param context     The context.
	 * @param state       The state value.
	 * @return The new name.
	 */
	public String pieceNameReplacement(final int playerIndex, final String pieceName, final Context context, final int state)
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof PieceRename)
				if (((PieceRename) graphicsItem).roleType() == null || MetadataFunctions.getRealOwner(context, ((PieceRename) graphicsItem).roleType()) == playerIndex)
					if (((PieceRename) graphicsItem).state() == null || ((PieceRename) graphicsItem).state().intValue() == state)
						if (((PieceRename) graphicsItem).piece() == null || ((PieceRename) graphicsItem).piece().equals(pieceName) || ((PieceRename) graphicsItem).piece().equals(StringRoutines.removeTrailingNumbers(pieceName)))
							return ((PieceRename) graphicsItem).nameReplacement();

		return null;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param playerIndex The index of the player.
	 * @param pieceName   The name of the piece.
	 * @param context     The context.
	 * @return The new scale.
	 */
	public Point2D.Float pieceScale(final int playerIndex, final String pieceName, final Context context)
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof PieceScale)
				if (((PieceScale) graphicsItem).roleType() == null || MetadataFunctions.getRealOwner(context, ((PieceScale) graphicsItem).roleType()) == playerIndex)
					if (((PieceScale) graphicsItem).pieceName() == null || ((PieceScale) graphicsItem).pieceName().equals(pieceName) || ((PieceScale) graphicsItem).pieceName().equals(StringRoutines.removeTrailingNumbers(pieceName)))
					{
						
						float scaleX, scaleY;
						if (Math.abs(((PieceScale) graphicsItem).scale() - 1.0) > EPSILON)
						{
							scaleX = Math.abs(((PieceScale) graphicsItem).scale());
							scaleY = Math.abs(((PieceScale) graphicsItem).scale());
						}
						else
						{
							scaleX = Math.abs(((PieceScale) graphicsItem).scaleX());
							scaleY = Math.abs(((PieceScale) graphicsItem).scaleY());
						}
						
						return new Point2D.Float(scaleX, scaleY);
					}

		return new Point2D.Float((float)0.9, (float)0.9);
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param playerIndex
	 * @param pieceName
	 * @param context
	 * @return True if the check is used.
	 */
	public boolean checkUsed(final int playerIndex, final String pieceName, final Context context)
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof ShowCheck)
				if (((ShowCheck) graphicsItem).roleType() == null || MetadataFunctions.getRealOwner(context, ((ShowCheck) graphicsItem).roleType()) == playerIndex)
					if (((ShowCheck) graphicsItem).pieceName() == null || ((ShowCheck) graphicsItem).pieceName().equals(pieceName) || ((ShowCheck) graphicsItem).pieceName().equals(StringRoutines.removeTrailingNumbers(pieceName)))
						return true;

		return false;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param context The context.
	 * @return The list of the MetadataImageInfo.
	 */
	public ArrayList<MetadataImageInfo> drawSymbol(final Context context)
	{
		final ArrayList<MetadataImageInfo> allSymbols = new ArrayList<>();
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof ShowSymbol)
			{
				final Colour fillColourMeta = ((ShowSymbol) graphicsItem).fillColour();
				final Color fillColour = (fillColourMeta == null) ? null : fillColourMeta.colour();
				
				final Colour edgeColourMeta = ((ShowSymbol) graphicsItem).edgeColour();
				final Color edgeColour = (edgeColourMeta == null) ? null : edgeColourMeta.colour();
				
				final int rotation = ((ShowSymbol) graphicsItem).rotation();
				
				final float offsetX = ((ShowSymbol) graphicsItem).getOffsetX();
				final float offsetY = ((ShowSymbol) graphicsItem).getOffsetY();
				
				final SiteType graphElementType = ((ShowSymbol) graphicsItem).graphElementType(context);
				
				float scaleX, scaleY;
				if (Math.abs(((ShowSymbol) graphicsItem).scale() - 1.0) > EPSILON)
				{
					scaleX = Math.abs(((ShowSymbol) graphicsItem).scale());
					scaleY = Math.abs(((ShowSymbol) graphicsItem).scale());
				}
				else
				{
					scaleX = Math.abs(((ShowSymbol) graphicsItem).scaleX());
					scaleY = Math.abs(((ShowSymbol) graphicsItem).scaleY());
				}
				
				if (((ShowSymbol) graphicsItem).rotation() < 0 || ((ShowSymbol) graphicsItem).rotation() > 360)
				{
					addError("Rotation for symbol" + ((ShowSymbol) graphicsItem).imageName() + "was equal to " + ((ShowSymbol) graphicsItem).rotation() + ", rotation must be between 0 and 360");
					continue;
				}
				
				if (((ShowSymbol) graphicsItem).sites() != null)
				{
					for (final Integer site : ((ShowSymbol) graphicsItem).sites())
					{	
						if (context.game().board().topology().getGraphElements(graphElementType).size() > site.intValue())
							allSymbols.add(new MetadataImageInfo(site.intValue(), graphElementType, ((ShowSymbol) graphicsItem).imageName(), scaleX, scaleY, fillColour, edgeColour, rotation, offsetX, offsetY));
						else
							addError("Failed to add symbol " + ((ShowSymbol) graphicsItem).imageName() + " at site " + site.intValue() + " with graphElementType " + graphElementType);
					}	
				}
				else if (((ShowSymbol) graphicsItem).region() != null)
				{
					for (final ArrayList<Integer> regionSites : MetadataFunctions.convertRegionToSiteArray(context, ((ShowSymbol) graphicsItem).region(), ((ShowSymbol) graphicsItem).roleType()))
					{
						for (final Integer site : regionSites)
						{
							if (context.game().board().topology().getGraphElements(graphElementType).size() > site.intValue())
								allSymbols.add(new MetadataImageInfo(site.intValue(), ((ShowSymbol) graphicsItem).graphElementType(context), ((ShowSymbol) graphicsItem).imageName(), scaleX, scaleY, fillColour, edgeColour, rotation, offsetX, offsetY));
							else
								addError("Failed to add symbol " + ((ShowSymbol) graphicsItem).imageName() + " at region site " + site.intValue() + " with graphElementType " + graphElementType);	
						}
					}
				}
				else if (((ShowSymbol) graphicsItem).regionFunction() != null)
				{
					((ShowSymbol) graphicsItem).regionFunction().preprocess(context.game());
					for(final int site : ((ShowSymbol) graphicsItem).regionFunction().eval(context).sites())
					{
						if (context.game().board().topology().getGraphElements(graphElementType).size() > site)
							allSymbols.add(new MetadataImageInfo(site, ((ShowSymbol) graphicsItem).graphElementType(context), ((ShowSymbol) graphicsItem).imageName(), scaleX, scaleY, fillColour, edgeColour, rotation, offsetX, offsetY));
						else
							addError("Failed to add symbol " + ((ShowSymbol) graphicsItem).imageName() + " at region site " + site + " with graphElementType " + graphElementType);

					}
				}
			}

		return allSymbols;
	}

	//-------------------------------------------------------------------------
	
	/**
	 * @param context The context.
	 * @return The list of the MetadataImageInfo.
	 */
	public ArrayList<MetadataImageInfo> drawLines(final Context context)
	{
		final ArrayList<MetadataImageInfo> allLines = new ArrayList<>();
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof ShowLine)
			{
				final Colour colourMeta = ((ShowLine) graphicsItem).colour();
				final Color colour = (colourMeta == null) ? null : colourMeta.colour();
				
				if (((ShowLine) graphicsItem).lines() != null)
				{
					for (final Integer[] line : ((ShowLine) graphicsItem).lines())
					{
						if (context.game().board().topology().vertices().size() > Math.max(line[0].intValue(), line[1].intValue()))
							if (((ShowLine) graphicsItem).curve() == null || ((ShowLine) graphicsItem).curve().length == 4)
								allLines.add(new MetadataImageInfo(line, colour, ((ShowLine) graphicsItem).scale(), ((ShowLine) graphicsItem).curve(), ((ShowLine) graphicsItem).siteType(), ((ShowLine) graphicsItem).curveType()));
							else
								addError("Exactly 4 values must be specified for the curve between " + line[0] + " and " + line[1]);
						else
							addError("Failed to draw line between vertices " + line[0] + " and " + line[1]);
					}	
				}
			}

		return allLines;
	}

	//-------------------------------------------------------------------------
	
	/**
	 * @param boardGraphicsType The BoardGraphicsType.
	 * @return The colour of the board.
	 */
	public Color boardColour(final BoardGraphicsType boardGraphicsType)
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof BoardColour)
				if (((BoardColour) graphicsItem).boardGraphicsType() == boardGraphicsType)
				{
					final Colour colourMeta = ((BoardColour) graphicsItem).colour();
					return (colourMeta == null) ? null : colourMeta.colour();
				}

		return null;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return True if the board is hidden.
	 */
	public boolean boardHidden()
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof NoBoard)
				return ((NoBoard) graphicsItem).boardHidden();					

		return false;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return True if the region owned has to be showed.
	 */
	public boolean showRegionOwner()
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof ShowRegionOwner)
				return ((ShowRegionOwner) graphicsItem).show();					

		return false;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return The ContainerStyleType.
	 */
	public ContainerStyleType boardStyle()
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof BoardStyle)
				return ((BoardStyle) graphicsItem).containerStyleType();					

		return null;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return If the container is supposed to only render Edges in a special way (pen and paper style).
	 */
	public boolean replaceComponentsWithFilledCells()
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof BoardStyle)
				return ((BoardStyle) graphicsItem).replaceComponentsWithFilledCells();					

		return false;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return True if the board is checkered.
	 */
	public boolean checkeredBoard()
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof BoardCheckered)
				return ((BoardCheckered) graphicsItem).checkeredBoard();					

		return false;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param context          The context.
	 * @return True if the regions have to be filled.
	 */
	public ArrayList<ArrayList<MetadataImageInfo>> regionsToFill(final Context context)
	{
		final ArrayList<ArrayList<MetadataImageInfo>> allRegions = new ArrayList<>();
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof RegionColour)
			{
				if (((RegionColour) graphicsItem).sites() != null)
				{
					allRegions.add(new ArrayList<>());
					for (final Integer site : ((RegionColour) graphicsItem).sites())
					{
						if (context.game().board().topology().getGraphElements(((RegionColour) graphicsItem).graphElementType(context)).size() > site.intValue())
						{
							final Colour colourolourMeta = ((RegionColour) graphicsItem).colour();
							final Color colour = (colourolourMeta == null) ? null : colourolourMeta.colour();
							final float scale = ((RegionColour) graphicsItem).getScale();
							
							allRegions.get(allRegions.size()-1).add(new MetadataImageInfo(site.intValue(), ((RegionColour) graphicsItem).graphElementType(context), ((RegionColour) graphicsItem).regionSiteType(context), colour, scale));
						}
						else
							addError("Failed to fill site " + site.intValue() + " with graphElementType " + ((RegionColour) graphicsItem).graphElementType(context));
					}
				}
				else if (((RegionColour) graphicsItem).region() != null)
				{
					final Colour colourolourMeta = ((RegionColour) graphicsItem).colour();
					final Color colour = (colourolourMeta == null) ? null : colourolourMeta.colour();
					final float scale = ((RegionColour) graphicsItem).getScale();
					
					for (final ArrayList<Integer> regionSiteList : MetadataFunctions.convertRegionToSiteArray(context, ((RegionColour) graphicsItem).region(), ((RegionColour) graphicsItem).roleType()))
					{
						allRegions.add(new ArrayList<>());
						for (final int site : regionSiteList)
						{
							if (context.game().board().topology().getGraphElements(((RegionColour) graphicsItem).graphElementType(context)).size() > site)
								allRegions.get(allRegions.size()-1).add(new MetadataImageInfo(site, ((RegionColour) graphicsItem).graphElementType(context), ((RegionColour) graphicsItem).regionSiteType(context), colour, scale));
							else
								addError("Failed to fill region " + ((RegionColour) graphicsItem).region() + "at site " + site + " with graphElementType " + ((RegionColour) graphicsItem).graphElementType(context));
						}
					}
				}
				else if (((RegionColour) graphicsItem).regionFunction() != null)
				{
					final Colour colourolourMeta = ((RegionColour) graphicsItem).colour();
					final Color colour = (colourolourMeta == null) ? null : colourolourMeta.colour();
					final float scale = ((RegionColour) graphicsItem).getScale();
					allRegions.add(new ArrayList<>());
					
					((RegionColour) graphicsItem).regionFunction().preprocess(context.game());
					for (final int site : ((RegionColour) graphicsItem).regionFunction().eval(context).sites())
					{
						if (context.game().board().topology().getGraphElements(((RegionColour) graphicsItem).graphElementType(context)).size() > site)
							allRegions.get(allRegions.size()-1).add(new MetadataImageInfo(site, ((RegionColour) graphicsItem).graphElementType(context), ((RegionColour) graphicsItem).regionSiteType(context), colour, scale));
						else
							addError("Failed to fill region " + ((RegionColour) graphicsItem).region() + "at site " + site + " with graphElementType " + ((RegionColour) graphicsItem).graphElementType(context));
					}
				}
			}
		return allRegions;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param boardGraphicsType The BoardGraphicsType.
	 * @return The new thickness.
	 */
	public float boardThickness(final BoardGraphicsType boardGraphicsType)
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof BoardStyleThickness)
				if (((BoardStyleThickness) graphicsItem).boardGraphicsType() == boardGraphicsType)
					if (((BoardStyleThickness) graphicsItem).thickness() >= 0 && ((BoardStyleThickness) graphicsItem).thickness() <= 100)
						return ((BoardStyleThickness) graphicsItem).thickness();
					else addError("Scale for board thickness " + boardGraphicsType.name() + " was equal to " + ((BoardStyleThickness) graphicsItem).thickness() + ", scale must be between 0 and 100");
						

		return (float) 1.0;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return True if the puzzle is adversarial.
	 */
	public boolean adversarialPuzzle()
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof AdversarialPuzzle)
				return ((AdversarialPuzzle) graphicsItem).adversarialPuzzle();
	
		return false;
	}

	//-------------------------------------------------------------------------
	
	/**
	 * @param playerId 
	 * @param context 
	 * @return When to show the score.
	 */
	public ScoreDisplayInfo scoreDisplayInfo(final Context context, final int playerId)
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof ShowScore)
				if (
						((ShowScore) graphicsItem).roleType() == RoleType.All
						||
						MetadataFunctions.getRealOwner(context, ((ShowScore) graphicsItem).roleType()) == playerId
					)
					return new ScoreDisplayInfo( 
								((ShowScore) graphicsItem).showScore(),  
								((ShowScore) graphicsItem).roleType(),
								((ShowScore) graphicsItem).scoreReplacement(),
								((ShowScore) graphicsItem).scoreSuffix()
							);

		return new ScoreDisplayInfo();
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return True if no animation.
	 */
	public boolean noAnimation()
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof NoAnimation)
				return ((NoAnimation) graphicsItem).noAnimation();
				
		return false;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return True if no sunken visuals.
	 */
	public boolean noSunken()
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof NoSunken)
				return ((NoSunken) graphicsItem).noSunken();
				
		return false;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return True if no hand scale.
	 */
	public boolean noHandScale()
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof NoHandScale)
				return ((NoHandScale) graphicsItem).noHandScale();
				
		return false;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return True if pips on the dice should be always drawn as a single number.
	 */
	public boolean noDicePips()
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof NoDicePips)
				return ((NoDicePips) graphicsItem).noDicePips();
				
		return false;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return The list of the suit ranking.
	 */
	public SuitType[] suitRanking()
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof SuitRanking)
				return ((SuitRanking) graphicsItem).suitRanking();

		return null;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param container The container.
	 * @param context   The context.
	 * @param site      The site.
	 * @param siteType  The graph element type.
	 * @param state     The state site.
	 * @return The piece stack scale.
	 */
	public double stackScale(final Container container, final Context context, final int site, final SiteType siteType, final int state)
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof StackType)
				if (((StackType) graphicsItem).roleType() == null || MetadataFunctions.getRealOwner(context, ((StackType) graphicsItem).roleType()) == container.owner())
					if (((StackType) graphicsItem).name() == null || ((StackType) graphicsItem).name().equals(container.name()) || ((StackType) graphicsItem).name().equals(StringRoutines.removeTrailingNumbers(container.name())))
						if (((StackType) graphicsItem).index() == null || ((StackType) graphicsItem).index().equals(Integer.valueOf(container.index())))
							if ( ((StackType) graphicsItem).sites() == null || Arrays.asList(((StackType) graphicsItem).sites()).contains(Integer.valueOf(site)) )
								if (((StackType) graphicsItem).graphElementType() == null || ((StackType) graphicsItem).graphElementType().equals(siteType))
									if (((StackType) graphicsItem).state() == null || ((StackType) graphicsItem).state().equals(Integer.valueOf(state)))
										if (((StackType) graphicsItem).scale() >= 0 && ((StackType) graphicsItem).scale() <= 100)
											return ((StackType) graphicsItem).scale();
										else
											addError("Stack scale for role " + ((StackType) graphicsItem).roleType() + " name " + ((StackType) graphicsItem).name() + " was equal to " + ((StackType) graphicsItem).scale() + ", scale must be between 0 and 100");

		return 1.0;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param container The container.
	 * @param context   The context.
	 * @param site      The site.
	 * @param siteType  The graph element type.
	 * @param state     The state site.
	 * @return The piece stack limit.
	 */
	public double stackLimit(final Container container, final Context context, final int site, final SiteType siteType, final int state)
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof StackType)
				if (((StackType) graphicsItem).roleType() == null || MetadataFunctions.getRealOwner(context, ((StackType) graphicsItem).roleType()) == container.owner())
					if (((StackType) graphicsItem).name() == null || ((StackType) graphicsItem).name().equals(container.name()) || ((StackType) graphicsItem).name().equals(StringRoutines.removeTrailingNumbers(container.name())))
						if (((StackType) graphicsItem).index() == null || ((StackType) graphicsItem).index().equals(Integer.valueOf(container.index())))
							if ( ((StackType) graphicsItem).sites() == null || Arrays.asList(((StackType) graphicsItem).sites()).contains(Integer.valueOf(site)) )
								if (((StackType) graphicsItem).graphElementType() == null || ((StackType) graphicsItem).graphElementType().equals(siteType))
									if (((StackType) graphicsItem).state() == null || ((StackType) graphicsItem).state().equals(Integer.valueOf(state)))
										if (((StackType) graphicsItem).limit() >= 1 && ((StackType) graphicsItem).limit() <= 10)
											return ((StackType) graphicsItem).limit();
										else
											addError("Stack scale for role " + ((StackType) graphicsItem).roleType() + " name " + ((StackType) graphicsItem).name() + " was equal to " + ((StackType) graphicsItem).limit() + ", scale must be between 1 and 10");

		return 5;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param container The container.
	 * @param context   The context.
	 * @param site      The site.
	 * @param siteType  The graph element type.
	 * @param state     The state site.
	 * @return The piece stack type.
	 */
	public PieceStackType stackType(final Container container, final Context context, final int site, final SiteType siteType, final int state)
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof StackType)
				if (((StackType) graphicsItem).roleType() == null || MetadataFunctions.getRealOwner(context, ((StackType) graphicsItem).roleType()) == container.owner())
					if (((StackType) graphicsItem).name() == null || ((StackType) graphicsItem).name().equals(container.name()) || ((StackType) graphicsItem).name().equals(StringRoutines.removeTrailingNumbers(container.name())))
						if (((StackType) graphicsItem).index() == null || ((StackType) graphicsItem).index().equals(Integer.valueOf(container.index())))
							if ( ((StackType) graphicsItem).sites() == null || Arrays.asList(((StackType) graphicsItem).sites()).contains(Integer.valueOf(site)) )
								if (((StackType) graphicsItem).graphElementType() == null || ((StackType) graphicsItem).graphElementType().equals(siteType))
									if (((StackType) graphicsItem).state() == null || ((StackType) graphicsItem).state().equals(Integer.valueOf(state)))
										return ((StackType) graphicsItem).stackType();

		return PieceStackType.Default;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * 
	 * @param playerIndex The index of the player.
	 * @param context     The context.
	 * @return The colour of a given player index.
	 */
	public Color playerColour(final int playerIndex, final Context context)
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof PlayerColour)
				if (MetadataFunctions.getRealOwner(context, ((PlayerColour) graphicsItem).roleType()) == playerIndex)
				{
					final Colour colourMeta = ((PlayerColour) graphicsItem).colour();
					return (colourMeta == null) ? null : colourMeta.colour();
				}

		return null;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * 
	 * @param playerIndex The index of the player.
	 * @param context     The context.
	 * @return The name of a given player index.
	 */
	public String playerName(final int playerIndex, final Context context)
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof PlayerName)
				if (MetadataFunctions.getRealOwner(context, ((PlayerName) graphicsItem).roleType()) == playerIndex)
				{
					return ((PlayerName) graphicsItem).name();
				}

		return null;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return True if the sites has to be drawn like a hole.
	 */
	public int[] sitesAsSpecialHoles()
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof ShowSitesAsHoles)
				return ((ShowSitesAsHoles) graphicsItem).indices();

		return new int[0];
	}
	
	/**
	 * @return The shape of the holes.
	 */
	public HoleType ShapeSpecialHole()
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof ShowSitesAsHoles)
				return ((ShowSitesAsHoles) graphicsItem).type();

		return null;
	}

	//-------------------------------------------------------------------------
	
	/**
	 * @return True If the player holes have to be showed.
	 */
	public boolean showPlayerHoles()
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof ShowPlayerHoles)
				return ((ShowPlayerHoles) graphicsItem).showPlayerHoles();
		
		return false;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return True If the holes with a local state of zero should be marked.
	 */
	public boolean holesUseLocalState()
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof ShowLocalStateHoles)
				return ((ShowLocalStateHoles) graphicsItem).useLocalState();
		
		return false;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param type         The edge type.
	 * @param relationType The relation type.
	 * @param connection   True if this is a connection.
	 * @return The EdgeInfoGUI.
	 */
	public EdgeInfoGUI drawEdge(final EdgeType type, final RelationType relationType, final boolean connection)
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof ShowEdges)
				if (((ShowEdges) graphicsItem).type().supersetOf(type))
					if (((ShowEdges) graphicsItem).relationType().supersetOf(relationType))
						if (((ShowEdges) graphicsItem).connection().booleanValue() == connection)
							return new EdgeInfoGUI(((ShowEdges) graphicsItem).style(), ((ShowEdges) graphicsItem).colour().colour());
		
		return null;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return true to show the pits.
	 */
	public boolean showPits()
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof ShowPits)
				return ((ShowPits) graphicsItem).showPits();

		return false;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return True to show the costs.
	 */
	public boolean showCost()
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof ShowCost)
				return ((ShowCost) graphicsItem).showCost();

		return false;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return True to show the hints.
	 */
	public PuzzleHintLocationType hintLocationType()
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof HintLocation)
				return ((HintLocation) graphicsItem).hintLocation();

		return null;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return True to show the hints.
	 */
	public PuzzleDrawHintType drawHintType()
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof DrawHint)
				return ((DrawHint) graphicsItem).drawHint();

		return null;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return True to show the costs.
	 */
	public boolean showEdgeDirections()
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof ShowEdgeDirections)
				return ((ShowEdgeDirections) graphicsItem).showEdgeDirections();

		return false;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return True to show curved edges.
	 */
	public boolean showCurvedEdges()
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof ShowCurvedEdges)
				return ((ShowCurvedEdges) graphicsItem).showCurvedEdges();

		return true;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return True to show straight edges.
	 */
	public boolean showStraightEdges()
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof ShowStraightEdges)
				return ((ShowStraightEdges) graphicsItem).showStraightEdges();

		return true;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return True to show the costs.
	 */
	public boolean showPossibleMoves()
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof ShowPossibleMoves)
				return ((ShowPossibleMoves) graphicsItem).showPossibleMoves();

		return false;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return True to draw the lines of the ring with straight lines.
	 */
	public boolean straightRingLines()
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof NoCurves)
				return ((NoCurves) graphicsItem).straightRingLines();

		return false;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return The shape of the cell.
	 */
	public ShapeType cellShape()
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof ShowSitesShape)
				return ((ShowSitesShape) graphicsItem).shape();

		return null;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return The shape of the board.
	 */
	public ShapeType boardShape()
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof BoardShape)
				return ((BoardShape) graphicsItem).shape();

		return null;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return The placement of the board.
	 */
	public Rectangle2D boardPlacement()
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof BoardPlacement)
				return (new Rectangle2D.Double(
						((BoardPlacement) graphicsItem).offsetX(),
						((BoardPlacement) graphicsItem).offsetY(),
						((BoardPlacement) graphicsItem).scale(),
						((BoardPlacement) graphicsItem).scale()
						));
		return null;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param playerId 
	 * @param context 
	 * @return The placement of the hand.
	 */
	public Rectangle2D handPlacement(final int playerId, final Context context)
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof HandPlacement)
				if (new Id(null,((HandPlacement) graphicsItem).getPlayer()).eval(context) == playerId)
					return (new Rectangle2D.Double(
							((HandPlacement) graphicsItem).offsetX(),
							((HandPlacement) graphicsItem).offsetY(),
							((HandPlacement) graphicsItem).scale(),
							((HandPlacement) graphicsItem).scale()
							));
		return null;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @param playerId 
	 * @param context 
	 * @return If orientation of a hand is vertical.
	 */
	public boolean handVertical(final int playerId, final Context context)
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof HandPlacement)
				if (new Id(null,((HandPlacement) graphicsItem).getPlayer()).eval(context) == playerId)
					return ((HandPlacement) graphicsItem).isVertical();
		return false;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * @return If the game should auto-pass when this is the only legal move.
	 */
	public boolean autoPassValid() 
	{
		for (final GraphicsItem graphicsItem : items)
			if (graphicsItem instanceof AutoPass)
				return ((AutoPass) graphicsItem).autoPass();
		return true;
	}
	
	//-------------------------------------------------------------------------

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder();
		
		final String open  = (items.size() <= 1) ? "" : "{";
		final String close = (items.size() <= 1) ? "" : "}";
		
		sb.append("    (graphics " + open + "\n");
		for (final GraphicsItem item : items)
			if (item != null)
				sb.append("        " + item.toString());
		sb.append("    " + close + ")\n");

		return sb.toString();
	}

	//-------------------------------------------------------------------------
	
	/**
	 * Add error to errorReport.
	 */
	private void addError(final String string) 
	{
		errorReport += "Error: " + string + "\n";
	}
	
	/**
	 * @return errorReport.
	 */
	public String getErrorReport()
	{
		return errorReport;
	}
	
	/**
	 * Set errorReport.
	 * 
	 * @param s The report.
	 */
	public void setErrorReport(final String s)
	{
		errorReport = s;
	}

	// -------------------------------------------------------------------------

	/**
	 * @param game The game.
	 * @return Accumulated concepts.
	 */
	public BitSet concepts(final Game game)
	{
		final BitSet concepts = new BitSet();
		for (final GraphicsItem item : items)
			if (item != null)
				concepts.or(item.concepts(game));
		return concepts;
	}

	/**
	 * @param game The game.
	 * @return Accumulated concepts.
	 */
	public long gameFlags(final Game game)
	{
		long gameFlags = 0l;
		for (final GraphicsItem item : items)
			if (item != null)
				gameFlags |= item.gameFlags(game);
		return gameFlags;
	}

	/**
	 * Compute if the game needs to be redrawn.
	 * @param game The game.
	 */
	public void computeNeedRedraw(final Game game)
	{
		for (final Regions region : game.equipment().regions())
			if (!region.isStatic())
				needRedraw = true;

		for (final GraphicsItem item : items)
			if (item != null && item.needRedraw())
				needRedraw = true;
	}

	/**
	 * @return True if the graphics need to be redrawn after each move.
	 */
	public boolean needRedrawn()
	{
		return needRedraw;
	}

}