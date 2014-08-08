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

import org.apache.mina.core.buffer.IoBuffer;

import resources.common.Opcodes;


public class ErrorMessage extends SWGMessage {

	private String errorType;
	private String errorMessage;
	
	public ErrorMessage(String errorType, String errorMessage) {
		this.errorType = errorType;
		this.errorMessage = errorMessage;
	}
	
	public void deserialize(IoBuffer data) {
		
	}
	
	public IoBuffer serialize() {
		IoBuffer result = IoBuffer.allocate(11 + errorType.length() + errorMessage.length());
		
		result.putShort((short)3);
		result.putInt(Opcodes.ErrorMessage);
		result.put(getAsciiString(errorType));
		result.put(getAsciiString(errorMessage));
		result.put((byte)0);
		
		return result.flip();
	}
}
