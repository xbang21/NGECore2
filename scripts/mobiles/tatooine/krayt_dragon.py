import sys
from services.spawn import MobileTemplate
from services.spawn import WeaponTemplate
from resources.datatables import WeaponType
from resources.datatables import Difficulty
from resources.datatables import Options
from java.util import Vector


def addTemplate(core):
	mobileTemplate = MobileTemplate()
	
	mobileTemplate.setCreatureName('krayt_dragon')
	mobileTemplate.setLevel(82)
	mobileTemplate.setDifficulty(Difficulty.BOSS)

	mobileTemplate.setMinSpawnDistance(4)
	mobileTemplate.setMaxSpawnDistance(8)
	mobileTemplate.setDeathblow(True)
	mobileTemplate.setScale(2.6)
	mobileTemplate.setMeatType("Carnivore Meat")
	mobileTemplate.setMeatAmount(750)
	mobileTemplate.setHideType("Bristly Hide")
	mobileTemplate.setBoneAmount(500)	
	mobileTemplate.setBoneType("Animal Bones")
	mobileTemplate.setHideAmount(410)
	mobileTemplate.setSocialGroup("krayt dragon")
	mobileTemplate.setAssistRange(24)
	mobileTemplate.setStalker(False)	
	mobileTemplate.setOptionsBitmask(Options.AGGRESSIVE | Options.ATTACKABLE)
	
	templates = Vector()
	templates.add('object/mobile/shared_krayt_dragon.iff')
	mobileTemplate.setTemplates(templates)

	weaponTemplates = Vector()
	weapontemplate = WeaponTemplate('object/weapon/melee/unarmed/shared_unarmed_default.iff', WeaponType.UNARMED, 1.0, 6, 'kinetic')
	weaponTemplates.add(weapontemplate)
	mobileTemplate.setWeaponTemplateVector(weaponTemplates)
	
	attacks = Vector()
	attacks.add('bm_bite_5')
	attacks.add('bm_bolster_armor_5')
	attacks.add('bm_disease_5')
	attacks.add('bm_enfeeble_5')
	mobileTemplate.setDefaultAttack('creatureMeleeAttack')
	mobileTemplate.setAttacks(attacks)
	
	core.spawnService.addMobileTemplate('krayt_dragon', mobileTemplate)
	return