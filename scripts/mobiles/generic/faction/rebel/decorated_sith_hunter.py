import sys
from services.spawn import MobileTemplate
from services.spawn import WeaponTemplate
from resources.datatables import WeaponType
from resources.datatables import Difficulty
from resources.datatables import Options
from resources.datatables import FactionStatus
from java.util import Vector

def addTemplate(core):
	mobileTemplate = MobileTemplate()
	
	mobileTemplate.setCreatureName('fbase_rebel_elite_heavy_trooper_extreme')
	mobileTemplate.setLevel(89)
	mobileTemplate.setDifficulty(Difficulty.ELITE)
	mobileTemplate.setMinSpawnDistance(4)
	mobileTemplate.setMaxSpawnDistance(8)
	mobileTemplate.setDeathblow(False)
	mobileTemplate.setScale(1)
	mobileTemplate.setSocialGroup("rebel")
	mobileTemplate.setAssistRange(24)
	mobileTemplate.setStalker(False)
	mobileTemplate.setFaction("rebel")
	mobileTemplate.setFactionStatus(FactionStatus.Combatant)
	
	
	templates = Vector()
	templates.add('object/mobile/shared_dressed_rebel_ris_01.iff')
	templates.add('object/mobile/shared_dressed_rebel_ris_02.iff')
	templates.add('object/mobile/shared_dressed_rebel_ris_03.iff')
	templates.add('object/mobile/shared_dressed_rebel_ris_04.iff')			
	mobileTemplate.setTemplates(templates)
	
	weaponTemplates = Vector()
	weapontemplate = WeaponTemplate('object/weapon/ranged/carbine/shared_carbine_e5.iff', WeaponType.CARBINE, 1.0, 15, 'energy')
	weaponTemplates.add(weapontemplate)
	mobileTemplate.setWeaponTemplateVector(weaponTemplates)
	
	attacks = Vector()
	mobileTemplate.setDefaultAttack('rangedShot')
	mobileTemplate.setAttacks(attacks)
	
	core.spawnService.addMobileTemplate('decorated_sith_hunter', mobileTemplate)
	return