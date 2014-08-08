import sys
from services.spawn import MobileTemplate
from services.spawn import WeaponTemplate
from resources.datatables import WeaponType
from resources.datatables import Difficulty
from resources.datatables import Options
from java.util import Vector


def addTemplate(core):
	mobileTemplate = MobileTemplate()

	mobileTemplate.setCreatureName('corsec_sergeant_aggro')
	mobileTemplate.setLevel(81)
	mobileTemplate.setDifficulty(Difficulty.NORMAL)

	mobileTemplate.setMinSpawnDistance(4)
	mobileTemplate.setMaxSpawnDistance(8)
	mobileTemplate.setDeathblow(True)
	mobileTemplate.setScale(1)
	mobileTemplate.setSocialGroup("aggro corsec")
	mobileTemplate.setAssistRange(10)
	mobileTemplate.setStalker(True)
	mobileTemplate.setOptionsBitmask(Options.AGGRESSIVE | Options.ATTACKABLE)

	templates = Vector()
	templates.add('object/mobile/shared_dressed_corsec_captain_human_female_01.iff')
	templates.add('object/mobile/shared_dressed_corsec_captain_human_male_01.iff')
	templates.add('object/mobile/shared_dressed_corsec_captain_human_male_02.iff')
	templates.add('object/mobile/shared_dressed_corsec_captain_human_male_03.iff')
	templates.add('object/mobile/shared_dressed_corsec_officer_human_female_01.iff')
	templates.add('object/mobile/shared_dressed_corsec_officer_human_male_01.iff')
	templates.add('object/mobile/shared_dressed_corsec_detective_human_female_01.iff')
	templates.add('object/mobile/shared_dressed_corsec_detective_human_male_01.iff')
	mobileTemplate.setTemplates(templates)

	weaponTemplates = Vector()
	weapontemplate = WeaponTemplate('object/weapon/ranged/carbine/shared_carbine_cdef.iff', WeaponType.CARBINE, 1.0, 15, 'energy')
	weaponTemplates.add(weapontemplate)
	mobileTemplate.setWeaponTemplateVector(weaponTemplates)

	attacks = Vector()
	mobileTemplate.setDefaultAttack('rangedShot')
	mobileTemplate.setAttacks(attacks)
	
	lootPoolNames_1 = ['Junk']
	lootPoolChances_1 = [100]
	lootGroupChance_1 = 100
	mobileTemplate.addToLootGroups(lootPoolNames_1,lootPoolChances_1,lootGroupChance_1)
	
	core.spawnService.addMobileTemplate('corsec_special_ops_sergeant', mobileTemplate)
	return