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
package protocol.swg;

import java.nio.ByteOrder;

import main.NGECore;

import org.apache.mina.core.buffer.IoBuffer;

import services.travel.TravelPoint;
import engine.resources.objects.SWGObject;

@SuppressWarnings("unused")
public class EnterTicketPurchaseModeMessage extends SWGMessage {

	private String planetName;
	private String cityName;
	private SWGObject player;
	private boolean isItv = false;
	
	public EnterTicketPurchaseModeMessage(String planetName, String cityName, SWGObject player, boolean isItv) {
		this.planetName = planetName;
		this.cityName = cityName;
		this.player = player;
		this.isItv = true;
	}
	
	public EnterTicketPurchaseModeMessage(String planetName, String cityName, SWGObject player) {
		this.planetName = planetName;
		this.cityName = cityName;
		this.player = player;
	}
	
	@Override
	public void deserialize(IoBuffer data) {

	}

	@Override
	public IoBuffer serialize() {
		final NGECore core = NGECore.getInstance();
		
		TravelPoint nearestPoint = null;
		if (!isItv) nearestPoint = core.travelService.getNearestTravelPoint(player);
		else nearestPoint = core.travelService.getNearestTravelPoint(player, 5120);
		
		IoBuffer result = IoBuffer.allocate(11 + planetName.length() + nearestPoint.getName().length()).order(ByteOrder.LITTLE_ENDIAN);
		
		result.putShort((short) 3);
		result.putInt(0x904DAE1A);
		
		result.put(getAsciiString(planetName));
		result.put(getAsciiString(nearestPoint.getName()));
		
		
		result.put((byte) 0);
		
		return result.flip();
	}

}
