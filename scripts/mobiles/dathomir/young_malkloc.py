import sys
from services.spawn import MobileTemplate
from services.spawn import WeaponTemplate
from resources.datatables import WeaponType
from resources.datatables import Difficulty
from resources.datatables import Options
from java.util import Vector

def addTemplate(core):
	mobileTemplate = MobileTemplate()
	
	mobileTemplate.setCreatureName('young_malkloc')
	mobileTemplate.setLevel(72)
	mobileTemplate.setDifficulty(Difficulty.NORMAL)
	mobileTemplate.setMinSpawnDistance(4)
	mobileTemplate.setMaxSpawnDistance(8)
	mobileTemplate.setDeathblow(False)
	mobileTemplate.setScale(1)
	mobileTemplate.setMeatType("Herbivore Meat")
	mobileTemplate.setMeatAmount(850)
	mobileTemplate.setHideType("Leathery Hide")
	mobileTemplate.setHideAmount(900)
	mobileTemplate.setBoneType("Animal Bones")
	mobileTemplate.setBoneAmount(850)
	mobileTemplate.setSocialGroup("malkloc")
	mobileTemplate.setAssistRange(12)
	mobileTemplate.setStalker(False)
	mobileTemplate.setOptionsBitmask(Options.AGGRESSIVE | Options.ATTACKABLE)
	
	templates = Vector()
	templates.add('object/mobile/shared_young_malkloc.iff')
	mobileTemplate.setTemplates(templates)
		
	attacks = Vector()
	attacks.add('bm_dampen_pain_5')
	attacks.add('bm_deflective_hide')
	attacks.add('bm_puncture_3')
	attacks.add('bm_stomp_5')
	mobileTemplate.setDefaultAttack('creatureMeleeAttack')
	mobileTemplate.setAttacks(attacks)
	
	core.spawnService.addMobileTemplate('young_malkloc', mobileTemplate)