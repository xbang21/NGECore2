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
package services.object;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import resources.common.*;
import resources.datatables.Options;
import resources.datatables.PlayerFlags;
import resources.harvest.SurveyTool;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.python.core.Py;
import org.python.core.PyObject;

import com.sleepycat.je.Transaction;
import com.sleepycat.je.TransactionConfig;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

import protocol.swg.CmdSceneReady;
import protocol.swg.CmdStartScene;
import protocol.swg.HeartBeatMessage;
import protocol.swg.ObjControllerMessage;
import protocol.swg.ParametersMessage;
import protocol.swg.SelectCharacter;
import protocol.swg.ServerTimeMessage;
import protocol.swg.UnkByteFlag;
import protocol.swg.chat.ChatFriendsListUpdate;
import protocol.swg.chat.ChatOnChangeFriendStatus;
import protocol.swg.chat.ChatOnConnectAvatar;
import protocol.swg.chat.ChatOnGetFriendsList;
import protocol.swg.chat.ChatRoomList;
import protocol.swg.chat.ChatServerStatus;
import protocol.swg.objectControllerObjects.ShowFlyText;
import protocol.swg.chat.VoiceChatStatus;
import protocol.swg.objectControllerObjects.UiPlayEffect;
import engine.clientdata.ClientFileManager;
import engine.clientdata.visitors.CrcStringTableVisitor;
import engine.clientdata.visitors.DatatableVisitor;
import engine.clientdata.visitors.WorldSnapshotVisitor;
import engine.clientdata.visitors.WorldSnapshotVisitor.SnapshotChunk;
import engine.clients.Client;
import engine.resources.common.CRC;
import engine.resources.container.Traverser;
import engine.resources.container.WorldCellPermissions;
import engine.resources.container.WorldPermissions;
import engine.resources.database.DatabaseConnection;
import engine.resources.database.ODBCursor;
import engine.resources.database.ObjectDatabase;
import engine.resources.objects.Delta;
import engine.resources.objects.IPersistent;
import engine.resources.objects.SWGObject;
import engine.resources.scene.Planet;
import engine.resources.scene.Point3D;
import engine.resources.scene.Quaternion;
import engine.resources.service.INetworkDispatch;
import engine.resources.service.INetworkRemoteEvent;
import main.NGECore;
import resources.objectives.BountyMissionObjective;
import resources.objects.building.BuildingObject;
import resources.objects.cell.CellObject;
import resources.objects.craft.DraftSchematic;
import resources.objects.creature.CreatureObject;
import resources.objects.factorycrate.FactoryCrateObject;
import resources.objects.group.GroupObject;
import resources.objects.guild.GuildObject;
import resources.objects.harvester.HarvesterObject;
import resources.objects.installation.InstallationObject;
import resources.objects.intangible.IntangibleObject;
import resources.objects.manufacture.ManufactureSchematicObject;
import resources.objects.mission.MissionObject;
import resources.objects.player.PlayerObject;
import resources.objects.resource.GalacticResource;
import resources.objects.resource.ResourceContainerObject;
import resources.objects.resource.ResourceRoot;
import resources.objects.staticobject.StaticObject;
import resources.objects.tangible.TangibleObject;
import resources.objects.waypoint.WaypointObject;
import resources.objects.weapon.WeaponObject;
import services.ai.AIActor;
import services.command.BaseSWGCommand;
import services.command.CombatCommand;
import services.bazaar.AuctionItem;
import services.chat.ChatRoom;
import services.equipment.EquipmentService;

@SuppressWarnings("unused")
public class ObjectService implements INetworkDispatch {

	private Map<Long, SWGObject> objectList = new ConcurrentHashMap<Long, SWGObject>();
	private NGECore core;
	private DatabaseConnection databaseConnection;
	private AtomicLong highestId = new AtomicLong();
	private Random random = new Random();
	private Map<String, PyObject> serverTemplates = new ConcurrentHashMap<String, PyObject>();
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	protected final Object objectMutex = new Object();
	private List<Runnable> loadServerTemplateTasks = Collections.synchronizedList(new ArrayList<Runnable>());
	
