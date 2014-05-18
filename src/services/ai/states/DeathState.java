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
package services.ai.states;

import main.NGECore;
import resources.objects.creature.CreatureObject;
import resources.objects.tangible.TangibleObject;
import services.ai.AIActor;

public class DeathState extends AIState {

	@Override
	public byte onEnter(AIActor actor) {
		NGECore.getInstance().aiService.awardExperience(actor);
		NGECore.getInstance().aiService.awardGcw(actor);
		actor.getCreature().setAttachment("radial_filename", "npc/corpse");
		//NGECore.getInstance().scriptService.callScript("scripts/radial/npc/corpse", "", "createRadial", NGECore.getInstance(), actor.getCreature().getKiller(), actor.getCreature(), new Vector<RadialOptions>());
		NGECore.getInstance().lootService.DropLoot((CreatureObject)(actor.getCreature().getKiller()),(TangibleObject)(actor.getCreature()));			
		actor.scheduleDespawn();
		return 0;
	}

	@Override
	public byte onExit(AIActor actor) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public byte move(AIActor actor) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public byte recover(AIActor actor) {
		// TODO Auto-generated method stub
		return 0;
	}

}
