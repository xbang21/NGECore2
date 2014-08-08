import sys
from services.spawn import MobileTemplate
from services.spawn import WeaponTemplate
from resources.datatables import WeaponType
from resources.datatables import Difficulty
from resources.datatables import Options
from java.util import Vector


def addTemplate(core):
	mobileTemplate = MobileTemplate()
	
	mobileTemplate.setCreatureName('pudgy_nudfuh')
	mobileTemplate.setLevel(36)
	mobileTemplate.setDifficulty(Difficulty.NORMAL)

	mobileTemplate.setMinSpawnDistance(4)
	mobileTemplate.setMaxSpawnDistance(8)
	mobileTemplate.setDeathblow(False)
	mobileTemplate.setScale(1)
	mobileTemplate.setMeatType("Herbivore Meat")
	mobileTemplate.setMeatAmount(900)
	mobileTemplate.setHideType("Leathery Hide")
	mobileTemplate.setHideAmount(1000)
	mobileTemplate.setBoneType("Avian Bones")
	mobileTemplate.setBoneAmount(900)
	mobileTemplate.setSocialGroup("nudfuh")
	mobileTemplate.setAssistRange(6)
	mobileTemplate.setStalker(False)
	mobileTemplate.setOptionsBitmask(Options.AGGRESSIVE | Options.ATTACKABLE)
	
	templates = Vector()
	templates.add('object/mobile/shared_nudfuh.iff')
	mobileTemplate.setTemplates(templates)
	
	weaponTemplates = Vector()
	weapontemplate = WeaponTemplate('object/weapon/melee/unarmed/shared_unarmed_default.iff', WeaponType.UNARMED, 1.0, 6, 'kinetic')
	weaponTemplates.add(weapontemplate)
	mobileTemplate.setWeaponTemplateVector(weaponTemplates)
	
	attacks = Vector()
	attacks.add('bm_dampen_pain_3')
	attacks.add('bm_deflective_hide')
	attacks.add('bm_puncture_1')
	attacks.add('bm_shaken_3')
	attacks.add('bm_stomp_3')
	mobileTemplate.setDefaultAttack('creatureMeleeAttack')
	mobileTemplate.setAttacks(attacks)
	
	core.spawnService.addMobileTemplate('pudgy_nudfuh', mobileTemplate)
	return