	public ObjectService(final NGECore core) {
		this.core = core;
		databaseConnection = core.getDatabase1();
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
		    public void run() {
		    	synchronized(objectList) {
		    		for(SWGObject obj : objectList.values()) {
		    			
		    			if(obj.getClient() != null && obj.getClient().getSession() != null) {
		    				core.connectionService.disconnect(obj.getClient());
		    			}
		    			
		    		}
		    	}
		    	core.bazaarService.saveAllItems();
		    	core.housingService.saveBuildings();
		    	core.harvesterService.saveHarvesters();
		    	core.closeODBs();
		    }
		});
		
		long highestId;

		try {
			PreparedStatement ps = databaseConnection.preparedStatement("SELECT id FROM highestid WHERE id=(SELECT max(id) FROM highestid)");
			ResultSet result = ps.executeQuery();
			result.next();
			highestId = result.getInt("id");
			this.highestId.set(highestId);
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void loadBuildings() {
		ODBCursor cursor = core.getSWGObjectODB().getCursor();
				
		while(cursor.hasNext()) {
			final SWGObject building = (SWGObject) cursor.next();
			if(!(building instanceof BuildingObject) || building == null)
				continue;
			objectList.put(building.getObjectID(), building);
			Planet planet = core.terrainService.getPlanetByID(building.getPlanetId());
			building.setPlanet(planet);
			building.viewChildren(building, true, true, (object) -> {
				objectList.put(object.getObjectID(), object);
				if(object.getParentId() != 0 && object.getContainer() == null)
					object.setParent(building);
				object.getContainerInfo(object.getTemplate());
			});
		}
		
		cursor.close();
	}
	
	public void loadServerTemplates() {
		System.out.println("Loading server templates...");
		loadServerTemplateTasks.forEach(Runnable::run);
		loadServerTemplateTasks.clear();
		System.out.println("Finished loading server templates...");
	}
	
	public SWGObject createObject(String Template, long objectID, Planet planet, Point3D position, Quaternion orientation, String customServerTemplate) {
		return createObject(Template, objectID, planet, position, orientation, customServerTemplate, false, true);
	}
	
	public SWGObject createObject(String Template, long objectID, Planet planet, Point3D position, Quaternion orientation, String customServerTemplate, boolean overrideSnapshot, boolean loadServerTemplate) {
		SWGObject object = null;
		CrcStringTableVisitor crcTable;
		try {
			crcTable = ClientFileManager.loadFile("misc/object_template_crc_string_table.iff", CrcStringTableVisitor.class);
			if(!crcTable.isValidCRC(CRC.StringtoCRC(Template))) {
				System.out.println("Invalid CRC for template:" + Template);
				return null;
			}
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		
		boolean isSnapshot = false;
		
		if(objectID == 0)
			objectID = generateObjectID();
		else
			isSnapshot = !overrideSnapshot;
		
		if(Template.startsWith("object/creature")) {
			
			object = new CreatureObject(objectID, planet, position, orientation, Template);

		} else if(Template.startsWith("object/player")) {
			
			object = new PlayerObject(objectID, planet);
			
		} else if(Template.startsWith("object/tangible/survey_tool")) {
			
			object = new SurveyTool(objectID, planet, position, orientation, Template);
			
		} else if(Template.startsWith("object/tangible")) {
			
			object = new TangibleObject(objectID, planet, position, orientation, Template);

		} else if(Template.startsWith("object/intangible")) {
			
			object = new IntangibleObject(objectID, planet, position, orientation,Template);

		} else if(Template.startsWith("object/weapon")) {
			
			object = new WeaponObject(objectID, planet, position, orientation, Template);

		} else if(Template.startsWith("object/building") || Template.startsWith("object/static/worldbuilding/structures") || Template.startsWith("object/static/structure")){
			
			object = new BuildingObject(objectID, planet, position, orientation, Template);
			if(!isSnapshot && !overrideSnapshot && object.getPortalVisitor() != null) {
				int cellCount = object.getPortalVisitor().cells.size() - 1; // -1 for index 0 cell which is outside the building and used for ai pathfinding
				for (int i = 0; i < cellCount; i++) {
					CellObject cell = (CellObject) createObject("object/cell/shared_cell.iff", planet);
					cell.setCellNumber(i+1);
					object.add(cell);
				}
			}
			
		} else if(Template.startsWith("object/cell")) {
			
			object = new CellObject(objectID, planet);
			
		} else if(Template.startsWith("object/static")) {
			
			object = new StaticObject(objectID, planet, position, orientation, Template);
			
		} else if(Template.startsWith("object/guild")) {
			
			object = new GuildObject(core, objectID, planet, position, orientation, Template);
			
		} else if(Template.startsWith("object/group")) {
			
			object = new GroupObject(objectID);
			
		} else if(Template.startsWith("object/mobile")){
			
			object = new CreatureObject(objectID, planet, position, orientation, Template);
			
		} else if(Template.startsWith("object/waypoint")) {
			
			object = new WaypointObject(objectID, planet, position);
			
		}  else if(Template.startsWith("object/mission")) {
			
			object = new MissionObject(objectID, planet, Template);
			
		} else if(Template.startsWith("object/resource_container")) {
			
			object = new ResourceContainerObject(objectID, planet, position, orientation, Template);
			
		} else if(Template.startsWith("object/factory/shared_factory_crate")) {
			
			object = new FactoryCrateObject(objectID, planet, position, orientation, Template);
			
		} else if(Template.startsWith("object/draft_schematic")) {
			
			object = new DraftSchematic(objectID, planet, Template, position, orientation);
			
		} else if(Template.startsWith("object/manufacture_schematic")) {
			
			object = new ManufactureSchematicObject(objectID, planet, Template, position, orientation);
			
		} else if(Template.startsWith("object/installation/mining_ore/construction")) {
			
			float positionY = core.terrainService.getHeight(planet.getID(), position.x, position.z)-1f;
			Point3D newpoint = new Point3D(position.x,positionY,position.z);				
			object = new InstallationObject(objectID, planet, newpoint, orientation, Template);
			
		} else if(Template.startsWith("object/installation/mining_ore") || Template.startsWith("object/installation/mining_liquid") ||
				  Template.startsWith("object/installation/mining_gas") || Template.startsWith("object/installation/mining_organic") || 
				  Template.startsWith("object/installation/generators")) {
			
			float positionY = core.terrainService.getHeight(planet.getID(), position.x, position.z)-1f;
			Point3D newpoint = new Point3D(position.x,positionY,position.z);			
			object = new HarvesterObject(objectID, planet, newpoint, orientation, Template);	
			
		} else {
			return null;			
		}
		
		object.setPlanetId(planet.getID());
		
		object.setAttachment("customServerTemplate", customServerTemplate);
		
		object.setisInSnapshot(isSnapshot);
		synchronized(objectMutex) {
			if(!core.getObjectIdODB().contains(objectID))
				core.getObjectIdODB().put(objectID, new ObjectId(objectID));
		}
		// Set Options - easier to set them across the board here
		// because we'll be spawning them despite most of them being unscripted.
		// Any such settings can be completely reset with setOptionsBitmask
		// in scripts and modified with setOptions(Options.X, true/false)
		if (Template.startsWith("object/creature/") || Template.startsWith("object/mobile/")) {
			if (Template.startsWith("object/mobile/beast_master/")) {
				((CreatureObject) object).setOptionsBitmask(Options.NONE);
			} else if (Template.startsWith("object/mobile/vendor/")) {
				((CreatureObject) object).setOptionsBitmask(Options.INVULNERABLE | Options.USABLE);
			} else if (Template.startsWith("object/mobile/vehicle/")) {
				((CreatureObject) object).setOptionsBitmask(Options.ATTACKABLE | Options.MOUNT);
			} else if (Template.startsWith("object/mobile/hologram/")) {
				((CreatureObject) object).setOptionsBitmask(Options.INVULNERABLE);
			} else if (Template.startsWith("object/mobile/")) {
				((CreatureObject) object).setOptionsBitmask(Options.ATTACKABLE);
			} else if (Template.startsWith("object/creature/npc/theme_park/")) {
				((CreatureObject) object).setOptionsBitmask(Options.INVULNERABLE);
			} else if (Template.startsWith("object/creature/npc/general/")) {
				((CreatureObject) object).setOptionsBitmask(Options.INVULNERABLE | Options.CONVERSABLE);
			} else if (Template.startsWith("object/creature/droid/crafted/")) {
				((CreatureObject) object).setOptionsBitmask(Options.NONE);
			} else if (Template.startsWith("object/creature/droid/")) {
				((CreatureObject) object).setOptionsBitmask(Options.ATTACKABLE | Options.INVULNERABLE);
			} else if (Template.startsWith("object/creature/player/")) {
				((CreatureObject) object).setOptionsBitmask(Options.ATTACKABLE);
			}
		} else if (object instanceof TangibleObject) {
			((TangibleObject) object).setOptionsBitmask(Options.INVULNERABLE);
			
			if (Template.startsWith("object/tangible/vendor/")) {
				((TangibleObject) object).setOptionsBitmask(Options.INVULNERABLE | Options.USABLE);
			}
		}
		
		if(loadServerTemplate)
			loadServerTemplate(object);		
		else {
			final SWGObject pointer = object;
			loadServerTemplateTasks.add(() -> loadServerTemplate(pointer));
		}
		
		objectList.put(objectID, object);
		
		return object;
	}
	
	public void loadServerTemplate(SWGObject object) {
		
		String template = ((object.getAttachment("customServerTemplate") == null) ? object.getTemplate() : (object.getTemplate().split("shared_")[0] + "shared_" + ((String) object.getAttachment("customServerTemplate")) + ".iff"));
		String serverTemplate = template.replace(".iff", "");
		// check if template is empty(4 default lines) to reduce RAM usage(saves about 500 MB of RAM)
		try {
			int numberOfLines = FileUtilities.getNumberOfLines("scripts/" + serverTemplate.split("shared_" , 2)[0].replace("shared_", "") + serverTemplate.split("shared_" , 2)[1] + ".py");
			
			if(numberOfLines > 4) {
				if(serverTemplates.containsKey(template)) {
					PyObject func = serverTemplates.get(template);
					func.__call__(Py.java2py(core), Py.java2py(object));
				} else {
					PyObject func = core.scriptService.getMethod("scripts/" + serverTemplate.split("shared_" , 2)[0].replace("shared_", ""), serverTemplate.split("shared_" , 2)[1], "setup");
					func.__call__(Py.java2py(core), Py.java2py(object));
					serverTemplates.put(template, func);
				}
			}

		} catch (FileNotFoundException e) {
			System.out.println("!File Not Found:" + template.toString());
		} catch (IOException e) {
			System.out.println("!IO error " + template.toString());
		}
		object.setAttachment("hasLoadedServerTemplate", new Boolean(true));
	}
	
	public SWGObject createObject(String Template, Planet planet) {
		return createObject(Template, 0, planet, new Point3D(0, 0, 0), new Quaternion(1, 0, 0, 0));
	}
	
	public SWGObject createObject(String Template, Planet planet, String customServerTemplate) {
		return createObject(Template, 0, planet, new Point3D(0, 0, 0), new Quaternion(1, 0, 0, 0), customServerTemplate);
	}
	
	public SWGObject createObject(String Template, Planet planet, float x, float z, float y) {
		return createObject(Template, 0, planet, new Point3D(x, y, z), new Quaternion(1, 0, 0, 0));
	}
	
	public SWGObject createObject(String Template, long objectID, Planet planet, Point3D position, Quaternion orientation) {
		return createObject(Template, objectID, planet, position, orientation, null);	
	}
	
	public void addObjectToScene(SWGObject object) {
		
		core.simulationService.add(object, object.getPosition().x, object.getPosition().z);
		
		// TODO: Get Objects in range and contained objects, send packets, add to observer lists
	}
	
	public SWGObject getObject(long objectID) {
		return objectList.get(objectID);
	}
	
	public Map<Long, SWGObject> getObjectList() {
		return objectList;
	}
	
	public void destroyObject(final SWGObject object, int seconds) {
		scheduler.schedule(new Runnable() {
			
			@Override
			public void run() {
				destroyObject(object);
			}
			
		}, seconds, TimeUnit.SECONDS);
	}
	
	public void destroyObject(SWGObject object) {
		if (object == null) {
			return;
		}
		
		if (object.getAttachment("AI") != null && object.getAttachment("AI") instanceof AIActor && ((AIActor) object.getAttachment("AI")).getMobileTemplate().getRespawnTime() > 0) {
			final long objectId = object.getObjectID();
			final String Template = object.getTemplate();
			final Planet planet = object.getPlanet();
			final Point3D position = object.getPosition();
			final Quaternion orientation = object.getOrientation();
			final long cellId = ((object.getContainer() == null) ? 0L : object.getContainer().getObjectID());
			final short level = ((object instanceof CreatureObject) ? ((CreatureObject) object).getLevel() : (short) 0);
			
			scheduler.schedule(new Runnable() {
				
				@Override
				public void run() {
					NGECore.getInstance().spawnService.spawnCreature(Template, objectId, planet.getName(), cellId, position.x, position.y, position.z, orientation.w, orientation.x, orientation.y, orientation.z, level);
				}
				
			}, ((AIActor) object.getAttachment("AI")).getMobileTemplate().getRespawnTime(), TimeUnit.SECONDS);
		}
		
		String filePath = "scripts/" + object.getTemplate().split("shared_" , 2)[0].replace("shared_", "") + object.getTemplate().split("shared_" , 2)[1].replace(".iff", "") + ".py";
		
		if (FileUtilities.doesFileExist(filePath)) {
			PyObject method = core.scriptService.getMethod("scripts/" + object.getTemplate().split("shared_" , 2)[0].replace("shared_", ""), object.getTemplate().split("shared_" , 2)[1].replace(".iff", ""), "destroy");
			
			if (method != null && method.isCallable()) {
				method.__call__(Py.java2py(core), Py.java2py(object));
			}
		}
		
		if (object == null) {
			return;
		}
		
		object.viewChildren(object, true, true, new Traverser() {
			@Override
			public void process(SWGObject obj) {
				objectList.remove(obj.getObjectID());
			}
		});
		objectList.remove(object.getObjectID());
		SWGObject parent = object.getContainer();

		if(parent != null) {
			if(parent instanceof CreatureObject) {
				core.equipmentService.unequip((CreatureObject) parent, object);
				
				((CreatureObject) parent).removeObjectFromEquipList(object);
				((CreatureObject) parent).removeObjectFromAppearanceEquipList(object);
			}
			long parentId = object.getParentId();
			parent.remove(object);
			object.setParentId(parentId);
		} else {
			core.simulationService.remove(object, object.getWorldPosition().x, object.getWorldPosition().z, true);
		}
		
	}
	
	public void destroyObject(long objectID) {
		
		SWGObject object = getObject(objectID);
		if(object != null) {
			destroyObject(object);
		}
		
	}
	
	public SWGObject getObjectByCustomName(String customName) {
		if (customName == null) {
			return null;
		}
		
		synchronized(objectList) {
			
			for(SWGObject obj : objectList.values()) {
				if(obj.getCustomName() == null)
					continue;
				if(obj.getCustomName().equals(customName))
					return obj;
			}
			
		}
		
		ODBCursor cursor = core.getSWGObjectODB().getCursor();
		
		while (cursor.hasNext()) {
			SWGObject object = (SWGObject) cursor.next();
			
			if (object == null) {
				continue;
			}
			
			if (object.getCustomName() != null && customName.length() > 0 && object.getCustomName().equals(customName)) {
				return object;
			}
		}
		
		return null;
	}
	
	public SWGObject getObjectByFirstName(String customName) {
		if (customName == null) {
			return null;
		}
		
		synchronized(objectList) {
			for(SWGObject obj : objectList.values()) {
				if(obj == null)
					continue;
				if(obj.getCustomName() == null)
					continue;
				if(obj.getCustomName().startsWith(customName))
					return obj;
			}
			
		}
		
		ODBCursor cursor = core.getSWGObjectODB().getCursor();
		
		while (cursor.hasNext()) {
			SWGObject object = (SWGObject) cursor.next();
			
			if (object == null) {
				continue;
			}
			
			if (object.getCustomName() != null && customName.length() > 0 && object.getCustomName().startsWith(customName)) {
				return object;
			}
		}
		cursor.close();
		return null;
	}
	
	public CreatureObject getCreatureFromDB(long objectId) {
		SWGObject object = (SWGObject) core.getSWGObjectODB().get(objectId);
		if(!(object instanceof CreatureObject))
			return null;
		if (object != null && getObject(object.getObjectID()) == null) {
			loadServerTemplate(object);
			object.viewChildren(object, true, true, (child) -> loadServerTemplate(child));
		}
		
		return (CreatureObject) object;
	}
	
	public long generateObjectID() {
		/*Random random = new Random();
		
		long objectID = random.nextInt();
		
		if(getObject(objectID) != null)
			return generateObjectID();
		
		if(core.getCreatureODB().contains(new Long(objectID), Long.class, CreatureObject.class))
			return generateObjectID();

		return objectID;*/
		
		long newId = 0;
		boolean found = false;
		// stack overflow when using recursion
		synchronized(objectMutex) {
			while(!found) {
				newId = highestId.incrementAndGet();
				PreparedStatement ps2;
				try {
					ps2 = databaseConnection.preparedStatement("UPDATE highestid SET id=" + newId + " WHERE id=(SELECT MAX(id) FROM highestid)");
					ps2.executeUpdate();
					ps2.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				if(objectList.containsKey(newId) || core.getObjectIdODB().contains(newId))
					found = false;
				else
					found = true;
			}
		}
		
		return newId;		

	}
	
	public long getDOId(String planet, String template, int type, long containerId, int cellNumber, float x1, float y, float z1) {
		SWGObject container = getObject(containerId);
		float x = ((container == null) ? x1 : container.getPosition().x + x1);
		float z = ((container == null) ? z1 : container.getPosition().z + z1);
		String key = "" + CRC.StringtoCRC(planet) + CRC.StringtoCRC(template) + type + containerId + cellNumber + x + y + z;
		
		long objectId = 0;
		
		if (core.getDuplicateIdODB().contains(key)) {
			objectId = ((DuplicateId) core.getDuplicateIdODB().get(key)).getObjectId();
		} else {
			objectId = generateObjectID();
			core.getDuplicateIdODB().put(key, new DuplicateId(key, objectId));
		}
		
		return objectId;
	}
	
	public void useObject(CreatureObject creature, SWGObject object) {
		if (creature == null || object == null) {
			return;
		}
		
		creature.setUseTarget(object);
		
		int reuse_time;
		
		try {
			reuse_time = object.getIntAttribute("reuse_time");
		} catch (NumberFormatException e) {
			reuse_time = 0;
		}
		
		try {
			DatatableVisitor visitor = ClientFileManager.loadFile("datatables/timer/template_command_mapping.iff", DatatableVisitor.class);
			
			for (int i = 0; i < visitor.getRowCount(); i++) {
				if (visitor.getObject(i, 0) != null && ((String) (visitor.getObject(i, 0))).equalsIgnoreCase(object.getTemplate())) {
					String commandName = (String) visitor.getObject(i, 1);
					String cooldownGroup = (String) visitor.getObject(i, 2);
					String animation = (String) visitor.getObject(i, 3);
					
					if (commandName.length() > 0) {
						BaseSWGCommand command = core.commandService.getCommandByName(commandName);
						
						if (command instanceof CombatCommand && animation.length() > 0) {
							((CombatCommand) command).setDefaultAnimations(new String[] { animation });
						}
						
						if (core.commandService.callCommand(creature, object, command, 0, "")) {
							core.commandService.removeCommand(creature, 0, command);
							return;
						}
					} else if (cooldownGroup.length() > 0) {
						if (creature.hasCooldown(cooldownGroup)) {
							return;
						}
						
						creature.addCooldown(cooldownGroup, object.getIntAttribute("reuse_time"));
					}
					
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (object instanceof TangibleObject) {
			TangibleObject item = (TangibleObject) object;
			int uses = item.getUses();
			
			if (uses > 0)  {
				item.setUses(uses--);
				
				if (item.getUses() == 0) {
					destroyObject(object);
				}
			}
		}
		
		if (object.getStringAttribute("proc_name") != null) {
			core.buffService.addBuffToCreature(creature, object.getStringAttribute("proc_name").replace("@ui_buff:", ""), creature);
		}
		
		String filePath = "scripts/" + object.getTemplate().split("shared_" , 2)[0].replace("shared_", "") + object.getTemplate().split("shared_" , 2)[1].replace(".iff", "") + ".py";
		
		if (FileUtilities.doesFileExist(filePath)) {
			filePath = "scripts/" + object.getTemplate().split("shared_" , 2)[0].replace("shared_", "");
			String fileName = object.getTemplate().split("shared_" , 2)[1].replace(".iff", "");
			
			PyObject method1 = core.scriptService.getMethod("scripts/" + object.getTemplate().split("shared_" , 2)[0].replace("shared_", ""), object.getTemplate().split("shared_" , 2)[1].replace(".iff", ""), "use");
			PyObject method2 = core.scriptService.getMethod("scripts/" + object.getTemplate().split("shared_" , 2)[0].replace("shared_", ""), object.getTemplate().split("shared_" , 2)[1].replace(".iff", ""), "useObject");
			
			if (method1 != null && method1.isCallable()) {
				method1.__call__(Py.java2py(core), Py.java2py(creature), Py.java2py(object));
			} else if (method2 != null && method2.isCallable()) {
				method2.__call__(Py.java2py(core), Py.java2py(creature), Py.java2py(object));
			}
		}
	}
	
	public void insertTimedEventBindings(ScheduledExecutorService executor) {
		
	}

	public void insertOpcodes(Map<Integer,INetworkRemoteEvent> swgOpcodes, Map<Integer,INetworkRemoteEvent> objControllerOpcodes) {
		
		swgOpcodes.put(Opcodes.SelectCharacter, new INetworkRemoteEvent() {

			@Override
			public void handlePacket(IoSession session, IoBuffer data) throws Exception {
				
				data = data.order(ByteOrder.LITTLE_ENDIAN);
				data.position(0);
				SelectCharacter selectCharacter = new SelectCharacter();
				selectCharacter.deserialize(data);

				long objectId = selectCharacter.getCharacterId();
				Client client = core.getClient(session);
				if(client == null) {
					System.out.println("NULL Client");
					return;
				}
				CreatureObject creature = null;
				if(getObject(objectId) == null) {
										
					creature = getCreatureFromDB(objectId);
					if(creature == null) {
						System.out.println("Cant get creature from db");
					}
					
				} else {
					
					if (!(getObject(objectId) instanceof CreatureObject))
						return;

					creature = (CreatureObject) getObject(objectId);
					if(creature.getAttachment("disconnectTask") != null && creature.getClient() != null && !creature.getClient().getSession().isClosing())
						return;
					

				}
				
				PlayerObject ghost = (PlayerObject) creature.getSlottedObject("ghost");

				if(ghost == null)
					return;
				
				ghost.clearFlagBitmask(PlayerFlags.LD);
				
				if(creature.getAttachment("disconnectTask") != null) {
					((ScheduledFuture<?>) creature.getAttachment("disconnectTask")).cancel(true);
				}					
				creature.getAwareObjects().removeAll(creature.getAwareObjects());
				creature.setAttachment("disconnectTask", null);

				creature.setClient(client);
				Planet planet = core.terrainService.getPlanetByID(creature.getPlanetId());
				creature.setPlanet(planet);
				client.setParent(creature);
				
				objectList.put(creature.getObjectID(), creature);
				
				creature.viewChildren(creature, true, true, (object) -> {
					objectList.put(object.getObjectID(), object);
				});
				
				creature.viewChildren(creature, true, true, (object) -> {
					if(object.getMutex() == null)
						object.initAfterDBLoad();
					if(object.getParentId() != 0 && object.getContainer() == null)
						object.setParent(getObject(object.getParentId()));
					object.getContainerInfo(object.getTemplate());
					if(getObject(object.getObjectID()) == null)
						objectList.put(object.getObjectID(), object);					
				});

				if(creature.getParentId() != 0) {
					SWGObject parent = getObject(creature.getParentId());
					System.out.println("Building: " + parent.getContainer().getTemplate());
					parent._add(creature);
				}

				Point3D position = creature.getPosition();
				
				if(Float.isNaN(position.x) || Float.isNaN(position.y) || Float.isNaN(position.z)) {
					creature.setPosition(new Point3D(0, 0, 0));
					position = creature.getPosition();
				}
				
				HeartBeatMessage heartBeat = new HeartBeatMessage();
				session.write(heartBeat.serialize());
		
				UnkByteFlag unkByteFlag = new UnkByteFlag();
				session.write(unkByteFlag.serialize());
				
				core.buffService.clearBuffs(creature);

				CmdStartScene startScene = new CmdStartScene((byte) 0, objectId, creature.getPlanet().getPath(), creature.getTemplate(), position.x, position.y, position.z, core.getGalacticTime() / 1000, 0);
				session.write(startScene.serialize());

				ChatServerStatus chatServerStatus = new ChatServerStatus();
				client.getSession().write(chatServerStatus.serialize());
				
				VoiceChatStatus voiceStatus = new VoiceChatStatus();
				client.getSession().write(voiceStatus.serialize());
				
				ParametersMessage parameters = new ParametersMessage();
				session.write(parameters.serialize());
				
				ChatOnConnectAvatar chatConnect = new ChatOnConnectAvatar();
				creature.getClient().getSession().write(chatConnect.serialize());
				
				creature.makeAware(core.guildService.getGuildObject());
				core.chatService.loadMailHeaders(client);
				
				core.simulationService.handleZoneIn(client);
				
				creature.makeAware(creature);
				
				//ChatOnGetFriendsList friendsListMessage = new ChatOnGetFriendsList(ghost);
				//client.getSession().write(friendsListMessage.serialize());
				
				if (ghost != null) {
					//ghost.clearFlagBitmask(PlayerFlags.LD);
					String objectShortName = creature.getCustomName().toLowerCase();
					
					if (creature.getCustomName().contains(" ")) {
						String[] splitName = creature.getCustomName().toLowerCase().split(" ");
						objectShortName = splitName[0].toLowerCase();
					}
					
					core.chatService.playerStatusChange(objectShortName, (byte) 1);
					
					if (!ghost.getFriendList().isEmpty()) {

						// Find out what friends are online/offline
						for (String friend : ghost.getFriendList()) {
							SWGObject friendObject = core.chatService.getObjectByFirstName(friend);
							
							if(friendObject != null && friendObject.isInQuadtree()) {
								ChatFriendsListUpdate onlineNotifyStatus = new ChatFriendsListUpdate(friend, (byte) 1);
								client.getSession().write(onlineNotifyStatus.serialize());

							} else {
								ChatFriendsListUpdate onlineNotifyStatus = new ChatFriendsListUpdate(friend, (byte) 0);
								client.getSession().write(onlineNotifyStatus.serialize());
							}
						}
					}
					
					for (Integer roomId : ghost.getJoinedChatChannels()) {
						ChatRoom room = core.chatService.getChatRoom(roomId);
						
						if (room != null) { core.chatService.joinChatRoom(objectShortName, roomId); } 
						// work-around for any channels that may have been deleted, or only spawn on server startup, that were added to the joined channels
						else { ghost.removeChannel(roomId); }
					}
				}
				
				if(!core.getConfig().getString("MOTD").equals(""))
					creature.sendSystemMessage(core.getConfig().getString("MOTD"), (byte) 2);
				
				if (core.getBountiesODB().contains(creature.getObjectID()))
					core.missionService.getBountyMap().put(creature.getObjectID(), (BountyListItem) core.getBountiesODB().get(creature.getObjectID()));
				
				if (creature.getSlottedObject("datapad") != null) {
					creature.getSlottedObject("datapad").viewChildren(creature, true, false, new Traverser() {

						@Override
						public void process(SWGObject obj) {
							if (obj instanceof MissionObject) {
								MissionObject mission = (MissionObject) obj;
								if (mission.getMissionType().equals("bounty")) {
									((BountyMissionObjective) mission.getObjective()).checkBountyActiveStatus(core);
								}
							}
						}
					});
				}
				
				core.playerService.postZoneIn(creature);
			}
			
		});
		
	}

	public void shutdown() {
		
	}

	public void loadSnapshotObjects(Planet planet) {
		
		System.out.println("Loading client objects for: " + planet.getName());
		WorldSnapshotVisitor visitor = planet.getSnapshotVisitor();
		int counter = 0;
		for(SnapshotChunk chunk : visitor.getChunks()) {
			++counter;
			// Since the ids are just ints, they append 0xFFFF86F9 to them
			// This is demonstated in the packet sent to the server when you /target client-spawned buildouts
			// This is done for buildouts; uncertain about snapshot objects so it's commented for now
			//long objectId = Delta.createBuffer(8).putInt(chunk.id).putInt(0xF986FFFF).flip().getLong(); // Not sure what extension they add to 4-byte-only snapshot objectIds.  With buildouts they add 0xFFFF86F9.  This is demonstated in the packet sent to the server when you /target client-spawned objects
			int objectId = chunk.id;
			SWGObject obj = createObject(visitor.getName(chunk.nameId), objectId, planet, new Point3D(chunk.xPosition, chunk.yPosition, chunk.zPosition), new Quaternion(chunk.orientationW, chunk.orientationX, chunk.orientationY, chunk.orientationZ), null, false, false);
			if(obj != null) {
				obj.setContainerPermissions(WorldPermissions.WORLD_PERMISSIONS);
				obj.setisInSnapshot(true);
				obj.setParentId(chunk.parentId);
				if(obj instanceof CellObject) {
					((CellObject) obj).setCellNumber(chunk.cellNumber);
					obj.setContainerPermissions(WorldCellPermissions.WORLD_CELL_PERMISSIONS);
				}
			}
			//System.out.print("\rLoading Object [" + counter + "/" +  visitor.getChunks().size() + "] : " + visitor.getName(chunk.nameId));
        }
		visitor.dispose();
		synchronized(objectList) {
			for(SWGObject obj : objectList.values()) {
				if(obj.getParentId() != 0 && getObject(obj.getParentId()) != null) {
					SWGObject parent = getObject(obj.getParentId());
					parent.add(obj);
				} 
			}
		}
		
		System.out.println("Finished loading client objects for: " + planet.getName());
		
	}
	/**
	 * Creates a child object and places it at a position and orientation offset from the parent object.
	 * @param parent The parent Object.
	 * @param template The template file of the child.
	 * @param position The position as an offset to the parent object.
	 * @param orientation The orientation as an offset to the parent object.
	 */
	@SuppressWarnings("unchecked")
	public SWGObject createChildObject(SWGObject parent, String template, Point3D position, Quaternion orientation, int cellNumber) {
		
		if(cellNumber == -1) {
		
			float radians = parent.getRadians();
			Point3D parentPos = parent.getWorldPosition();
			
			float x = (float) ((Math.cos(radians) * position.x) + (Math.sin(radians) * position.z));
			float y = position.y + parentPos.y;
			float z = (float) ((Math.cos(radians) * position.z) - (Math.sin(radians) * position.x));
			
			x += parentPos.x;
			z += parentPos.z;
			
			position = new Point3D(x, y, z);
			orientation = MathUtilities.rotateQuaternion(orientation, radians, new Point3D(0, 1, 0));
			
		}
		
		SWGObject child = createObject(template, 0, parent.getPlanet(), position, orientation);
		child.setContainerPermissions(WorldPermissions.WORLD_PERMISSIONS);
		if(parent.getAttachment("childObjects") == null)
			parent.setAttachment("childObjects", new Vector<SWGObject>());
		
		((Vector<SWGObject>) parent.getAttachment("childObjects")).add(child);
		
		if(cellNumber != -1)
			child.setAttachment("cellNumber", cellNumber);
		
		//core.simulationService.add(child, x, z);
		return child;
	}
	
	public SWGObject createChildObject(SWGObject parent, String template, float x, float y, float z, float qy, float qw) {
		return createChildObject(parent, template, new Point3D(x, y, z), new Quaternion(qw, 0, qy, 0), -1);
	}
	
	public SWGObject createChildObject(SWGObject parent, String template, float x, float y, float z, float qy, float qw, int cellNumber) {
		return createChildObject(parent, template, new Point3D(x, y, z), new Quaternion(qw, 0, qy, 0), cellNumber);
	}
	
	public void loadBuildoutObjects(Planet planet) throws InstantiationException, IllegalAccessException {
		
		DatatableVisitor buildoutTable = ClientFileManager.loadFile("datatables/buildout/areas_" + planet.getName() + ".iff", DatatableVisitor.class);
		
		for (int i = 0; i < buildoutTable.getRowCount(); i++) {
			
			String areaName = (String) buildoutTable.getObject(i, 0);
			float x1 = (Float) buildoutTable.getObject(i, 1);
			float z1 = (Float) buildoutTable.getObject(i, 2);
			
			readBuildoutDatatable(ClientFileManager.loadFile("datatables/buildout/" + planet.getName() + "/" + areaName + ".iff", DatatableVisitor.class), planet, x1, z1);

		}
	}
	
	public void readBuildoutDatatable(DatatableVisitor buildoutTable, Planet planet, float x1, float z1) throws InstantiationException, IllegalAccessException {

		CrcStringTableVisitor crcTable = ClientFileManager.loadFile("misc/object_template_crc_string_table.iff", CrcStringTableVisitor.class);
		List<BuildingObject> persistentBuildings = new ArrayList<BuildingObject>();
		Map<Long, Long> duplicate = new HashMap<Long, Long>();

		for (int i = 0; i < buildoutTable.getRowCount(); i++) {
			
			String template;
			
			if(buildoutTable.getColumnCount() <= 11)
				template = crcTable.getTemplateString((Integer) buildoutTable.getObject(i, 0));
			else
				template = crcTable.getTemplateString((Integer) buildoutTable.getObject(i, 3));
			
			if(template != null) {
				
				float px, py, pz, qw, qx, qy, qz, radius;
				long objectId = 0, containerId = 0;
				int type = 0, cellIndex = 0, portalCRC;
				
				if(buildoutTable.getColumnCount() <= 11) {

					cellIndex = (Integer) buildoutTable.getObject(i, 1);
					px = (Float) buildoutTable.getObject(i, 2);
					py = (Float) buildoutTable.getObject(i, 3);
					pz = (Float) buildoutTable.getObject(i, 4);
					qw = (Float) buildoutTable.getObject(i, 5);
					qx = (Float) buildoutTable.getObject(i, 6);
					qy = (Float) buildoutTable.getObject(i, 7);
					qz = (Float) buildoutTable.getObject(i, 8);
					radius = (Float) buildoutTable.getObject(i, 9);
					portalCRC = (Integer) buildoutTable.getObject(i, 10);

				} else {
					
					// Since the ids are just ints, they append 0xFFFF86F9 to them
					// This is demonstated in the packet sent to the server when you /target client-spawned objects
					objectId = (((Integer) buildoutTable.getObject(i, 0) == 0) ? 0 : Delta.createBuffer(8).putInt((Integer) buildoutTable.getObject(i, 0)).putInt(0xF986FFFF).flip().getLong());
					containerId = (((Integer) buildoutTable.getObject(i, 1) == 0) ? 0 : Delta.createBuffer(8).putInt((Integer) buildoutTable.getObject(i, 1)).putInt(0xF986FFFF).flip().getLong());
					type = (Integer) buildoutTable.getObject(i, 2);
					cellIndex = (Integer) buildoutTable.getObject(i, 4);
					
					px = (Float) buildoutTable.getObject(i, 5);
					py = (Float) buildoutTable.getObject(i, 6);
					pz = (Float) buildoutTable.getObject(i, 7);
					qw = (Float) buildoutTable.getObject(i, 8);
					qx = (Float) buildoutTable.getObject(i, 9);
					qy = (Float) buildoutTable.getObject(i, 10);
					qz = (Float) buildoutTable.getObject(i, 11);
					radius = (Float) buildoutTable.getObject(i, 12);
					portalCRC = (Integer) buildoutTable.getObject(i, 13);

				}
				
				// Treeku - Refactored to work around duplicate objectIds
				// Required for instances/heroics which are duplicated ie. 10 times
				if(!template.equals("object/cell/shared_cell.iff") && objectId != 0 && getObject(objectId) != null) {
					SWGObject object = getObject(objectId);
					
					// Same coordinates is a true duplicate
					if ((px + ((containerId == 0) ? 0 : x1)) == object.getPosition().x &&
						py == object.getPosition().y &&
						(pz + ((containerId == 0) ? 0 : z1)) == object.getPosition().z) {
						//System.out.println("Duplicate buildout object: " + template);
						continue;
					}
				}
				
				if (duplicate.containsKey(containerId)) {
					containerId = duplicate.get(containerId);
				}
				
				String planetName = planet.getName();
				
				// TODO needs to a way to work for mustafar and kashyyyk which both have instances
				if (objectId != 0 && getObject(objectId) != null && (planetName.contains("dungeon") || planetName.contains("adventure"))) {
					SWGObject container = getObject(containerId);
					float x = (px + ((container == null) ? x1 : container.getPosition().x));
					float z = (pz + ((container == null) ? z1 : container.getPosition().z));
					String key = "" + CRC.StringtoCRC(planet.getName()) + CRC.StringtoCRC(template) + type + containerId + cellIndex + x + py + z;
					long newObjectId = 0;
					
					if (core.getDuplicateIdODB().contains(key)) {
						newObjectId = ((DuplicateId) core.getDuplicateIdODB().get(key)).getObjectId();
					} else {
						newObjectId = generateObjectID();
						core.getDuplicateIdODB().put(key, new DuplicateId(key, newObjectId));
					}
					
					duplicate.put(objectId, newObjectId);
					objectId = newObjectId;
				}

				List<Long> containers = new ArrayList<Long>();
				SWGObject object;
				if(objectId != 0 && containerId == 0) {					
					if(portalCRC != 0) {
						if (core.getSWGObjectODB().contains(objectId) && !duplicate.containsValue(objectId))
							continue;
						containers.add(objectId);
						object = createObject(template, objectId, planet, new Point3D(px + x1, py, pz + z1), new Quaternion(qw, qx, qy, qz), null, true, false);
						object.setAttachment("childObjects", null);
						
						/*if (!duplicate.containsValue(objectId)) {
							((BuildingObject) object).createTransaction(core.getBuildingODB().getEnvironment());
							core.getBuildingODB().put((BuildingObject) object, Long.class, BuildingObject.class, ((BuildingObject) object).getTransaction());
							((BuildingObject) object).getTransaction().commitSync();
						}*/
					} else {
						object = createObject(template, 0, planet, new Point3D(px + x1, py, pz + z1), new Quaternion(qw, qx, qy, qz), null, false, false);
					}
					if(object == null)
						continue;
					object.setContainerPermissions(WorldPermissions.WORLD_PERMISSIONS);
					if(radius > 256)
						object.setAttachment("bigSpawnRange", new Boolean(true));
					if (!duplicate.containsValue(objectId) && object instanceof BuildingObject && portalCRC != 0)
						persistentBuildings.add((BuildingObject) object);
				} else if(containerId != 0) {
					object = createObject(template, 0, planet, new Point3D(px, py, pz), new Quaternion(qw, qx, qy, qz), null, false, false);
					if(containers.contains(containerId)) {
						object.setContainerPermissions(WorldPermissions.WORLD_PERMISSIONS);
						object.setisInSnapshot(false);
						containers.add(objectId);
					}
					if(object instanceof CellObject && cellIndex != 0) {
						object.setContainerPermissions(WorldCellPermissions.WORLD_CELL_PERMISSIONS);
						((CellObject) object).setCellNumber(cellIndex);
					}
					SWGObject parent = getObject(containerId);
					
					if(parent != null && object != null) {
						if(parent instanceof BuildingObject && ((BuildingObject) parent).getCellByCellNumber(cellIndex) != null)
							continue;
						parent.add(object);
					}
				} else {
					object = createObject(template, 0, planet, new Point3D(px + x1, py, pz + z1), new Quaternion(qw, qx, qy, qz), null, false, false);
					object.setContainerPermissions(WorldPermissions.WORLD_PERMISSIONS);
				}
				
				if (object != null && object instanceof TangibleObject && !(object instanceof CreatureObject)) {
					((TangibleObject) object).setStaticObject(true);
				}
				
				//System.out.println("Spawning: " + template + " at: X:" + object.getPosition().x + " Y: " + object.getPosition().y + " Z: " + object.getPosition().z);
				if(object != null)
					object.setAttachment("isBuildout", new Boolean(true));
			}
				
			
		}

		for(BuildingObject building : persistentBuildings) {
			core.getSWGObjectODB().put(building.getObjectID(), building);
			destroyObject(building);
		}
		
	}

	public int objsInContainer(SWGObject owner, TangibleObject container) {
		if (owner == null) {
			Console.println("Owner null!");
		}
		if (container == null) {
			Console.println("Container is null!");
		}
		final AtomicInteger count = new AtomicInteger();
		
		container.viewChildren(owner, false, false, new Traverser() {

			@Override
			public void process(SWGObject child) {
				count.getAndIncrement();
			}
			
		});
		
		return count.get();
	}
	
	public void persistObject(long key, Object value, ObjectDatabase odb) {
		odb.put(key, value);
	}
	
	public void deletePersistentObject(long key, ObjectDatabase odb) {
		odb.remove(key);
	}
	
}
