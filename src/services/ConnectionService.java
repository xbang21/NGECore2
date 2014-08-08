/*******************************************************************************
 * Copyright (c) 2013 <Project SWG>
 * 
 * This File is part of NGECore2.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Using NGEngine to work with NGECore2 is making a combined work based on NGEngine. 
 * Therefore all terms and conditions of the GNU Lesser General Public License cover the combination.
 ******************************************************************************/
package services;

import java.nio.ByteOrder;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import main.NGECore;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;

import protocol.swg.AccountFeatureBits;
import protocol.swg.ClientIdMsg;
import protocol.swg.ClientPermissionsMessage;
import protocol.swg.ConnectionServerLagResponse;
import protocol.swg.GalaxyLoopTimesResponse;
import protocol.swg.GameServerLagResponse;
import protocol.swg.HeartBeatMessage;
import engine.clients.Client;
import engine.resources.database.DatabaseConnection;
import engine.resources.objects.SWGObject;
import engine.resources.scene.Point3D;
import engine.resources.service.INetworkDispatch;
import engine.resources.service.INetworkRemoteEvent;
import resources.common.*;
import resources.common.collidables.AbstractCollidable;
import resources.datatables.PlayerFlags;
import resources.objects.creature.CreatureObject;
import resources.objects.player.PlayerObject;
import services.chat.ChatRoom;

@SuppressWarnings("unused")

public class ConnectionService implements INetworkDispatch {

