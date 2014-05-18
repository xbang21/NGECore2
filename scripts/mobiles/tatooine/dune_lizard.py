import sys
from services.spawn import MobileTemplate
from services.spawn import WeaponTemplate
from java.util import Vector

def addTemplate(core):
	mobileTemplate = MobileTemplate()
	
	mobileTemplate.setCreatureName('dune_lizard')
	mobileTemplate.setLevel(18)
	mobileTemplate.setMinLevel(18)
	mobileTemplate.setMaxLevel(19)
	mobileTemplate.setDifficulty(0)
	mobileTemplate.setAttackRange(5)
	mobileTemplate.setAttackSpeed(1.0)
	mobileTemplate.setWeaponType(6)
	mobileTemplate.setMinSpawnDistance(4)
	mobileTemplate.setMaxSpawnDistance(8)
	mobileTemplate.setDeathblow(True)
	mobileTemplate.setScale(1)
	mobileTemplate.setMeatType("Carnivore Meat")
	mobileTemplate.setMeatAmount(85)
	mobileTemplate.setHideType("Bristly Hide")
	mobileTemplate.setBoneAmount(50)	
	mobileTemplate.setBoneType("Animal Bone")
	mobileTemplate.setHideAmount(35)
	mobileTemplate.setSocialGroup("dune lizard")
	mobileTemplate.setAssistRange(10)
	mobileTemplate.setStalker(True)	
	mobileTemplate.setOptionsBitmask(192)
	
	templates = Vector()
	templates.add('object/mobile/shared_dune_lizard.iff')
	mobileTemplate.setTemplates(templates)

	weaponTemplates = Vector()
	weapontemplate = WeaponTemplate('object/weapon/ranged/creature/shared_creature_spit_small_toxicgreen.iff', 6, 1.0)
	weaponTemplates.add(weapontemplate)
	mobileTemplate.setWeaponTemplateVector(weaponTemplates)
	
	attacks = Vector()
	mobileTemplate.setDefaultAttack('creatureMeleeAttack')
	mobileTemplate.setAttacks(attacks)
	
	core.spawnService.addMobileTemplate('dune_lizard', mobileTemplate)
	return