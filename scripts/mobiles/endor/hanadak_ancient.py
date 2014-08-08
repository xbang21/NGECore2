import sys
from services.spawn import MobileTemplate
from services.spawn import WeaponTemplate
from resources.datatables import WeaponType
from resources.datatables import Difficulty
from resources.datatables import Options
from java.util import Vector


def addTemplate(core):
	mobileTemplate = MobileTemplate()
	
	mobileTemplate.setCreatureName('hanadak_ancient')
	mobileTemplate.setLevel(61)
	mobileTemplate.setDifficulty(Difficulty.NORMAL)

	mobileTemplate.setMinSpawnDistance(4)
	mobileTemplate.setMaxSpawnDistance(8)
	mobileTemplate.setDeathblow(True)
	mobileTemplate.setScale(1.2)
	mobileTemplate.setMeatType("Carnivore Meat")
	mobileTemplate.setMeatAmount(40)
	mobileTemplate.setHideType("Bristley Hide")
	mobileTemplate.setHideAmount(40)
	mobileTemplate.setBoneType("Animal Bones")
	mobileTemplate.setBoneAmount(40)
	mobileTemplate.setSocialGroup("hanadak")
	mobileTemplate.setAssistRange(12)
	mobileTemplate.setStalker(False)
	mobileTemplate.setOptionsBitmask(Options.AGGRESSIVE | Options.ATTACKABLE)
	
	templates = Vector()
	templates.add('object/mobile/shared_hanadak.iff')
	mobileTemplate.setTemplates(templates)
	
	weaponTemplates = Vector()
	weapontemplate = WeaponTemplate('object/weapon/melee/unarmed/shared_unarmed_default.iff', WeaponType.UNARMED, 1.0, 6, 'kinetic')
	weaponTemplates.add(weapontemplate)
	mobileTemplate.setWeaponTemplateVector(weaponTemplates)
	
	attacks = Vector()
	attacks.add('bm_dampen_pain_4')
	attacks.add('bm_shaken_2')
	attacks.add('bm_stomp_4')
	mobileTemplate.setDefaultAttack('creatureMeleeAttack')
	mobileTemplate.setAttacks(attacks)
	
	core.spawnService.addMobileTemplate('hanadak_ancient', mobileTemplate)
	return