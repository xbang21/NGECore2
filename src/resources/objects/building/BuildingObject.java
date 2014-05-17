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
package resources.objects.building;

import java.io.Serializable;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.core.buffer.IoBuffer;

import main.NGECore;
import resources.common.OutOfBand;
import resources.datatables.DisplayType;
import resources.datatables.Options;
import resources.objects.ObjectMessageBuilder;
import resources.objects.cell.CellObject;
import resources.objects.creature.CreatureObject;
import resources.objects.tangible.TangibleObject;
import engine.clientdata.ClientFileManager;
import engine.clientdata.visitors.PortalVisitor;
import engine.clients.Client;
import engine.resources.objects.Baseline;
import engine.resources.objects.IPersistent;
import engine.resources.scene.Planet;
import engine.resources.scene.Point3D;
import engine.resources.scene.Quaternion;

public class BuildingObject extends TangibleObject implements IPersistent, Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private transient BuildingMessageBuilder messageBuilder;
	
	private Vector<Long> entryList = new Vector<Long>();
	private Vector<Long> banList = new Vector<Long>();
	
	public static final byte PRIVATE = (byte) 0;
	public static final byte PUBLIC = (byte) 1;
	
	public BuildingObject(long objectID, Planet planet, Point3D position, Quaternion orientation, String Template) {
		super(objectID, planet, position, orientation, Template);
		getBaseline(3).set("volume", 255);
		setOptionsBitmask(Options.INVULNERABLE);
		setConditionDamage(100);
		setMaximumCondition(4320);
		setStaticObject(true);
	}
	
	public BuildingObject() {
		super();
		getBaseline(3).set("volume", 255);
		setOptionsBitmask(Options.INVULNERABLE);
		setConditionDamage(100);
		setMaximumCondition(4320);
		setStaticObject(true);
	}
	
	@Override
	public void initAfterDBLoad() {
		super.init();
		defendersList = new Vector<TangibleObject>();
		getBaseline(3).set("volume", 255);
		setOptionsBitmask(Options.INVULNERABLE);
		setConditionDamage(100);
		setMaximumCondition(4320);
	}
	
	@Override
	public Baseline getOtherVariables() {
		Baseline baseline = super.getOtherVariables();
		baseline.put("maintainanceAmount", (float) 0);
		baseline.put("baseMaintenanceRate", 0);
		baseline.put("deedTemplate", "");
		baseline.put("residency", false);
		baseline.put("privacy", (byte) 0);
		baseline.put("maximumStorageCapacity", (short) 0);
		return baseline;
	}
	
	@Override
	public Baseline getBaseline3() {
		Baseline baseline = super.getBaseline3();
		return baseline;
	}
	
	@Override
	public Baseline getBaseline6() {
		Baseline baseline = super.getBaseline6();
		return baseline;
	}
	
	@Override
	public Baseline getBaseline8() {
		Baseline baseline = super.getBaseline8();
		return baseline;
	}
	
	@Override
	public Baseline getBaseline9() {
		Baseline baseline = super.getBaseline9();
		return baseline;
	}
	
	public Vector<CellObject> getCells() {
		final Vector<CellObject> cells = new Vector<CellObject>();
		
		this.viewChildren(this, true, false, (obj) -> {
			if (obj instanceof CellObject) {
				cells.add((CellObject) obj);
			}
		});
		
		return cells;
	}
	
	public CellObject getCellByCellNumber(final int cellNumber) {
		final AtomicReference<CellObject> ref = new AtomicReference<CellObject>();
		
		synchronized(objectMutex) {
			this.viewChildren(this, true, false, (obj) -> {
				if (obj instanceof CellObject && ((CellObject) obj).getCellNumber() == cellNumber) {
					ref.set((CellObject) obj);
				}
			});
		}
		
		return ref.get();
	}
	
	public CellObject getCellByCellName(String cellName) {
		Map<String, Object> attributes = getTemplateData().getAttributes();
		
		if (attributes.containsKey("portalLayoutFilename") && ((String) attributes.get("portalLayoutFilename")).length() > 0) {
			String portalLayoutFilename = (String) attributes.get("portalLayoutFilename");
			
			try {
				PortalVisitor portal = ClientFileManager.loadFile(portalLayoutFilename, PortalVisitor.class);
				
				for (int i = 1; i <= portal.cellCount; i++) {
					if (cellName.equals(portal.cells.get(i).name)) {
						return getCellByCellNumber(i);
					}
				}
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
	public int getCellNumberByObjectId(long objectId) {
		Vector<CellObject> cells = getCells();
		
		for (CellObject cell : cells) {
			if (cell.getObjectID() == objectId) {
				return cell.getCellNumber();
			}
		}
		
		return 0;
	}
	
	public float getMaintenanceAmount() {
		return (float) otherVariables.get("maintenanceAmount");
	}
	
	public void setMaintenanceAmount(float maintenanceAmount) {
		otherVariables.set("maintenanceAmount", maintenanceAmount);
	}
	
	public int getBMR() {
		return (int) otherVariables.get("baseMaintenanceRate");
	}
	
	public void setBMR(int BMR) {
		otherVariables.set("baseMaintenanceRate", BMR);
	}
	
	public String getDeedTemplate(){
		return (String) otherVariables.get("deedTemplate");
	}
	
	public void setDeedTemplate(String deedTemplate){
		otherVariables.set("deedTemplate", deedTemplate);
	}
	
	public String getBuildingName() {
		return getCustomName();
	}
	
	public void setBuildingName(String buildingName, CreatureObject owner) {
		setCustomName(buildingName);
		((CreatureObject) owner).sendSystemMessage("Structure renamed.", DisplayType.Broadcast);
	}
	
	public boolean getResidency(){
		return (boolean) otherVariables.get("residency");
	}
	
	public void setResidency(CreatureObject owner){
		owner.sendSystemMessage("@player_structure:declared_residency", (byte) 1);
		otherVariables.set("residency", true);
	}
	
	public byte getPrivacy() {
		return (byte) otherVariables.get("privacy");
	}
	
	public String getPrivacyString() {
		switch (getPrivacy()) {
			case PRIVATE:
				return "private";
			case PUBLIC:
				return "public";
			default:
				return "42";
		}
	}
	
	public void setPrivacy(byte privacy) {
		otherVariables.set("residency", privacy);
	}
	
	public Vector<TangibleObject> getItemsList() {
		Vector<TangibleObject> items = new Vector<TangibleObject>();
		
		getCells().forEach(c -> c.viewChildren(c, true, false, (item) -> {
			if(!(item instanceof CreatureObject) && item.getTemplate() != "object/tangible/terminal/shared_terminal_player_structure.iff"
			&& item.getTemplate() != "object/tangible/terminal/shared_terminal_city.iff"
			&& item.getTemplate() != "object/tangible/terminal/shared_terminal_city_vote.iff") {
				items.add((TangibleObject) item);
			}
		}));
		
		return items;
	}
	
	public short getMaximumStorageCapacity() {
		return (byte) otherVariables.get("maximumStorageCapacity");
	}
	
	public void setMaximumStorageCapacity(short maximumStorageCapacity) {
		otherVariables.set("maximumStorageCapacity", maximumStorageCapacity);
	}
	
	public void setPermissionEntry(String name, CreatureObject owner){
		Vector<String> entryListFirstNames = new Vector<String>();
		
		for (long oid : entryList) {
			String firstName = NGECore.getInstance().characterService.getPlayerFirstName(oid);
			entryListFirstNames.add(firstName);
		}
		
		entryListFirstNames.add("Peter");
		entryListFirstNames.add("Jackson");
		
		owner.getClient().getSession().write(messageBuilder.buildPermissionListCreate(entryListFirstNames, name));      				
	}
	
	public void setPermissionBan(String name, CreatureObject owner){
		Vector<String> banListFirstNames = new Vector<String>();
		
		for (long oid : banList) {
			String firstName = NGECore.getInstance().characterService.getPlayerFirstName(oid);
			banListFirstNames.add(firstName);
		}
		
		banListFirstNames.add("Peter");
		banListFirstNames.add("Smith");
		
		owner.getClient().getSession().write(messageBuilder.buildPermissionListCreate(banListFirstNames, name));      				
	}
	
	public void addPlayerToEntryList(CreatureObject owner, long oid, String firstName){
		if (!entryList.contains(oid)){
			entryList.add(oid);	
			owner.sendSystemMessage(OutOfBand.ProsePackage("@player_structure:player_removed", "TO", NGECore.getInstance().objectService.getObject(oid).getCustomName()), DisplayType.Screen);
		}
	}
	
	public void removePlayerFromEntryList(CreatureObject owner, long oid, String firstName){
		if (entryList.contains(oid)){
			entryList.remove(oid);
			owner.sendSystemMessage(OutOfBand.ProsePackage("@player_structure:player_removed", "TO", NGECore.getInstance().objectService.getObject(oid).getCustomName()), DisplayType.Screen);
		}
	}
	
	public void addPlayerToBanList(CreatureObject owner, long oid, String firstName){
		if (!banList.contains(oid)){
			banList.add(oid);	
			owner.sendSystemMessage(OutOfBand.ProsePackage("@player_structure:player_removed", "TO", NGECore.getInstance().objectService.getObject(oid).getCustomName()), DisplayType.Screen);
		}
	}
	
	public void removePlayerFromBanList(CreatureObject owner, long oid, String firstName){
		if (banList.contains(oid)){
			banList.remove(oid);
			owner.sendSystemMessage(OutOfBand.ProsePackage("@player_structure:player_removed", "TO", NGECore.getInstance().objectService.getObject(oid).getCustomName()), DisplayType.Screen);
		}
	}
	
	@Override
	public void notifyClients(IoBuffer buffer, boolean notifySelf) {
		notifyObservers(buffer, false);
	}
	
	@Override
	public ObjectMessageBuilder getMessageBuilder() {
		synchronized(objectMutex) {
			if (messageBuilder == null) {
				messageBuilder = new BuildingMessageBuilder(this);
			}
			
			return messageBuilder;
		}
	}
	
	@Override
	public void sendBaselines(Client destination) {
		if (destination != null && destination.getSession() != null) {
			destination.getSession().write(getBaseline(3).getBaseline());
			destination.getSession().write(getBaseline(6).getBaseline());
			
			Client parent = ((getGrandparent() == null) ? null : getGrandparent().getClient());
			
			if (parent != null && destination == parent) {
				destination.getSession().write(getBaseline(8).getBaseline());
				destination.getSession().write(getBaseline(9).getBaseline());
			}
		}
	}
	
	public void sendListDelta(byte viewType, short updateType, IoBuffer buffer) {
		super.sendListDelta(viewType, updateType, buffer);
	}
	
}
