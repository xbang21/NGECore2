import sys
from services.spawn import MobileTemplate
from services.spawn import WeaponTemplate
from resources.datatables import WeaponType
from resources.datatables import Difficulty
from resources.datatables import Options
from java.util import Vector


def addTemplate(core):
	mobileTemplate = MobileTemplate()
	
	mobileTemplate.setCreatureName('tatooine_valarian_bookie')
	mobileTemplate.setLevel(18)
	mobileTemplate.setDifficulty(Difficulty.NORMAL)

	mobileTemplate.setMinSpawnDistance(4)
	mobileTemplate.setMaxSpawnDistance(8)
	mobileTemplate.setDeathblow(False)
	mobileTemplate.setScale(1)
	mobileTemplate.setSocialGroup("valarian")
	mobileTemplate.setAssistRange(4)
	mobileTemplate.setStalker(False)
	mobileTemplate.setOptionsBitmask(Options.AGGRESSIVE | Options.ATTACKABLE)
	
	
	templates = Vector()
	templates.add('object/mobile/shared_dressed_valarian_thug_arcona.iff') # need to find correct template
	templates.add('object/mobile/shared_dressed_valarian_thug_bith_female.iff')
	templates.add('object/mobile/shared_dressed_valarian_thug_chiss_female.iff')
	templates.add('object/mobile/shared_dressed_valarian_thug_gotal.iff')
	templates.add('object/mobile/shared_dressed_valarian_thug_gran.iff')
	templates.add('object/mobile/shared_dressed_valarian_thug_naut.iff')
	templates.add('object/mobile/shared_dressed_valarian_thug_weequay.iff')
	mobileTemplate.setTemplates(templates)
	
	weaponTemplates = Vector()
	weapontemplate = WeaponTemplate('object/weapon/ranged/pistol/shared_pistol_cdef.iff', WeaponType.PISTOL, 1.0, 15, 'energy')
	weaponTemplates.add(weapontemplate)
	mobileTemplate.setWeaponTemplateVector(weaponTemplates)
	
	attacks = Vector()
	mobileTemplate.setDefaultAttack('rangedShotpistol')
	mobileTemplate.setAttacks(attacks)
	
	core.spawnService.addMobileTemplate('valarian_bookie', mobileTemplate)
	return