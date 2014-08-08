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

public class LairTemplate {
	
	private String name;
	private String lairCRC;
	private String mobileName;
	private Vector<String> mobiles;
	private int mobileLimit;
	
	public LairTemplate(String name, String mobile, int mobileLimit, String lairCRC) {
		this.name = name;
		this.mobileName = mobile;
		this.mobileLimit = mobileLimit;
		this.lairCRC = lairCRC;
	}
	
	public LairTemplate(String name, Vector<String> mobiles, int mobileLimit, String lairCRC) {
		this.name = name;
		this.mobiles = mobiles;
		this.mobileLimit = mobileLimit;
		this.lairCRC = lairCRC;
	}
	
	public String getLairCRC() {
		return lairCRC;
	}

	public void setLairCRC(String lairCRC) {
		this.lairCRC = lairCRC;
	}

	public String getMobileName() {
		return mobileName;
	}

	public void setMobileName(String mobileName) {
		this.mobileName = mobileName;
	}

	public int getMobileLimit() {
		return mobileLimit;
	}

	public void setMobileLimit(int mobileLimit) {
		this.mobileLimit = mobileLimit;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Vector<String> getMobiles() {
		return mobiles;
	}

	public void setMobiles(Vector<String> mobiles) {
		this.mobiles = mobiles;
	}

}
