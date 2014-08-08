import sys

def setup(core, object):
	object.setStfFilename('static_item_n')
	object.setStfName('armor_smuggler_roadmap_boots_02_01')
	object.setDetailFilename('static_item_d')
	object.setDetailName('armor_smuggler_roadmap_boots_02_01')
	object.setIntAttribute('cat_stat_mod_bonus.@stat_n:constitution_modified', 3)
	object.setStringAttribute('class_required', 'Smuggler')
	object.setStringAttribute('armor_category', '@obj_attr_n:armor_reconnaissance')
	return	