	private NGECore core;
	private DatabaseConnection databaseConnection;
	private DatabaseConnection databaseConnection2;
	private int maxNumberOfCharacters;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	public ConnectionService(final NGECore core) {

		this.core = core;
		this.databaseConnection = core.getDatabase1();
		this.databaseConnection2 = core.getDatabase2();
		this.maxNumberOfCharacters = core.getConfig().getInt("MAXNUMBEROFCHARACTERS");
		
		scheduler.scheduleAtFixedRate(new Runnable() {
			
			public void run() {
				try {
					synchronized(core.getActiveConnectionsMap()) {
						for(Client c : core.getActiveConnectionsMap().values()) {
							if(c.getParent() != null) {
								if ((System.currentTimeMillis() - c.getSession().getLastReadTime()) > 300000) {
									try {
										disconnect(c);
									} catch (Exception e) {
										System.err.println("ConnectionService:disconnect(): Error disconnecting client.");
										e.printStackTrace();
									}
								}
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			
		}, 10, 10, TimeUnit.MINUTES);
	
	}
	
	@Override
	public void insertOpcodes(Map<Integer,INetworkRemoteEvent> swgOpcodes, Map<Integer,INetworkRemoteEvent> objControllerOpcodes) {

		swgOpcodes.put(Opcodes.ClientIdMsg, new INetworkRemoteEvent() {

			@Override
			public void handlePacket(IoSession session, IoBuffer data) throws Exception {
				
				data = data.order(ByteOrder.LITTLE_ENDIAN);
				ClientIdMsg clientIdMsg = new ClientIdMsg();
				data.position(0);
				clientIdMsg.deserialize(data);
				
				Client client = core.getClient(session);
				if(client == null) {
					System.out.println("NULL Client");
					return;
				}
				client.setSession(session);
                ResultSet resultSet;
	            PreparedStatement preparedStatement;

	            try {
	            	
		            preparedStatement = databaseConnection.preparedStatement("SELECT * FROM sessions WHERE key=?");
		            preparedStatement.setBytes(1, clientIdMsg.getSessionKey());
		            resultSet = preparedStatement.executeQuery();
		            
		            if (resultSet.next()) {
		            	client.setAccountId(resultSet.getLong("accountId"));
		            	client.setSessionKey(clientIdMsg.getSessionKey());
		            	client.setGM(core.loginService.checkForGmPermission((int) resultSet.getLong("accountId")));
		            	AccountFeatureBits accountFeatureBits = new AccountFeatureBits();
		            	ClientPermissionsMessage clientPermissionsMessage = new ClientPermissionsMessage(maxNumberOfCharacters - core.characterService.getNumberOfCharacters((int) resultSet.getLong("accountId")));
		            	session.write(new HeartBeatMessage().serialize());
		            	session.write(accountFeatureBits.serialize());
		            	session.write(clientPermissionsMessage.serialize());
		                preparedStatement.close();
		                
		            } else {
		            	System.out.println("Cant get login session");
		            }
	                
	            } catch (SQLException e) {
	
	                e.printStackTrace();
	
	            }
	            
			}
			
			
			
		});
		
		swgOpcodes.put(Opcodes.RequestGalaxyLoopTimes, new INetworkRemoteEvent() {

			@Override
			public void handlePacket(IoSession session, IoBuffer data) throws Exception {

				/* 10 000 000 = extremely heavy
				 * 5 000 000 = very heavy
				 * 4 000 000 = heavy
				 * 2 500 000 = medium
				 * 2 000 000 = light
				 * < 2 000 000 = very light
				*/
				Client client = core.getClient(session);
				if(client == null) {
					System.out.println("NULL Client");
					return;
				}
				SWGObject object = client.getParent();
				if(object == null)
					return;
				int observers = object.getObservers().size();
				int areaActivity = 0;
				if(observers > 32)
					areaActivity = 2000000;
				if(observers > 40)
					areaActivity = 2500000;
				if(observers > 50)
					areaActivity = 4000000;
				if(observers > 75)
					areaActivity = 5000000;
				if(observers > 100)
					areaActivity = 10000000;
				
				GalaxyLoopTimesResponse response = new GalaxyLoopTimesResponse(areaActivity);
				session.write(response.serialize());
				
			}
			
		});

		swgOpcodes.put(Opcodes.LagRequest, new INetworkRemoteEvent() {

			@Override
			public void handlePacket(IoSession session, IoBuffer data) throws Exception {

				ConnectionServerLagResponse connectionReponse = new ConnectionServerLagResponse();
				session.write(connectionReponse.serialize());
				GameServerLagResponse gameReponse = new GameServerLagResponse();
				session.write(gameReponse.serialize());

			}
			
		});
		
		swgOpcodes.put(Opcodes.ConnectPlayerMessage, new INetworkRemoteEvent() {

			@Override
			public void handlePacket(IoSession session, IoBuffer data) throws Exception {
				//ConnectPlayerResponseMessage
			}
			
		});
		
	}
	
	public void disconnect(Client client) {
		//Client client = core.getClient(session);
		IoSession session = client.getSession();
		if(session == null || client.getParent() == null)
			return;
		
		CreatureObject object = (CreatureObject) client.getParent();
		
		try {
			for (CreatureObject opponent : new ArrayList<CreatureObject>(object.getDuelList())) {
				if (opponent != null) {
					core.combatService.handleEndDuel(object, opponent, true);
				}
			}
			core.combatService.endCombat((CreatureObject) object);
			object.setInviteCounter(0);
			object.setInviteSenderId(0);
			object.setInviteSenderName("");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			if(object.getAttachment("inspireDuration") != null)
				object.setAttachment("inspireDuration", null);
			
			if(object.getPerformanceListenee() != null) {
				object.getPerformanceListenee().removeSpectator(object);
				object.setPerformanceListenee(null);
			}
			
			if(object.getPerformanceWatchee() != null) {
				object.getPerformanceWatchee().removeSpectator(object);
				object.setPerformanceWatchee(null);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			core.groupService.handleGroupDisband(object, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			if (core.instanceService.isInInstance(object)) {
				core.instanceService.remove(core.instanceService.getActiveInstance(object), object);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		object.setClient(null);
		
		PlayerObject ghost = (PlayerObject) object.getSlottedObject("ghost");
		
		if(ghost == null)
			return;
		
		Point3D objectPos = object.getWorldPosition();
		
		try {
			List<AbstractCollidable> collidables = core.simulationService.getCollidables(object.getPlanet(), objectPos.x, objectPos.z, 512);
	
			collidables.forEach(c -> c.removeCollidedObject(object));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			if (ghost != null) {
				String objectShortName = object.getCustomName();
				
				if (object.getCustomName().contains(" ")) {
					String[] splitName = object.getCustomName().toLowerCase().split(" ");
					objectShortName = splitName[0];
				}
				
				core.chatService.playerStatusChange(objectShortName, (byte) 0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
				
		long parentId = object.getParentId();
		
		/*if(object.getContainer() == null) {
			boolean remove = core.simulationService.remove(object, object.getPosition().x, object.getPosition().z);
			if(remove)
				System.out.println("Successful quadtree remove");
		} else {
			object.getContainer()._remove(object);
			object.setParentId(parentId);
		}*/

		
		/*HashSet<Client> oldObservers = new HashSet<Client>(object.getObservers());
		for(Iterator<Client> it = oldObservers.iterator(); it.hasNext();) {
			Client observerClient = it.next();
			if(observerClient.getParent() != null && !(observerClient.getSession() == session)) {
				observerClient.getParent().makeUnaware(object);
			}
		}*/
		
		try {
			if (core.getBountiesODB().contains(object.getObjectID())) {
				core.missionService.getBountyMap().remove(core.getBountiesODB().get(object.getObjectID()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		ghost.toggleFlag(PlayerFlags.LD);
		
		object.setPerformanceListenee(null);
		object.setPerformanceWatchee(null);
		object.setAttachment("disconnectTask", null);
		
		try {
			List<ScheduledFuture<?>> schedulers = core.playerService.getSchedulers().get(object.getObjectID());
			if(schedulers != null) {
				schedulers.forEach(s -> s.cancel(true));
				schedulers.clear();
			}
			core.playerService.getSchedulers().remove(object.getObjectID());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			core.getSWGObjectODB().put(object.getObjectID(), object);
			core.objectService.destroyObject(object);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
		
	}

}
