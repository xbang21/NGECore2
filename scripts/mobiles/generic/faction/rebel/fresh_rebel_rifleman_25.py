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
	
	mobileTemplate.setCreatureName('crackdown_rebel_rifleman')
	mobileTemplate.setLevel(25)
	mobileTemplate.setDifficulty(Difficulty.NORMAL)
	mobileTemplate.setMinSpawnDistance(4)
	mobileTemplate.setMaxSpawnDistance(8)
	mobileTemplate.setDeathblow(False)
	mobileTemplate.setScale(1)
	mobileTemplate.setSocialGroup("rebel")
	mobileTemplate.setAssistRange(6)
	mobileTemplate.setStalker(False)
	mobileTemplate.setFaction("rebel")
	mobileTemplate.setFactionStatus(FactionStatus.Combatant)
	
	
	templates = Vector()
	templates.add('object/mobile/shared_dressed_rebel_trooper_bith_m_01.iff')
	templates.add('object/mobile/shared_dressed_rebel_trooper_human_female_01.iff')
	templates.add('object/mobile/shared_dressed_rebel_trooper_human_male_01.iff')
	templates.add('object/mobile/shared_dressed_rebel_trooper_sullustan_male_01.iff')
	templates.add('object/mobile/shared_dressed_rebel_trooper_twk_female_01.iff')
	templates.add('object/mobile/shared_dressed_rebel_trooper_twk_male_01.iff')				
	mobileTemplate.setTemplates(templates)
	
	weaponTemplates = Vector()
	weapontemplate = WeaponTemplate('object/weapon/ranged/carbine/shared_carbine_e5.iff', WeaponType.CARBINE, 1.0, 15, 'energy')
	weaponTemplates.add(weapontemplate)
	mobileTemplate.setWeaponTemplateVector(weaponTemplates)
	
	attacks = Vector()
	mobileTemplate.setDefaultAttack('rangedShot')
	mobileTemplate.setAttacks(attacks)
	
	core.spawnService.addMobileTemplate('fresh_rebel_rifleman_25', mobileTemplate)
	return