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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;

import protocol.swg.CharacterSheetResponseMessage;
import protocol.swg.ClientIdMsg;
import protocol.swg.ClientMfdStatusUpdateMessage;
import protocol.swg.CollectionServerFirstListRequest;
import protocol.swg.CollectionServerFirstListResponse;
import protocol.swg.CreateClientPathMessage;
import protocol.swg.ExpertiseRequestMessage;
import protocol.swg.GuildRequestMessage;
import protocol.swg.GuildResponseMessage;
import protocol.swg.ObjControllerMessage;
import protocol.swg.PlayerMoneyResponse;
import protocol.swg.ServerTimeMessage;
import protocol.swg.SetWaypointColor;
import protocol.swg.ShowBackpack;
import protocol.swg.ShowHelmet;
import protocol.swg.objectControllerObjects.ChangeRoleIconChoice;
import protocol.swg.objectControllerObjects.ShowFlyText;
import protocol.swg.objectControllerObjects.ShowLootBox;
import resources.buffs.Buff;
import resources.common.BountyListItem;
import resources.common.FileUtilities;
import resources.common.ObjControllerOpcodes;
import resources.common.Opcodes;
import resources.common.OutOfBand;
import resources.common.ProsePackage;
import resources.common.SpawnPoint;
import resources.datatables.DisplayType;
import resources.datatables.Options;
import resources.datatables.PlayerFlags;
import resources.datatables.Professions;
import resources.guild.Guild;
import resources.objects.building.BuildingObject;
import resources.objects.cell.CellObject;
import resources.objects.creature.CreatureObject;
import resources.objects.player.PlayerMessageBuilder;
import resources.objects.player.PlayerObject;
import resources.objects.tangible.TangibleObject;
import resources.objects.waypoint.WaypointObject;
import services.sui.SUIService.InputBoxType;
import services.sui.SUIService.ListBoxType;
import services.sui.SUIWindow;
import services.sui.SUIWindow.Trigger;
import services.sui.SUIWindow.SUICallback;
import main.NGECore;
import engine.clientdata.ClientFileManager;
import engine.clientdata.visitors.CrcStringTableVisitor;
import engine.clientdata.visitors.DatatableVisitor;
import engine.clients.Client;
import engine.resources.common.CRC;
import engine.resources.common.RGB;
import engine.resources.container.Traverser;
import engine.resources.objects.SWGObject;
import engine.resources.scene.Point3D;
import engine.resources.scene.Quaternion;
import engine.resources.service.INetworkDispatch;
import engine.resources.service.INetworkRemoteEvent;

@SuppressWarnings("unused")

public class PlayerService implements INetworkDispatch {
	
	private NGECore core;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private float xpMultiplier;
    protected final Object objectMutex = new Object();
    private ConcurrentHashMap<Long, List<ScheduledFuture<?>>> schedulers = new ConcurrentHashMap<Long, List<ScheduledFuture<?>>>();
    
	public PlayerService(final NGECore core) {
		this.core = core;
		this.xpMultiplier = (float) core.getConfig().getDouble("XPMULTIPLIER");
		if(xpMultiplier == 0)
			xpMultiplier = 1;
	}
	
