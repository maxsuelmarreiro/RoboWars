package robowars.server.view;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.UIManager;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import lejos.robotics.Pose;

import net.sf.fmj.ui.application.PlayerPanel;

import org.apache.log4j.Logger;

import robowars.server.controller.*;
import robowars.shared.model.GameEvent;
import robowars.shared.model.GameListener;
import robowars.shared.model.GameModel;
import robowars.shared.model.GameType;
import robowars.shared.model.RobotCommand;

/**
 * Provides a GUI for the server administrator to modify configuration options
 * and monitor the current server/game state.
 */
public class AdminView extends JFrame implements GameListener, ServerLobbyListener {
	/** The logger used by this class */
	private static Logger log = Logger.getLogger(AdminView.class);
	
	/** List models for the list of connected robots and users */
	private DefaultListModel robotListModel, userListModel;
	
	/** Area for displaying chat text */
	private JTextArea mainChatArea;
	
	/** JLabel to show the currently selected game type */
	private JLabel curGameType;
	
	/** The MediaStreamer managing camera selection and settings for the AdminView */
	private MediaStreamer mediaSource;
	
	/** Reference to the ServerLobby this AdminView is administrating */
	private ServerLobby lobby;
	
	/** 
	 * Generates a new AdminView frame
	 * @param frameTitle	The title of the frame
	 * @param lobby			The ServerLobby that this view should listen for events from
	 * @param mediaSource	The MediaStreamer managing camera selection and settings for the AdminView
	 **/
	public AdminView(String windowTitle, ServerLobby lobby, MediaStreamer mediaSource) {
		super(windowTitle);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        this.lobby = lobby;
        this.mediaSource = mediaSource;
        
		// Set look and feel
		try {
		    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {
		   try {
		        UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
		    }
		    catch (Exception e2) {
		        System.err.println("Unable to load default look and feel.");
		        System.exit(1); // Might not want to exit
		    }
		}

		// Setup Menus
		JMenuBar menuBar = new JMenuBar();
        this.setJMenuBar(menuBar);
        initFileMenu();
        
        // Setup user and player lists
        robotListModel = new DefaultListModel();
        userListModel = new DefaultListModel();
        JList robotList = new JList(robotListModel);
        JList userList = new JList(userListModel);
        robotList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        new PositionResetListener(robotList);
        
        // Setup the connected robots and user lists
        JPanel connectedListsPanel = new JPanel();
        connectedListsPanel.setLayout(new BorderLayout());
        this.getContentPane().add(connectedListsPanel, BorderLayout.EAST);
        
        JPanel userListPanel = new JPanel();
        userListPanel.setLayout(new BorderLayout());
        JLabel userLabel = new JLabel("Users");
        userLabel.setPreferredSize(new Dimension(100, 15));
        userListPanel.add(userLabel, BorderLayout.NORTH);
        userListPanel.add(new JScrollPane(userList), BorderLayout.SOUTH);
        connectedListsPanel.add(userListPanel, BorderLayout.NORTH);
        
        JPanel robotListPanel = new JPanel();
        robotListPanel.setLayout(new BorderLayout());
        JLabel robotLabel = new JLabel("Robots");
        robotLabel.setPreferredSize(new Dimension(100, 15));
        robotListPanel.add(robotLabel, BorderLayout.NORTH);
        robotListPanel.add(new JScrollPane(robotList), BorderLayout.SOUTH);
        connectedListsPanel.add(robotListPanel, BorderLayout.SOUTH);
        
        // Setup the main chat area
        mainChatArea = new JTextArea();
        mainChatArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        mainChatArea.setEditable(false);
        mainChatArea.setLineWrap(true);
        mainChatArea.setWrapStyleWord(true);
        JScrollPane chatScrollPane = new JScrollPane(mainChatArea);
        chatScrollPane.setPreferredSize(new Dimension(500,200));
        this.getContentPane().add(chatScrollPane, BorderLayout.WEST);
        
        // Add a label showing the currently selected game type
        curGameType = new JLabel();
        setGameTypeLabel(lobby.getCurrentGameType());
		this.getContentPane().add(curGameType, BorderLayout.SOUTH);

        // Set view to listen on the provided ServerLobby
        lobby.addLobbyStateListener(this);
        
        this.pack();
        this.setResizable(false);
		this.setVisible(true);
	}
	

	/**
	 * Initializes the "File" menu and adds it to the main menu bar.
	 */
	private void initFileMenu() {
		// Add the "File" menu
		JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        this.getJMenuBar().add(fileMenu);
        
		// Quit Menu Option
        JMenuItem quit = new JMenuItem("Quit", KeyEvent.VK_Q);
        quit.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                        AdminView.this.dispose();
                        System.exit(0);
                }
        });
        fileMenu.add(quit);

	}

	@Override
	/** @see ServerLobbyListener#userStateChanged(LobbyUserEvent) */
	public void userStateChanged(LobbyUserEvent event) {
		if(event.getEventType() == ServerLobbyEvent.EVENT_PLAYER_JOINED) {
			userListModel.addElement(event.getUser().getUsername());
			addLineToMainChat(event.getUser().getUsername() + " has joined the server.");
		} else if (event.getEventType() == ServerLobbyEvent.EVENT_PLAYER_LEFT) {
			userListModel.removeElement(event.getUser().getUsername());
			addLineToMainChat(event.getUser().getUsername() + " has left the server.");
		} else if (event.getEventType() == ServerLobbyEvent.EVENT_PLAYER_STATE_CHANGE) {
			addLineToMainChat("< " + event.getUser().getUsername() + " Ready = " +
					event.getUser().isReady() +", Spectator = " + event.getUser().isPureSpectator() + " >");
		}
	}

	@Override
	/** @see ServerLobbyListener#robotStateChanged(LobbyRobotEvent) */
	public void robotStateChanged(LobbyRobotEvent event) {
		if(event.getEventType() == ServerLobbyEvent.EVENT_ROBOT_REGISTERED) {
			robotListModel.addElement(event.getRobot().getIdentifier());
			addLineToMainChat("Robot " + event.getRobot().getIdentifier() + " has registered with the server.");
		} else if (event.getEventType() == ServerLobbyEvent.EVENT_ROBOT_UNREGISTERED) {
			robotListModel.removeElement(event.getRobot().getIdentifier());
			addLineToMainChat("Robot " + event.getRobot().getIdentifier() + " has unregistered with the server.");
		}
	}

	@Override
	/** @see ServerLobbyListener#lobbyGameStateChanged(LobbyGameEvent) */
	public void lobbyGameStateChanged(LobbyGameEvent event) {
		if(event.getEventType() == ServerLobbyEvent.EVENT_GAMETYPE_CHANGE) {
			addLineToMainChat("Game type changed to: " + event.getGameType().toString());
			setGameTypeLabel(event.getGameType());
		} else if (event.getEventType() == ServerLobbyEvent.EVENT_GAME_LAUNCH) {
			// addLineToMainChat("--- New game has been launched ---");
			ServerLobby source = (ServerLobby) event.getSource();
			Admin2DGameView view = new Admin2DGameView((int) GameModel.DEFAULT_ARENA_SIZE, source.getCurrentGame().getGameModel());
			source.getCurrentGame().getGameModel().addListener(view);
		} else if (event.getEventType() == ServerLobbyEvent.EVENT_GAME_OVER) {
			// addLineToMainChat("--- Game in progress terminated ---");
		}
		
	}
	
	/** 
	 * Adds a time stamped line of text to the main chat area (followed by
	 * a newline)
	 * @param text	The text to add to the chat pane
	 */
	private void addLineToMainChat(String text) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("[h:mm:ss a] ");
		mainChatArea.setText(mainChatArea.getText() + dateFormat.format(new Date())
				+ text + "\n");
	}
	
	/**
	 * Sets the game type label to show the specified game type
	 * @param gameType	The game type to display
	 */
	private void setGameTypeLabel(GameType gameType) {
		curGameType.setText("Selected Game Type: " + gameType.toString() + " (Minimum players = "
				+ gameType.getMinimumPlayers() + ")");
		this.pack();
	}


	@Override
	/** @see ServerLobbyListener#lobbyChatMessage(LobbyChatEvent) */
	public void lobbyChatMessage(LobbyChatEvent event) {
		 addLineToMainChat(event.getMessage());
	}
	
	public void gameStateChanged(GameEvent event){
		
	}
	
	/**
	 * Control class which manages issuing position reset commands to registered
	 * robots based on admin input.
	 */
	private class PositionResetListener extends MouseAdapter {
		/** 
		 * The JList that this list should use to determine clicked indexes.
		 */
		private JList robotList;
		
		/**
		 * Generates a new PositionResetListener watching the passed JList
		 * @param robotList	The list of connected robot identifiers
		 */
		public PositionResetListener(JList robotList) {
			this.robotList = robotList;
			robotList.addMouseListener(this);
		}
		
		/**
		 * If a robot in the list of connected robots is double clicked and no
		 * game is currently in progress that admin is prompted to enter a new
		 * position and heading for the robot.
		 */
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				if(lobby.gameInProgress()) {
					// Ignore attempts to reset position when game is in progress
					log.info("Attempted position reset during active game.");
					return;
				}
				
				int index = robotList.locationToIndex(e.getPoint());
				String robotId = (String)robotList.getModel().getElementAt(index);
				try {
					float xPos = Float.parseFloat(JOptionPane.showInputDialog(AdminView.this, 
							"Please enter new X position coordinate.",
							robotId + " - Position Reset", JOptionPane.DEFAULT_OPTION));
					float yPos = Float.parseFloat(JOptionPane.showInputDialog(AdminView.this, 
							"Please enter new Y position coordinate.",
							robotId + " - Position Reset", JOptionPane.DEFAULT_OPTION));
					float heading = Float.parseFloat(JOptionPane.showInputDialog(AdminView.this, 
							"Please enter new heading.",
							robotId + " - Position Reset", JOptionPane.DEFAULT_OPTION));
					
					Pose newPos = new Pose(xPos, yPos, heading);
					
					RobotProxy robot = lobby.getRobotProxy(robotId);
					if(robot != null) {
						robot.sendCommand(new RobotCommand(newPos, 10));
					}
					  
				} catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(AdminView.this, 
							"Invalid input, please try again.",
					robotId + " - Position Reset (FAILED)", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
	}
}
