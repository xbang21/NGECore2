import sys
from services.equipment import BonusSetTemplate
from java.util import Vector

def addBonusSet(core):
	bonusSet = BonusSetTemplate("set_bonus_jedi_robe")
	
	# Waistpack
	bonusSet.addRequiredItem("object/tangible/wearables/backpack/shared_fannypack_s01.iff")
	
	# Jedi Robes - Light
	bonusSet.addRequiredItem("object/tangible/wearables/robe/shared_robe_jedi_light_s01.iff")
	bonusSet.addRequiredItem("object/tangible/wearables/robe/shared_robe_jedi_light_s02.iff")
	bonusSet.addRequiredItem("object/tangible/wearables/robe/shared_robe_jedi_light_s03.iff")
	bonusSet.addRequiredItem("object/tangible/wearables/robe/shared_robe_jedi_light_s04.iff")
	bonusSet.addRequiredItem("object/tangible/wearables/robe/shared_robe_jedi_light_s05.iff")
	
	# Jedi Robes - Dark
	bonusSet.addRequiredItem("object/tangible/wearables/robe/shared_robe_jedi_dark_s01.iff")
	bonusSet.addRequiredItem("object/tangible/wearables/robe/shared_robe_jedi_dark_s02.iff")
	bonusSet.addRequiredItem("object/tangible/wearables/robe/shared_robe_jedi_dark_s03.iff")
	bonusSet.addRequiredItem("object/tangible/wearables/robe/shared_robe_jedi_dark_s04.iff")
	bonusSet.addRequiredItem("object/tangible/wearables/robe/shared_robe_jedi_dark_s05.iff")
	
	# Jedi Cloaks
	bonusSet.addRequiredItem("object/tangible/wearables/robe/shared_robe_s32.iff")
	bonusSet.addRequiredItem("object/tangible/wearables/robe/shared_robe_s32_h1.iff")
	bonusSet.addRequiredItem("object/tangible/wearables/robe/shared_robe_s33.iff")
	bonusSet.addRequiredItem("object/tangible/wearables/robe/shared_robe_s33_h1.iff")
	bonusSet.addRequiredItem("item_jedi_robe_06_01")

	
	core.equipmentService.addBonusSetTemplate(bonusSet)
	
def handleChange(core, creature, set):
	wornItems = set.getWornTemplateCount(creature)
	
	if wornItems == 2:
		core.buffService.addBuffToCreature(creature, "set_bonus_jedi_robe_1", creature)
		creature.sendSystemMessage('@set_bonus:set_bonus_jedi_robe_1_sys', 0)
	else:
		core.buffService.removeBuffFromCreatureByName(creature, "set_bonus_jedi_robe_1")