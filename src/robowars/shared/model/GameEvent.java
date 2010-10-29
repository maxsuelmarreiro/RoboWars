package robowars.shared.model;

import java.util.EventObject;

public class GameEvent extends EventObject{

	public static final int GAME_START = 0;
	public static final int GAME_OVER = 1;
	public static final int COLLISION_DETECTED = 2;
	public static final int PROJECTILE_FIRED = 3;
	public static final int PROJECTILE_HIT = 4;

	
	private int type;
	
	public GameEvent(GameModel model, int type){
		super(model);
	}
}
