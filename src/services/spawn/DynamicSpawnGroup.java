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
package services.spawn;

import java.util.Vector;

public class DynamicSpawnGroup extends SpawnGroup {

	private Vector<String> mobiles;
	private String name;
	private int maxSpawns;
	private int minSpawnDistance;
	private int groupMembersNumber = -1;

	public Vector<String> getMobiles() {
		return mobiles;
	}

	public void setMobiles(Vector<String> mobiles) {
		this.mobiles = mobiles;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getMaxSpawns() {
		return maxSpawns;
	}

	public void setMaxSpawns(int maxSpawns) {
		this.maxSpawns = maxSpawns;
	}

	public int getMinSpawnDistance() {
		return minSpawnDistance;
	}

	public void setMinSpawnDistance(int minSpawnDistance) {
		this.minSpawnDistance = minSpawnDistance;
	}

	public int getGroupMembersNumber() {
		return groupMembersNumber;
	}

	public void setGroupMembersNumber(int groupMembersNumber) {
		this.groupMembersNumber = groupMembersNumber;
	}
	
}
