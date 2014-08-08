import sys
from services.spawn import MobileTemplate
from services.spawn import WeaponTemplate
from resources.datatables import WeaponType
from resources.datatables import Difficulty
from resources.datatables import Options
from java.util import Vector


def addTemplate(core):
	mobileTemplate = MobileTemplate()
	
	mobileTemplate.setCreatureName('young_borgle_gruntling')
	mobileTemplate.setLevel(30)
	mobileTemplate.setDifficulty(Difficulty.NORMAL)

	mobileTemplate.setMinSpawnDistance(4)
	mobileTemplate.setMaxSpawnDistance(8)
	mobileTemplate.setDeathblow(False)
	mobileTemplate.setScale(1)
	mobileTemplate.setMeatType("Carnivore Meat")
	mobileTemplate.setMeatAmount(10)
	mobileTemplate.setHideType("Leathery Hide")
	mobileTemplate.setHideAmount(10)
	mobileTemplate.setBoneType("Avian Bones")
	mobileTemplate.setBoneAmount(10)
	mobileTemplate.setSocialGroup("borgle")
	mobileTemplate.setAssistRange(6)
	mobileTemplate.setStalker(False)
	mobileTemplate.setOptionsBitmask(Options.ATTACKABLE)
	
	templates = Vector()
	templates.add('object/mobile/shared_borgle.iff')
	mobileTemplate.setTemplates(templates)
	
	weaponTemplates = Vector()
	weapontemplate = WeaponTemplate('object/weapon/melee/unarmed/shared_unarmed_default.iff', WeaponType.UNARMED, 1.0, 6, 'kinetic')
	weaponTemplates.add(weapontemplate)
	mobileTemplate.setWeaponTemplateVector(weaponTemplates)
	
	attacks = Vector()
	attacks.add('bm_puncture_1')
	attacks.add('bm_siphon_1')
	attacks.add('bm_wing_buffet_3')
	mobileTemplate.setDefaultAttack('creatureMeleeAttack')
	mobileTemplate.setAttacks(attacks)
	
	core.spawnService.addMobileTemplate('young_borgle_gruntling', mobileTemplate)
	return