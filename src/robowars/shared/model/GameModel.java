package robowars.shared.model;

import java.io.Serializable;
import java.util.Vector;
import java.util.ArrayList;

public abstract class GameModel implements Serializable{
	
	protected GameType gameType;
	protected ControlType controlType;
	protected int minimumPlayers;
	protected Vector<Float> arenaSize;
	protected boolean inProgress;
	protected ArrayList<GameEntity> entities;
	protected ArrayList<GameRobot> robots;
	protected GameListener listener;
	
	public static final float DEFAULT_ARENA_SIZE = 100;
	
	public static GameModel generateGameModel(GameType gameType){
		if(gameType == GameType.LIGHTCYCLES)
			return new LightCycles();
		if(gameType == GameType.TANK_SIMULATION)
			return new TankSimulation();
		return null;
	}
	
	public void initVariables() {
		arenaSize = new Vector<Float>(2);
		inProgress = false;
		entities = new ArrayList<GameEntity>();
		minimumPlayers = gameType.getMinimumPlayers();
		
		arenaSize.add(DEFAULT_ARENA_SIZE);//x
		arenaSize.add(DEFAULT_ARENA_SIZE);//y
	}
	
	public void addListener(GameListener listener){
		this.listener = listener;
	}
	
	public void startGame() {
		if(robots.get(0) != null && robots.get(1) != null)
			inProgress = true;
	}
	
	public ArrayList<GameEntity> getEntities() {
		return entities;
	}
	
	public abstract void updateGameState(int timeElapsed);
	
	public boolean updateRobotPosition(String identifier, Vector<Float> pos, Vector<Float> heading) {
		GameRobot robot = null;
		
		for(GameRobot r : robots){
			if(r.getRobotId() == identifier)
				robot = r;
		}
		
		if(robot == null)
			return false;//error, robot with specified identifier doesn't exist.
		
		robot.setPosition(pos);
		robot.setHeading(heading);
		return true;
	}
	
	public RobotCommand getCurrentRobotCommand(String identifier) {
		return null;
	}
	
	public GameRobot getGameRobot(String identifier) {
		
		for (GameRobot robot : robots){
			if (robot.getRobotId() == identifier)
				return robot;
		}
		return null;
	}
	
	public void processCommand(RobotCommand command) {
		if(isValidCommand(command))
			return;
	}
	
	public boolean isValidCommand(RobotCommand command){
		if (gameType == GameType.LIGHTCYCLES){
			if (command.getType() == CommandType.MOVE_CONTINUOUS || 
					command.getType() == CommandType.TURN_RIGHT_ANGLE_LEFT || 
					command.getType() == CommandType.TURN_RIGHT_ANGLE_RIGHT) {
				return true;
			}else{
				return false;
			}
		}else if (gameType == GameType.TANK_SIMULATION){
			return true; //Any command restrictions in TankSimulation?
		}else{
			return true;
		}
	}
	
	public ControlType getControlType(){
		return controlType;
	}
	
	public abstract boolean checkGameOver();

	
	public void addRobot(String identifier) {
		//In the constructor of GameRobot, two parameters id and robotid, what's the difference?
		//GameRobot newRobot = new GameRobot(new Vector<Float>(),new Vector<Float>(),1,1,0,1,identifier);
		GameRobot newRobot = new GameRobot(1,1,identifier);
		entities.add(newRobot);
		robots.add(newRobot);
	}
	
	private byte[] serializeState() {
		return null;
	}
	
	private byte[] marshallState() {
		return null;
	}
	
}
