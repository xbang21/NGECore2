import sys
from services.spawn import MobileTemplate
from services.spawn import WeaponTemplate
from resources.datatables import WeaponType
from resources.datatables import Difficulty
from resources.datatables import Options
from java.util import Vector


def addTemplate(core):
	mobileTemplate = MobileTemplate()
	
	mobileTemplate.setCreatureName('frightened_borgle')
	mobileTemplate.setLevel(34)
	mobileTemplate.setDifficulty(Difficulty.NORMAL)

	mobileTemplate.setMinSpawnDistance(4)
	mobileTemplate.setMaxSpawnDistance(8)
	mobileTemplate.setDeathblow(False)
	mobileTemplate.setScale(1)
	mobileTemplate.setMeatType("Carnivore Meat")
	mobileTemplate.setMeatAmount(5)
	mobileTemplate.setHideType("Leathery Hide")
	mobileTemplate.setHideAmount(5)
	mobileTemplate.setBoneType("Avian Bones")
	mobileTemplate.setBoneAmount(5)
	mobileTemplate.setSocialGroup("borgle")
	mobileTemplate.setAssistRange(6)
	mobileTemplate.setStalker(False)
	mobileTemplate.setOptionsBitmask(Options.AGGRESSIVE | Options.ATTACKABLE)
	
	templates = Vector()
	templates.add('object/mobile/shared_borgle.iff')
	mobileTemplate.setTemplates(templates)
	
	weaponTemplates = Vector()
	weaponTemplate = WeaponTemplate('object/weapon/melee/unarmed/shared_unarmed_default.iff', WeaponType.UNARMED, 1.0, 6, 'kinetic')
	weaponTemplates.add(weaponTemplate)
	mobileTemplate.setWeaponTemplateVector(weaponTemplates)
	
	attacks = Vector()
	attacks.add('bm_puncture_1')
	attacks.add('bm_siphon_1')
	attacks.add('bm_wing_buffet_3')
	mobileTemplate.setDefaultAttack('creatureMeleeAttack')
	mobileTemplate.setAttacks(attacks)
	
	core.spawnService.addMobileTemplate('frightened_borgle', mobileTemplate)
	return