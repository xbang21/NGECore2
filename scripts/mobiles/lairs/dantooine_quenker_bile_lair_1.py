import sys
from java.util import Vector

def addTemplate(core):
	mobileTemplates = Vector()
	mobileTemplates.add('bile_drenched_quenker')

	core.spawnService.addLairTemplate('dantooine_quenker_bile_lair_1', mobileTemplates, 15, 'object/tangible/lair/base/poi_all_lair_thicket_small_fog_green.iff')
	return