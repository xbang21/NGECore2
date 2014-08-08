import sys
from services.spawn import MobileTemplate
from services.spawn import WeaponTemplate
from resources.datatables import WeaponType
from resources.datatables import Difficulty
from resources.datatables import Options
from java.util import Vector


def addTemplate(core):
	mobileTemplate = MobileTemplate()
	
	mobileTemplate.setCreatureName('elder_squall')
	mobileTemplate.setLevel(72)
	mobileTemplate.setDifficulty(Difficulty.NORMAL)

	mobileTemplate.setMinSpawnDistance(4)
	mobileTemplate.setMaxSpawnDistance(8)
	mobileTemplate.setDeathblow(False)
	mobileTemplate.setScale(1)
	mobileTemplate.setHideType("Bristley Hide")
	mobileTemplate.setHideAmount(12)
	mobileTemplate.setBoneType("Animal Bones")
	mobileTemplate.setBoneAmount(8)
	mobileTemplate.setSocialGroup("squall")
	mobileTemplate.setAssistRange(12)
	mobileTemplate.setOptionsBitmask(Options.ATTACKABLE)
	mobileTemplate.setStalker(True)
	
	templates = Vector()
	templates.add('object/mobile/shared_squall.iff')
	mobileTemplate.setTemplates(templates)
	
	weaponTemplates = Vector()
	weapontemplate = WeaponTemplate('object/weapon/melee/unarmed/shared_unarmed_default.iff', WeaponType.UNARMED, 1.0, 6, 'kinetic')
	weaponTemplates.add(weapontemplate)
	mobileTemplate.setWeaponTemplateVector(weaponTemplates)
	
	attacks = Vector()
	attacks.add('bm_bite_4')
	mobileTemplate.setDefaultAttack('creatureMeleeAttack')
	mobileTemplate.setAttacks(attacks)
	
	core.spawnService.addMobileTemplate('elder_squall', mobileTemplate)
	return