	public void postZoneIn(final CreatureObject creature) {
		if(schedulers.get(creature.getObjectID()) != null)
			return;
		
		List<ScheduledFuture<?>> scheduleList = new ArrayList<ScheduledFuture<?>>();
		
		scheduleList.add(scheduler.scheduleAtFixedRate(() -> {
			try {
				ServerTimeMessage time = new ServerTimeMessage(core.getGalacticTime() / 1000);
				IoBuffer packet = time.serialize();
				creature.getClient().getSession().write(packet);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, 45, 45, TimeUnit.SECONDS));
		
		scheduleList.add(scheduler.scheduleAtFixedRate(() -> {
			try {
				PlayerObject player = (PlayerObject) creature.getSlottedObject("ghost");
				player.setTotalPlayTime((int) (player.getTotalPlayTime() + ((System.currentTimeMillis() - player.getLastPlayTimeUpdate()) / 1000)));
				player.setLastPlayTimeUpdate(System.currentTimeMillis());
				core.collectionService.checkExplorationRegions(creature);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, 30, 30, TimeUnit.SECONDS));
		
		scheduleList.add(scheduler.scheduleAtFixedRate(() -> {
			try {
				if (creature.isInStealth() && !creature.getOption(Options.INVULNERABLE) && ((PlayerObject) creature.getSlottedObject("ghost")).getGodLevel() == 0) {
					List<SWGObject> objects = core.simulationService.get(creature.getPlanet(), creature.getPosition().x, creature.getPosition().z, 64);
					
					for (SWGObject object : objects) {
						if (object == null) {
							continue;
						}
						
						if (!(object instanceof CreatureObject)) {
							continue;
						}
						
						CreatureObject observer = (CreatureObject) object;
						
						int camoflauge = creature.getSkillModBase("camoflauge") - observer.getSkillModBase("detectcamo");
						camoflauge -= (64 - creature.getPosition().getDistance(observer.getPosition()));
						
						if (new Random(camoflauge).nextInt() == camoflauge) {
							creature.setInStealth(false);
						}
					}
				}
				
				if (creature.getDefendersList().size() == 0 && creature.isInCombat()) {
					creature.setInCombat(false);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, 15, 15, TimeUnit.SECONDS));
		
		scheduleList.add(scheduler.scheduleAtFixedRate(() -> {
			try {
				if(creature.getAction() < creature.getMaxAction() && creature.getPosture() != 14) {
					if(!creature.isInCombat())
						creature.setAction(creature.getAction() + (15 + creature.getLevel() * 5));
					else
						creature.setAction(creature.getAction() + ((15 + creature.getLevel() * 5) / 2));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, 0, 1000, TimeUnit.MILLISECONDS));

		scheduleList.add(scheduler.scheduleAtFixedRate(() -> {
			try {
				if(creature.getHealth() < creature.getMaxHealth() && !creature.isInCombat() && creature.getPosture() != 13 && creature.getPosture() != 14)
					creature.setHealth(creature.getHealth() + (36 + creature.getLevel() * 4));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, 0, 1000, TimeUnit.MILLISECONDS));
		
		scheduleList.add(scheduler.scheduleAtFixedRate(() -> {
			try {
				long[] ids = creature.getAwareObjects().stream().mapToLong(SWGObject::getObjectID).toArray();
				for(int i = 0; i < ids.length; i++) {
					for(int j = 0; j < ids.length; j++) {
						if(ids[i] == ids[j] && i != j) 
							System.err.println("Detected duplicate ids, Template " + core.objectService.getObject(ids[i]).getTemplate());
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, 0, 5000, TimeUnit.MILLISECONDS));

		schedulers.put(creature.getObjectID(), scheduleList);
		
		/*final PlayerObject ghost = (PlayerObject) creature.getSlottedObject("ghost");
		scheduler.schedule(new Runnable() {

			@Override
			public void run() {
				try {
					if (ghost.isSet(PlayerFlags.LD)) {
						ghost.toggleFlag(PlayerFlags.LD);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
		}, 1, TimeUnit.SECONDS);*/
	}

	@Override
	public void insertOpcodes(Map<Integer, INetworkRemoteEvent> swgOpcodes, Map<Integer, INetworkRemoteEvent> objControllerOpcodes) {

		swgOpcodes.put(Opcodes.ShowBackpack, (session, data) -> {
			data.order(ByteOrder.LITTLE_ENDIAN);

			Client client = core.getClient(session);

			if (client == null)
				return;

			SWGObject player = client.getParent();

			if (player == null)
				return;

			PlayerObject ghost = (PlayerObject) player.getSlottedObject("ghost");

			if (ghost == null)
				return;

			ShowBackpack sentPacket = new ShowBackpack();
			sentPacket.deserialize(data);

			ghost.setShowBackpack(sentPacket.isShowBackpack());
		});

		swgOpcodes.put(Opcodes.ShowHelmet, (session, data) -> {
			data.order(ByteOrder.LITTLE_ENDIAN);

			Client client = core.getClient(session);

			if (client == null)
				return;

			SWGObject player = client.getParent();

			if (player == null)
				return;

			PlayerObject ghost = (PlayerObject) player.getSlottedObject("ghost");

			if (ghost == null)
				return;

			ShowHelmet sentPacket = new ShowHelmet();
			sentPacket.deserialize(data);

			ghost.setShowHelmet(sentPacket.isShowHelmet());
		});

		objControllerOpcodes.put(ObjControllerOpcodes.ChangeRoleIconChoice, (session, data) -> {

			Client client = core.getClient(session);

			if (client == null)
				return;

			SWGObject object = client.getParent();

			if (object == null)
				return;

			PlayerObject player = (PlayerObject) object.getSlottedObject("ghost");

			if (player == null)
				return;

			data.order(ByteOrder.LITTLE_ENDIAN);

			ChangeRoleIconChoice packet = new ChangeRoleIconChoice();
			packet.deserialize(data);

			player.setProfessionIcon(packet.getIcon());

		});
		
		swgOpcodes.put(Opcodes.SetWaypointColor, (session, data) -> {

			data.order(ByteOrder.LITTLE_ENDIAN);
			
			Client client = core.getClient(session);
			
			if (client == null)
				return;
			
			SWGObject player = client.getParent();
			
			if (player == null)
				return;
			
			PlayerObject ghost = (PlayerObject) player.getSlottedObject("ghost");
			
			if (ghost == null)
				return;
			
			SetWaypointColor packet = new SetWaypointColor();
			packet.deserialize(data);
			
			WaypointObject packetWay = (WaypointObject) core.objectService.getObject(packet.getObjectId());
			WaypointObject obj = (WaypointObject) ghost.getWaypointFromList(packetWay);
			
			if (obj == null || packetWay != obj)
				return;
			
			String color = packet.getColor();
			switch(color) {
				case "purple":
					obj.setColor(WaypointObject.PURPLE);
					break;
				case "green":
					obj.setColor(WaypointObject.GREEN);
					break;
				case "blue":
					obj.setColor(WaypointObject.BLUE);
					break;
				case "yellow":
					obj.setColor(WaypointObject.YELLOW);
					break;
				case "white":
					obj.setColor(WaypointObject.WHITE);
					break;
				case "orange":
					obj.setColor(WaypointObject.ORANGE);
					break;
			}
			
			ghost.waypointUpdate(obj);
			
		});
		swgOpcodes.put(Opcodes.GuildRequestMessage, (session, data) -> {

			data.order(ByteOrder.LITTLE_ENDIAN);
			
			Client client = core.getClient(session);
			
			if (client == null)
				return;
			
			SWGObject player = client.getParent();
			
			if (player == null)
				return;
			
			GuildRequestMessage request = new GuildRequestMessage();
			request.deserialize(data);
			
			CreatureObject targetPlayer = (CreatureObject) core.objectService.getObject(request.getCharacterId());
			
			if (targetPlayer == null) {
				System.err.println("PlayerService::GuildRequestMessage: Target is null: " + request.getCharacterId());
				return;
			}
			
			if (targetPlayer.getGuildId() != 0) {
				Guild targetGuild = core.guildService.getGuildById(targetPlayer.getGuildId());
				GuildResponseMessage response = new GuildResponseMessage(request.getCharacterId(), targetGuild.getName());
				client.getSession().write(response.serialize());
			} else {
				GuildResponseMessage response = new GuildResponseMessage(request.getCharacterId(), "None");
				client.getSession().write(response.serialize());
			}
		
		});
		
		swgOpcodes.put(Opcodes.PlayerMoneyRequest, (session, data) -> {

			Client client = core.getClient(session);
			
			if (client == null)
				return;
			
			SWGObject player = client.getParent();
			
			if (player == null)
				return;
			
			CreatureObject creature = (CreatureObject) player;
			
			if (creature == null)
				return;
			
			PlayerMoneyResponse response = new PlayerMoneyResponse(creature.getCashCredits(), creature.getBankCredits());
			session.write(response.serialize());
			
			PlayerObject ghost = (PlayerObject) player.getSlottedObject("ghost");
			if (ghost == null)
				return;
			
			CharacterSheetResponseMessage msg = new CharacterSheetResponseMessage(ghost);
			session.write(msg.serialize());
		
		});
		
		swgOpcodes.put(Opcodes.GetSpecificMapLocationsMessage, (session, data) -> {
			
		});
		
		swgOpcodes.put(Opcodes.SetCombatSpamFilter, (session, data) -> {
			
		});
		
		swgOpcodes.put(Opcodes.SetCombatSpamRangeFilter, (session, data) -> {

		});
		
		swgOpcodes.put(Opcodes.SetLfgInterests, (session, data) -> {

		});
				
		swgOpcodes.put(Opcodes.SetFurnitureRoationDegree, (session, data) -> {

		});
		
		swgOpcodes.put(Opcodes.CommoditiesResourceTypeListRequest, (session, data) -> {

		});
		
		swgOpcodes.put(Opcodes.CollectionServerFirstListRequest, (session, data) -> {
			data.order(ByteOrder.LITTLE_ENDIAN);
			data.position(0);

			Client client = core.getClient(session);
			
			if (client == null)
				return;
			
			SWGObject player = client.getParent();
			
			if (player == null)
				return;
			
			CollectionServerFirstListRequest request = new CollectionServerFirstListRequest();
			request.deserialize(data);
			
			String server = request.getServer();
			System.out.println(server);
			if (server == null || server.equals(""))
				return;

			CollectionServerFirstListResponse response = new CollectionServerFirstListResponse(server, core.guildService.getGuildObject().getServerFirst());
			session.write(response.serialize());
		});
		
		swgOpcodes.put(Opcodes.Unknown, (session, data) -> {

		});
		
		swgOpcodes.put(Opcodes.CmdSceneReady, (session, data) -> {

		});
		
		/*swgOpcodes.put(Opcodes.ExpertiseRequestMessage, new INetworkRemoteEvent() {

			@Override
			public void handlePacket(IoSession session, IoBuffer buffer) throws Exception {
				
				buffer = buffer.order(ByteOrder.LITTLE_ENDIAN);
				buffer.position(0);
				
				ExpertiseRequestMessage expertise = new ExpertiseRequestMessage();
				expertise.deserialize(buffer);

				Client client = core.getClient((Integer) session.getAttribute("connectionId"));
				if(client == null) {
					System.out.println("NULL Client");
					return;
				}

				if(client.getParent() == null)
					return;
				
				CreatureObject creature = (CreatureObject) client.getParent();
				
				for(String expertiseName : expertise.getExpertiseSkills()) {
					handleExpertiseSkillBox(creature, expertiseName);
				}
				
				
			}
			
		});*/
		
	}
	
	public void handleExpertiseSkillBox(CreatureObject creature, String expertiseBox) {
		
		if(!FileUtilities.doesFileExist("scripts/expertise/" + expertiseBox + ".py"))
			return;
		
		core.scriptService.callScript("scripts/expertise/", expertiseBox, "addExpertisePoint", core, creature);
		
	}
	
	@SuppressWarnings("unchecked")
	public void sendCloningWindow(CreatureObject creature, final boolean pvpDeath) {
		
		//if(creature.getPosture() != 14)
		//	return;
		
		List<SWGObject> cloners = core.staticService.getCloningFacilitiesByPlanet(creature.getPlanet());
		Map<Long, String> cloneData = new HashMap<Long, String>();
		Point3D position = creature.getWorldPosition();
		
		SWGObject preDesignatedCloner = null;
		
		if(creature.getPlayerObject().getBindLocation() != 0) {
			preDesignatedCloner = core.objectService.getObject((long) creature.getPlayerObject().getBindLocation());
			if(preDesignatedCloner != null) 
				cloneData.put(preDesignatedCloner.getObjectID(), "@base_player:clone_location_registered_select_begin " + core.mapService.getClosestCityName(preDesignatedCloner) + " @base_player:clone_location_registered_select_end");
		}
		final long preDesignatedObjectId = (preDesignatedCloner != null) ? preDesignatedCloner.getObjectID() : 0;
		cloners.stream().filter(c -> c.getObjectID() != preDesignatedObjectId).forEach(c -> cloneData.put(c.getObjectID(), core.mapService.getClosestCityName(c)));
		
		final SUIWindow window = core.suiService.createListBox(ListBoxType.LIST_BOX_OK_CANCEL, "@base_player:revive_title", "@base_player:clone_prompt_header", 
				cloneData, creature, null, 0);
		Vector<String> returnList = new Vector<String>();
		returnList.add("List.lstList:SelectedRow");
		
		window.addHandler(0, "", Trigger.TRIGGER_OK, returnList, (owner, eventType, resultList) -> {
				
			if(((CreatureObject)owner).getPosture() != 14)
				return;
							
			int index = Integer.parseInt(resultList.get(0));
			
			if(window.getObjectIdByIndex(index) == 0 || core.objectService.getObject(window.getObjectIdByIndex(index)) == null)
				return;
				
				
			SWGObject cloner = core.objectService.getObject(window.getObjectIdByIndex(index));
				
			if(cloner.getAttachment("spawnPoints") == null)
				return;
			
			Vector<SpawnPoint> spawnPoints = (Vector<SpawnPoint>) cloner.getAttachment("spawnPoints");
			
			SpawnPoint spawnPoint = spawnPoints.get(new Random().nextInt(spawnPoints.size()));

			handleCloneRequest((CreatureObject) owner, (BuildingObject) cloner, spawnPoint, pvpDeath);
							
		});
		
		core.suiService.openSUIWindow(window);
		
	}
	
	public void handleCloneRequest(CreatureObject creature, BuildingObject cloner, SpawnPoint spawnPoint, boolean pvpDeath) {
		
		CellObject cell = cloner.getCellByCellNumber(spawnPoint.getCellNumber());
		
		if(cell == null)
			return;
		
		creature.setPosture((byte) 0);
		
		core.simulationService.transferToPlanet(creature, cloner.getPlanet(), spawnPoint.getPosition(), spawnPoint.getOrientation(), cell);
		
		creature.setHealth(creature.getMaxHealth());
		creature.setAction(creature.getMaxAction());
		
		creature.setSpeedMultiplierBase(1);
		creature.setTurnRadius(1);
		
		if(pvpDeath) {
			List<Buff> buffs = new ArrayList<Buff>(creature.getBuffList().get());
			buffs.stream().filter(Buff::isDecayOnPvPDeath).forEach(Buff::incDecayCounter);			
			creature.updateAllBuffs();
		}
		
		creature.setFactionStatus(0);
		core.buffService.addBuffToCreature(creature, "cloning_sickness", creature);
		
	}
	
	/*
	 * Respecs to the specified profession.
	 */
	public void respec(CreatureObject creature, String profession) {
		DatatableVisitor experienceTable;
		PlayerObject player = (PlayerObject) creature.getSlottedObject("ghost");
		int level = 0;
		
		if (creature == null || player == null) {
			return;
		}
		
		if (profession == null || !Professions.isProfession(profession)) {
			return;
		}
		
		String xpType = ((profession.contains("entertainer")) ? "entertainer" : ((profession.contains("trader")) ? "crafting" : "combat_general"));
		int experience = player.getXp(xpType);
		
		// Remove old profession abilties
		
		try {
			String[] skills;
			
			DatatableVisitor skillTemplate = ClientFileManager.loadFile("datatables/skill_template/skill_template.iff", DatatableVisitor.class);
			
			for (int s = 0; s < skillTemplate.getRowCount(); s++) {
				if (skillTemplate.getObject(s, 0) != null) {
					if (((String) skillTemplate.getObject(s, 0)).equals(player.getProfession())) {
						skills = ((String) skillTemplate.getObject(s, 4)).split(",");
						
						for (String skill : skills) {
							core.skillService.removeSkill(creature, skill);
						}
						
						break;
					}
				}
			}
		}  catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		
		core.skillService.resetExpertise(creature);
		
		player.setProfession(profession);

		try {
			experienceTable = ClientFileManager.loadFile("datatables/player/player_level.iff", DatatableVisitor.class);
			
			for (int i = 0; i < experienceTable.getRowCount(); i++) {
				if (experienceTable.getObject(i, 0) != null) {
					if (experience >= ((Integer) experienceTable.getObject(i, 1))) {
						level = (Integer) experienceTable.getObject(i, 0);
					}
				}
			}
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		
		grantLevel(creature, level);
		
		player.setProfessionIcon(Professions.get(profession));
		
		if (creature.getGuildId() != 0) {
			Guild guild = core.guildService.getGuildById(creature.getGuildId());
			
			if (guild != null && guild.getMembers().containsKey(creature.getObjectID())) {
				guild.getMember(creature.getObjectID()).setProfession(getFormalProfessionName(profession));
			}
		}
	}
	
	/*
	 * Resets to level 1
	 */
	public void resetLevel(CreatureObject creature) {
		PlayerObject player = (PlayerObject) creature.getSlottedObject("ghost");
		SWGObject inventory = creature.getSlottedObject("inventory");
		
		try {
        	for (Long equipmentId : new ArrayList<Long>(creature.getEquipmentList())) {
        		
        		SWGObject equipment = core.objectService.getObject(equipmentId);
        		
        		if (equipment == null || equipment.getTemplate().startsWith("object/tangible/hair/")) {
        			continue;
        		}
        		
        		switch (equipment.getTemplate()) {
        			case "object/tangible/inventory/shared_character_inventory.iff":
        			case "object/tangible/inventory/shared_appearance_inventory.iff":
        			case "object/tangible/datapad/shared_character_datapad.iff":
        			case "object/tangible/bank/shared_character_bank.iff":
       				case "object/tangible/mission_bag/shared_mission_bag.iff":
        			case "object/weapon/creature/shared_creature_default_weapon.iff":
        				continue;
        			default:
        				creature.transferTo(creature, inventory, equipment);
       			}
        	}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//for (SWGObject equipment : creature.getAppearanceEquipmentList()) {
			//core.equipmentService.unequip(creature, equipment);
		//}
		
		for (Buff buff : creature.getBuffList().get().toArray(new Buff[] { })) {
			if (buff.isRemoveOnRespec()) {
				core.buffService.removeBuffFromCreature(creature, buff);
			}
		}
		
		try {
			String[] skills;
			
			DatatableVisitor skillTemplate = ClientFileManager.loadFile("datatables/skill_template/skill_template.iff", DatatableVisitor.class);
			
			for (int s = 0; s < skillTemplate.getRowCount(); s++) {
				if (skillTemplate.getObject(s, 0) != null) {
					if (((String) skillTemplate.getObject(s, 0)).equals(player.getProfession())) {
						skills = ((String) skillTemplate.getObject(s, 4)).split(",");
						
						for (String skill : skills) {
							core.skillService.removeSkill(creature, skill);
						}
						
						core.skillService.addSkill(creature, skills[0]);
						
						break;
					}
				}
			}
		}  catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		
		core.skillService.resetExpertise(creature);
		
		String xpType = ((player.getProfession().contains("entertainer")) ? "entertainer" : ((player.getProfession().contains("trader")) ? "crafting" : "combat_general"));
			
		player.setXp(xpType, 0);
		creature.setXpBarValue(0);
		
		player.setProfessionWheelPosition("");
		
		core.skillModService.deductSkillMod(creature, "luck", creature.getSkillModBase("luck"));
		core.skillModService.deductSkillMod(creature, "precision", creature.getSkillModBase("precision"));
		core.skillModService.deductSkillMod(creature, "strength", creature.getSkillModBase("strength"));
		core.skillModService.deductSkillMod(creature, "constitution", creature.getSkillModBase("constitution"));
		core.skillModService.deductSkillMod(creature, "stamina", creature.getSkillModBase("stamina"));
		core.skillModService.deductSkillMod(creature, "agility", creature.getSkillModBase("agility"));
		creature.setMaxHealth(1000);
		creature.setHealth(1000);
		creature.setMaxAction(300);
		creature.setAction(300);
		creature.setGrantedHealth(0);
		
		creature.setLevel((short) 1);
		
		if (creature.getGuildId() != 0) {
			Guild guild = core.guildService.getGuildById(creature.getGuildId());
			
			if (guild != null && guild.getMembers().containsKey(creature.getObjectID())) {
				guild.getMember(creature.getObjectID()).setLevel((short) 1);
			}
		}
	}
	
	/*
	 * Grants the specified level.
	 * 1. Grant the experience.
	 * 2. Add the relevant health/action and expertise points.
	 * 3. Add skills and roadmap items.
	 */
	public void grantLevel(CreatureObject creature, int level) {
		DatatableVisitor experienceTable;
		PlayerObject player = (PlayerObject) creature.getSlottedObject("ghost");
		int experience = 0;
		
		if (player == null) {
			return;
		}
		
		if(level == 0) return;
		
		resetLevel(creature);
		
		try {
			experienceTable = ClientFileManager.loadFile("datatables/player/player_level.iff", DatatableVisitor.class);
			
			// 1. Grant the experience.
			experience = (Integer) experienceTable.getObject((level - 1), 1);
			
			String xpType = ((player.getProfession().contains("entertainer")) ? "entertainer" : ((player.getProfession().contains("trader")) ? "crafting" : "combat_general"));
			
			player.setXp(xpType, experience);
			creature.setXpBarValue(experience);
			

			// 2. Add the relevant health/action and expertise points.
			float luck = (((((float) (core.scriptService.getMethod("scripts/roadmap/", player.getProfession(), "getLuck").__call__().asInt()) + (core.scriptService.getMethod("scripts/roadmap/", creature.getStfName(), "getLuck").__call__().asInt())) / ((float) 90)) * ((float) level)));
			float precision = (((((float) (core.scriptService.getMethod("scripts/roadmap/", player.getProfession(), "getPrecision").__call__().asInt()) + (core.scriptService.getMethod("scripts/roadmap/", creature.getStfName(), "getPrecision").__call__().asInt())) / ((float) 90)) * ((float) level)));
			float strength = (((((float) (core.scriptService.getMethod("scripts/roadmap/", player.getProfession(), "getStrength").__call__().asInt()) + (core.scriptService.getMethod("scripts/roadmap/", creature.getStfName(), "getStrength").__call__().asInt())) / ((float) 90)) * ((float) level)));
			float constitution = (((((float) (core.scriptService.getMethod("scripts/roadmap/", player.getProfession(), "getConstitution").__call__().asInt()) + (core.scriptService.getMethod("scripts/roadmap/", creature.getStfName(), "getConstitution").__call__().asInt())) / ((float) 90)) * ((float) level)));
			float stamina = (((((float) (core.scriptService.getMethod("scripts/roadmap/", player.getProfession(), "getStamina").__call__().asInt()) + (core.scriptService.getMethod("scripts/roadmap/", creature.getStfName(), "getStamina").__call__().asInt())) / ((float) 90)) * ((float) level)));
			float agility = (((((float) (core.scriptService.getMethod("scripts/roadmap/", player.getProfession(), "getAgility").__call__().asInt()) + (core.scriptService.getMethod("scripts/roadmap/", creature.getStfName(), "getAgility").__call__().asInt())) / ((float) 90)) * ((float) level)));
			float health = (100 * level);
			float action = (75 * level);
			
			int healthGranted = ((Integer) experienceTable.getObject((level - 1), 4));;
			
			// 3. Add skills and roadmap items.
			for (int i = 1; i <= level; i++) {
				if ((i == 4 || i == 7 || i == 10) || ((i > 10) && (((i - 10)  % 4) == 0))) {
					int skill = ((i <= 10) ? ((i - 1) / 3) : ((((i - 10) / 4)) + 3));
					String roadmapSkillName = "";
					DatatableVisitor skillTemplate, roadmap;
					
					try {
						skillTemplate = ClientFileManager.loadFile("datatables/skill_template/skill_template.iff", DatatableVisitor.class);
						
						for (int s = 0; s < skillTemplate.getRowCount(); s++) {
							if (skillTemplate.getObject(s, 0) != null) {
								if (((String) skillTemplate.getObject(s, 0)).equals(player.getProfession())) {
									String[] skillArray = ((String) skillTemplate.getObject(s, 4)).split(",");
									roadmapSkillName = skillArray[skill];
									break;
								}
							}
						}
						
						core.skillService.addSkill(creature, roadmapSkillName);
						player.setProfessionWheelPosition(roadmapSkillName);
					}  catch (InstantiationException | IllegalAccessException e) {
						e.printStackTrace();
					}
					
					try {
						roadmap = ClientFileManager.loadFile("datatables/roadmap/item_rewards.iff", DatatableVisitor.class);
						
						for (int s = 0; s < roadmap.getRowCount(); s++) {
							if (roadmap.getObject(s, 0) != null) {
								if (((String) roadmap.getObject(s, 1)).equals(roadmapSkillName)) {
									String[] apts = ((String) roadmap.getObject(s, 2)).split(",");
									String[] items = ((String) roadmap.getObject(s, 4)).split(",");
									String[] wookieeItems = ((String) roadmap.getObject(s, 5)).split(",");
									String[] ithorianItems = ((String) roadmap.getObject(s, 6)).split(",");
									
									int arrayLength = items.length;
									
									if (wookieeItems.length > 0 && creature.getStfName().contains("wookiee")) {
										arrayLength = wookieeItems.length;
									} else if (ithorianItems.length > 0 && creature.getStfName().contains("ithorian")) {
										arrayLength = ithorianItems.length;
									}
									
									for (int n = 0; n < arrayLength; n++) {
										String item = items[n];
										
										if (creature.getStfName().contains("wookiee")) {
											item = wookieeItems[n];
										} else if (creature.getStfName().contains("ithorian")) {
											item = ithorianItems[n];
										}
										
										try {
											String customServerTemplate = null;
											
											if (item.contains("/")) {
												item = (item.substring(0, (item.lastIndexOf("/") + 1)) + "shared_" + item.substring((item.lastIndexOf("/") + 1)));
											} else {
												customServerTemplate = item;
												item = core.scriptService.callScript("scripts/roadmap/", player.getProfession(), "getRewards", item).asString();
											}
											
											if (item != null && item != "") {
												SWGObject itemObj = core.objectService.createObject(item, 0, creature.getPlanet(), new Point3D(0, 0, 0), new Quaternion(1, 0, 0, 0), customServerTemplate);
											} else {
												//System.out.println("Can't find template: " + item);
											}
										} catch (Exception e) {
											e.printStackTrace();
										}
									}			
								}
							}
						}				
					} catch (InstantiationException | IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			}
			
			if (luck >= 1) {
				core.skillModService.addSkillMod(creature, "luck", (int) luck);
			}
			
			if (precision >= 1) {
				core.skillModService.addSkillMod(creature, "precision", (int) precision);
			}
			
			if (strength >= 1) {
				core.skillModService.addSkillMod(creature, "strength", (int) strength);
			}
			
			if (constitution >= 1) {
				core.skillModService.addSkillMod(creature, "constitution", (int) constitution);
			}
			
			if (stamina >= 1) {
				core.skillModService.addSkillMod(creature, "stamina", (int) stamina);
			}
			
			if (agility >= 1) {
				core.skillModService.addSkillMod(creature, "agility", (int) agility);
			}
			
			if (health >= 1) {
				creature.setMaxHealth((creature.getMaxHealth() + (int) health + (healthGranted - creature.getGrantedHealth())));
				creature.setHealth(creature.getMaxHealth());
			}
			
			if (action >= 1) {
				creature.setMaxAction((creature.getMaxAction() + (int) action));
				creature.setAction(creature.getMaxAction());
			}
			
			creature.setGrantedHealth(healthGranted);
			
			creature.getClient().getSession().write((new ClientMfdStatusUpdateMessage((float) ((level >= 90) ? 90 : (level + 1)), "/GroundHUD.MFDStatus.vsp.role.targetLevel")).serialize());
			creature.setLevel((short) level);
			
			if (level == 90) {
				core.scriptService.callScript("scripts/collections/", "profession_master", player.getProfession(), core, creature);
			}
			
			creature.showFlyText(OutOfBand.ProsePackage("@cbt_spam:skill_up"), 2.5f, new RGB(154, 205, 50), 0, true);
			creature.playEffectObject("clienteffect/skill_granted.cef", "");
			creature.playMusic("sound/music_acq_bountyhunter.snd");
			
			if (creature.getGuildId() != 0) {
				Guild guild = core.guildService.getGuildById(creature.getGuildId());
				
				if (guild != null && guild.getMembers().containsKey(creature.getObjectID())) {
					guild.getMember(creature.getObjectID()).setLevel((short) level);
				}
			}
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Gives experience.  If they have enough experience, it auto-levels them up.
	 * 
	 * First, it adds the experience.
	 * Second, it sees if they need to level up.
	 * Third, if they do, it adds the relevant stat/health/action increases & expertise points.
	 * Fourth, if their roadmap increased, it adds the relevant roadmap update, roadmap items, skillmods, abilities, and profession quests.
	 * ALL of this info is contained in the client files, so we don't need to bother with scripts.
	 */
	public void giveExperience(CreatureObject creature, int experience) {
		DatatableVisitor experienceTable;
		PlayerObject player = (PlayerObject) creature.getSlottedObject("ghost");
		experience *= xpMultiplier;
		
		if (creature.getLevel() >= 90) {
			return;
		}
		
		try {
			experienceTable = ClientFileManager.loadFile("datatables/player/player_level.iff", DatatableVisitor.class);
			
			// Cannot gain more than half of the XP needed for the next level in one go
			// TODO: Do check
			
			int experienceBonus = creature.getSkillModBase("flush_with_success");
			
			experience += ((experience * experienceBonus) / 100);
			
			// 1. Add the experience.
			if (experience > 0 && !creature.isStationary()) {
				creature.showFlyText(OutOfBand.ProsePackage("@base_player:prose_flytext_xp", experience), 2.5f, new RGB(180, 60, 240), 1, true);
			}
			
			String xpType = ((player.getProfession().contains("entertainer")) ? "entertainer" : ((player.getProfession().contains("trader")) ? "crafting" : "combat_general"));
			
			if (player.getXpList().containsKey(xpType)) {
				experience += player.getXp(xpType);
			}
			
			player.setXp(xpType, experience);
			creature.setXpBarValue(experience);
			
			// 2. See if they need to level up.
			for (int i = 0; i < experienceTable.getRowCount(); i++) {
				if (experienceTable.getObject(i, 0) != null) {
					if (experience >= ((Integer) experienceTable.getObject(i, 1))) {
						if (creature.getLevel() < (Integer) experienceTable.getObject(i, 0)) {
							creature.playEffectObject("clienteffect/level_granted.cef", "");
							creature.getClient().getSession().write((new ClientMfdStatusUpdateMessage((float) ((creature.getLevel() == 90) ? 90 : (creature.getLevel() + 1)), "/GroundHUD.MFDStatus.vsp.role.targetLevel")).serialize());
							creature.setLevel(((Integer) experienceTable.getObject(i, 0)).shortValue());
							
							if (creature.getLevel() == 90) {
								core.scriptService.callScript("scripts/collections/", "profession_master", player.getProfession(), core, creature);
							}
							
							// 3. Add the relevant health/action and expertise points.
							float luck = (((((float) (core.scriptService.getMethod("scripts/roadmap/", player.getProfession(), "getLuck").__call__().asInt()) + (core.scriptService.getMethod("scripts/roadmap/", creature.getStfName(), "getLuck").__call__().asInt())) / ((float) 90)) * ((float) creature.getLevel())) - ((float) creature.getSkillModBase("luck")));
							float precision = (((((float) (core.scriptService.getMethod("scripts/roadmap/", player.getProfession(), "getPrecision").__call__().asInt()) + (core.scriptService.getMethod("scripts/roadmap/", creature.getStfName(), "getPrecision").__call__().asInt())) / ((float) 90)) * ((float) creature.getLevel())) - ((float) creature.getSkillModBase("precision")));
							float strength = (((((float) (core.scriptService.getMethod("scripts/roadmap/", player.getProfession(), "getStrength").__call__().asInt()) + (core.scriptService.getMethod("scripts/roadmap/", creature.getStfName(), "getStrength").__call__().asInt())) / ((float) 90)) * ((float) creature.getLevel())) - ((float) creature.getSkillModBase("strength")));
							float constitution = (((((float) (core.scriptService.getMethod("scripts/roadmap/", player.getProfession(), "getConstitution").__call__().asInt()) + (core.scriptService.getMethod("scripts/roadmap/", creature.getStfName(), "getConstitution").__call__().asInt())) / ((float) 90)) * ((float) creature.getLevel())) - ((float) creature.getSkillModBase("constitution")));
							float stamina = (((((float) (core.scriptService.getMethod("scripts/roadmap/", player.getProfession(), "getStamina").__call__().asInt()) + (core.scriptService.getMethod("scripts/roadmap/", creature.getStfName(), "getStamina").__call__().asInt())) / ((float) 90)) * ((float) creature.getLevel())) - ((float) creature.getSkillModBase("stamina")));
							float agility = (((((float) (core.scriptService.getMethod("scripts/roadmap/", player.getProfession(), "getAgility").__call__().asInt()) + (core.scriptService.getMethod("scripts/roadmap/", creature.getStfName(), "getAgility").__call__().asInt())) / ((float) 90)) * ((float) creature.getLevel())) - ((float) creature.getSkillModBase("agility")));
							float health = 100;
							float action = 75;
							
							int healthGranted = ((Integer) experienceTable.getObject(i, 4));
							
							if (luck >= 1) {
								core.skillModService.addSkillMod(creature, "luck", (int) luck);
							creature.sendSystemMessage(OutOfBand.ProsePackage("@spam:level_up_stat_gain_0", (int) luck), DisplayType.Broadcast);
							}
							
							if (precision >= 1) {
								core.skillModService.addSkillMod(creature, "precision", (int) precision);
								creature.sendSystemMessage(OutOfBand.ProsePackage("@spam:level_up_stat_gain_1", (int) precision), DisplayType.Broadcast);
							}
							
							if (strength >= 1) {
								core.skillModService.addSkillMod(creature, "strength", (int) strength);
								creature.sendSystemMessage(OutOfBand.ProsePackage("@spam:level_up_stat_gain_2", (int) strength), DisplayType.Broadcast);
							}
							
							if (constitution >= 1) {
								core.skillModService.addSkillMod(creature, "constitution", (int) constitution);
								creature.sendSystemMessage(OutOfBand.ProsePackage("@spam:level_up_stat_gain_3", (int) constitution), DisplayType.Broadcast);
							}
							
							if (stamina >= 1) {
								core.skillModService.addSkillMod(creature, "stamina", (int) stamina);
								creature.sendSystemMessage(OutOfBand.ProsePackage("@spam:level_up_stat_gain_4", (int) stamina), DisplayType.Broadcast);
							}
							
							if (agility >= 1) {
								core.skillModService.addSkillMod(creature, "agility", (int) agility);
								creature.sendSystemMessage(OutOfBand.ProsePackage("@spam:level_up_stat_gain_5", (int) agility), DisplayType.Broadcast);
							}
							
							if (health >= 1) {
								creature.setMaxHealth((creature.getMaxHealth() + (int) health + (healthGranted - creature.getGrantedHealth())));
								creature.setHealth(creature.getMaxHealth());
								creature.sendSystemMessage(OutOfBand.ProsePackage("@spam:level_up_stat_gain_6", (((int) health) + (((int) constitution) * 8) + (((int) stamina) * 2))), DisplayType.Broadcast);
							}
								
							if (action >= 1) {
								creature.setMaxAction((creature.getMaxAction() + (int) action));
								creature.setAction(creature.getMaxAction());
								creature.sendSystemMessage(OutOfBand.ProsePackage("@spam:level_up_stat_gain_7", (((int) action) + (((int) stamina) * 8) + (((int) constitution) * 2))), DisplayType.Broadcast);
							}
							
							creature.setGrantedHealth(((Integer) experienceTable.getObject(i, 4)));
							// -> Expertise point added automatically by client
							creature.showFlyText(OutOfBand.ProsePackage("@cbt_spam:level_up"), 2.5f, new RGB(100, 149, 237), 0, true);
							
							// 4. Adds roadmap rewards
							int level = creature.getLevel();
							
							if ((level == 4 || level == 7 || level == 10) || ((level > 10) && (((creature.getLevel() - 10)  % 4) == 0))) {
								int skill = ((level <= 10) ? ((level - 1) / 3) : ((((level - 10) / 4)) + 3));
								String roadmapSkillName = "";
								DatatableVisitor skillTemplate, roadmap;
								
								try {
									skillTemplate = ClientFileManager.loadFile("datatables/skill_template/skill_template.iff", DatatableVisitor.class);
									
									for (int s = 0; s < skillTemplate.getRowCount(); s++) {
										if (skillTemplate.getObject(s, 0) != null) {
											if (((String) skillTemplate.getObject(s, 0)).equals(player.getProfession())) {
												String[] skillArray = ((String) skillTemplate.getObject(s, 4)).split(",");
												roadmapSkillName = skillArray[skill];
												break;
											}
										}
									}
									
									creature.showFlyText(OutOfBand.ProsePackage("@cbt_spam:skill_up"), 2.5f, new RGB(154, 205, 50), 0, true);
									creature.playEffectObject("clienteffect/skill_granted.cef", "");
									creature.playMusic("sound/music_acq_bountyhunter.snd");
									core.skillService.addSkill(creature, roadmapSkillName);
									player.setProfessionWheelPosition(roadmapSkillName);
								}  catch (InstantiationException | IllegalAccessException e) {
									e.printStackTrace();
								}
								
								try {
									roadmap = ClientFileManager.loadFile("datatables/roadmap/item_rewards.iff", DatatableVisitor.class);
									
									Vector<SWGObject> rewards = new Vector<SWGObject>();
									
									for (int s = 0; s < roadmap.getRowCount(); s++) {
										if (roadmap.getObject(s, 0) != null) {
											if (((String) roadmap.getObject(s, 1)).equals(roadmapSkillName)) {
												String[] apts = ((String) roadmap.getObject(s, 2)).split(",");
												String[] items = ((String) roadmap.getObject(s, 4)).split(",");
												String[] wookieeItems = ((String) roadmap.getObject(s, 5)).split(",");
												String[] ithorianItems = ((String) roadmap.getObject(s, 6)).split(",");
												
												int arrayLength = items.length;
												
												if (wookieeItems.length > 0 && creature.getStfName().contains("wookiee")) {
													arrayLength = wookieeItems.length;
												} else if (ithorianItems.length > 0 && creature.getStfName().contains("ithorian")) {
													arrayLength = ithorianItems.length;
												}
												
												for (int n = 0; n < arrayLength; n++) {
													String item = items[n];
													
													if (creature.getStfName().contains("wookiee")) {
														item = wookieeItems[n];
													} else if (creature.getStfName().contains("ithorian")) {
														item = ithorianItems[n];
													}
													
													try {
														String customServerTemplate = null;
														
														if (item.contains("/")) {
															item = (item.substring(0, (item.lastIndexOf("/") + 1)) + "shared_" + item.substring((item.lastIndexOf("/") + 1)));
														} else {
															customServerTemplate = item;
															item = core.scriptService.callScript("scripts/roadmap/", player.getProfession(), "getRewards", item).asString();
														}
														
													if (item != null && item != "") {
														SWGObject itemObj = core.objectService.createObject(item, 0, creature.getPlanet(), new Point3D(0, 0, 0), new Quaternion(1, 0, 0, 0), customServerTemplate);
													} else {
															//System.out.println("Can't find template: " + item);
														}
													} catch (Exception e) {
														e.printStackTrace();
													}
												}
											}
										}
									}
									
									if (rewards != null && !rewards.isEmpty()) {
										giveItems(creature, rewards);
									}
									
								}  catch (InstantiationException | IllegalAccessException e) {
									e.printStackTrace();
								}
							}
						}
					}	
				}
			}
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	public void addPlayerTitle(PlayerObject player, String title) {
		
		if (player.getTitleList().contains(title))
			return;
		
		player.getTitleList().add(title);

	}
	
	public void removePlayerTitle(PlayerObject player, String title) {
		if (player == null || title == "")
			return;
		
		if (player.getTitleList().contains(title))
			player.getTitleList().remove(title);

	}
	
	/**
	 * Creates a blue path to the destination point.
	 * @param actor Player that will be seeing the blue path.
	 * @param destination Where the blue path will lead to.
	 */
	public void createClientPath(SWGObject actor, Point3D destination) {
		
		if (actor == null || actor.getClient() == null || actor.getClient().getSession() == null)
			return;
		
		List<Point3D> coordinates = new ArrayList<Point3D>();
		coordinates.add(actor.getPosition());
		
		// TODO: Generate a path to destination based off of objects in the world.
		
		coordinates.add(destination); // Destination MUST be last coordinate in array
		
		CreateClientPathMessage path = new CreateClientPathMessage(coordinates);
		actor.getClient().getSession().write(path.serialize());
	}
	
	/**
	 * Gives a player an item and shows the "New Items" message.
	 * @param reciever Player receiving the item.
	 * @param item The object to be given.
	 * @author Waverunner
	 */
	public void giveItem(CreatureObject reciever, SWGObject item) {
		if (reciever == null || item == null)
			return;
		
		if (reciever.getClient() == null)
			return;
		Client client = reciever.getClient();
		
		if (client.getSession() == null)
			return;
		SWGObject inventory = reciever.getSlottedObject("inventory");
		
		if (inventory == null)
			return;
		
		inventory.add(item);
		
		ObjControllerMessage objController = new ObjControllerMessage(11, new ShowLootBox(reciever.getObjectID(), item));
		client.getSession().write(objController.serialize());
	}
	
	/**
	 * Gives a player a variety of items and shows the "New Items" message.
	 * @param reciever Player receiving the items.
	 * @param items Vector of the items.
	 */
	public void giveItems(CreatureObject reciever, Vector<SWGObject> items) {
		if (reciever == null || items == null)
			return;
		
		if (reciever.getClient() == null)
			return;
		Client client = reciever.getClient();
		
		if (client.getSession() == null)
			return;
		SWGObject inventory = reciever.getSlottedObject("inventory");
		
		if (inventory == null)
			return;
		
		for (SWGObject obj : items) {
			inventory.add(obj);
		}
		
		ObjControllerMessage objController = new ObjControllerMessage(11, new ShowLootBox(reciever.getObjectID(), items));
		client.getSession().write(objController.serialize());
	}
	
	public void performUnity(final CreatureObject acceptor, final CreatureObject proposer){
		TangibleObject acceptorInventory = (TangibleObject) acceptor.getSlottedObject("inventory");
		final AtomicBoolean acceptorHasRing = new AtomicBoolean();

		acceptorInventory.viewChildren(acceptor, false, false, new Traverser() {

			@Override
			public void process(SWGObject obj) {
				if(obj.getAttachment("objType") != null) {
					String objType = (String) obj.getAttachment("objType");
					if(objType == "ring") {
						acceptorHasRing.set(true);
					}
				}
			}
		});

		if(acceptorHasRing.get() == false) {
			acceptor.sendSystemMessage("@unity:no_ring", (byte) 0);
			proposer.sendSystemMessage("@unity:accept_fail", (byte) 0);
			acceptor.setAttachment("proposer", null);
		} else {
			PlayerObject aGhost = (PlayerObject) acceptor.getSlottedObject("ghost");
			PlayerObject pGhost = (PlayerObject) proposer.getSlottedObject("ghost");

			if (aGhost == null || pGhost == null) {
				acceptor.sendSystemMessage("@unity:wed_error", (byte) 0);
				proposer.sendSystemMessage("@unity:wed_error", (byte) 0);
				acceptor.setAttachment("proposer", null);
				return;
			} else {
				final Vector<SWGObject> ringList = new Vector<SWGObject>();
				acceptorInventory.viewChildren(acceptor, false, false, new Traverser() {

					@Override
					public void process(SWGObject obj) {
						if (obj.getAttachment("objType") != null) {
							if (obj.getAttachment("objType") == "ring") {
								ringList.add(obj);
							}
						}
					}
				});

				if (ringList.size() > 1) {
					sendRingSelectWindow(acceptor, proposer, ringList);
				} else {
					// Proposer's ring is already 'unified' from the start, so no
					// need to set a unity attachment.
					ringList.get(0).setAttachment("unity", (Boolean) true);

					if(!acceptor.getEquipmentList().contains(ringList.get(0).getObjectId()))
						core.equipmentService.equip(acceptor, ringList.get(0));

					aGhost.setSpouseName(proposer.getCustomName());
					pGhost.setSpouseName(acceptor.getCustomName());

					acceptor.sendSystemMessage("Your union with " + proposer.getCustomName() + " is complete.", (byte) 0);
					proposer.sendSystemMessage("Your union with " + acceptor.getCustomName() + " is complete.", (byte) 0);

					acceptor.setAttachment("proposer", null);
				}
			}
		}
	}

	private void sendRingSelectWindow(final CreatureObject actor, final CreatureObject proposer, Vector<SWGObject> ringList) {
		Map<Long, String> ringData = new HashMap<Long, String>();

		for(SWGObject obj : ringList) {
			ringData.put(obj.getObjectId(), obj.getCustomName());
		}

		final SUIWindow ringWindow = core.suiService.createListBox(ListBoxType.LIST_BOX_OK_CANCEL, "@unity:ring_prompt", "@unity:ring_prompt", ringData, actor, proposer, (float) 15);
		Vector<String> returnList = new Vector<String>();
		returnList.add("List.lstList:SelectedRow");

		ringWindow.addHandler(0, "", Trigger.TRIGGER_OK, returnList, new SUICallback() {

			@Override
			public void process(SWGObject owner, int eventType, Vector<String> returnList) {
				int index = Integer.parseInt(returnList.get(0));

				SWGObject selectedRing = core.objectService.getObject(ringWindow.getObjectIdByIndex(index));
				selectedRing.setAttachment("unity", (Boolean) true);

				if(!actor.getEquipmentList().contains(selectedRing.getObjectId()))
					core.equipmentService.equip(actor, selectedRing);

				PlayerObject aGhost = (PlayerObject) actor.getSlottedObject("ghost");
				PlayerObject pGhost = (PlayerObject) proposer.getSlottedObject("ghost");
				aGhost.setSpouseName(proposer.getCustomName());
				pGhost.setSpouseName(actor.getCustomName());

				actor.sendSystemMessage("Your union with " + proposer.getCustomName() + " is complete.", (byte) 0);
				proposer.sendSystemMessage("Your union with " + actor.getCustomName() + " is complete.", (byte) 0);
			}

		});

		ringWindow.addHandler(1, "", Trigger.TRIGGER_CANCEL, returnList, new SUICallback() {

			@Override
			public void process(SWGObject owner, int eventType, Vector<String> returnList) {

				PlayerObject aGhost = (PlayerObject) actor.getSlottedObject("ghost");
				PlayerObject pGhost = (PlayerObject) proposer.getSlottedObject("ghost");

				actor.sendSystemMessage("@unity:decline", (byte) 0);
				proposer.sendSystemMessage("@unity:declined", (byte) 0);
				actor.setAttachment("proposer", null);
				for(Long objId : proposer.getEquipmentList()) {
					SWGObject obj = core.objectService.getObject(objId);
					if(obj != null && obj.getAttachment("unity") != null) {
						obj.setAttachment("unity", null);
						break;
					}
				}
			}
		});
		core.suiService.openSUIWindow(ringWindow);
	}
	
	public void sendSetBountyWindow(final CreatureObject victim, final CreatureObject attacker) {
		SUIWindow bountyWindow = core.suiService.createInputBox(InputBoxType.INPUT_BOX_OK_CANCEL, "@bounty_hunter:setbounty_title", "@bounty_hunter:setbounty_prompt1 " + attacker.getCustomName() + "?" + "\n@bounty_hunter:setbounty_prompt2 " 
				+ String.valueOf(victim.getBankCredits() + victim.getCashCredits()), victim, null, (float) 10, new SUICallback() {

			@Override
			public void process(SWGObject owner, int eventType, Vector<String> returnList) {
				if (eventType == 0 && returnList.size() > 0 && returnList.get(0) != null) {
					int bounty = Integer.parseInt(returnList.get(0));
					int totalFunds = victim.getBankCredits() + victim.getCashCredits();
					
					if (bounty > totalFunds) {
						victim.sendSystemMessage("@bounty_hunter:setbounty_too_much", DisplayType.Broadcast);
						sendSetBountyWindow(victim, attacker);
						return;
					}
					
					if (bounty < 20000) {
						victim.sendSystemMessage("@bounty_hunter:setbounty_too_little", DisplayType.Broadcast);
						sendSetBountyWindow(victim, attacker);
						return;
					} else if (bounty > 1000000) {
						victim.sendSystemMessage("@bounty_hunter:setbounty_cap", DisplayType.Broadcast);
						bounty = 1000000;
					}
					
					if (core.getBountiesODB().contains(attacker.getObjectID())) {
						if (((BountyListItem) core.getBountiesODB().get(attacker.getObjectID())).getCreditReward() >= 20000000) {
							victim.sendSystemMessage("@bounty_hunter:max_bounty", DisplayType.Broadcast);
							return;
						}
					}
					
					// Try removing bounty amount from the bank first then cash. Remove amount accordingly if bank/cash is less than placed bounty.
					if (bounty > victim.getBankCredits()) {
						int difference = bounty - victim.getBankCredits();
						
						victim.setCashCredits(victim.getCashCredits() - difference);
						victim.setBankCredits(0);
					} else if (bounty > victim.getCashCredits()) {
						int difference = bounty - victim.getCashCredits();
						
						victim.setBankCredits(victim.getBankCredits() - difference);
						victim.setCashCredits(0);
					} else { victim.setBankCredits(victim.getBankCredits() - bounty); }
					
					if (!core.missionService.addToExistingBounty(attacker.getObjectID(), victim.getObjectID(), bounty))
						core.missionService.createNewBounty(attacker, victim.getObjectID(), bounty);
					
					victim.sendSystemMessage("You have placed a bounty for " + bounty + " credits on the head of " + attacker.getCustomName(), (byte) 0);
				}
			}
			
		});
		bountyWindow.setProperty("txtInput:NumericInteger", "true");
		bountyWindow.setProperty("txtInput:MaxLength", "7");
		bountyWindow.setProperty("inputBox:Size", "306,306");
		core.suiService.openSUIWindow(bountyWindow);
	}
	
	public String getFormalProfessionName(String template) {
		String formalName = "";

		switch (template) {
		case "force_sensitive_1a":	formalName = "Jedi"; break;
		case "bounty_hunter_1a":	formalName = "Bounty Hunter"; break;
		case "officer_1a":			formalName = "Officer"; break;
		case "smuggler_1a":			formalName = "Smuggler"; break;
		case "entertainer_1a":		formalName = "Entertainer"; break;
		case "spy_1a":				formalName = "Spy"; break;
		case "medic_1a":			formalName = "Medic"; break;
		case "commando_1a":			formalName = "Commando"; break;
		
		default:					formalName = "Trader"; break;	// Ziggy: Trader profession names are a bit irregular, so this is used.

		}
		return formalName;
	}
	
	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
		
	}
	
	public Map<Long, List<ScheduledFuture<?>>> getSchedulers() {
		return schedulers;
	}
	
}
