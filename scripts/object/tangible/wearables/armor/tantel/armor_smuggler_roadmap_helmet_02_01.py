import sys

def setup(core, object):
	object.setStfFilename('static_item_n')
	object.setStfName('armor_smuggler_roadmap_helmet_02_01')
	object.setDetailFilename('static_item_d')
	object.setDetailName('armor_smuggler_roadmap_helmet_02_01')
	object.setIntAttribute('cat_stat_mod_bonus.@stat_n:constitution_modified', 3)
	object.setStringAttribute('class_required', 'Smuggler')
	object.setStringAttribute('armor_category', '@obj_attr_n:armor_reconnaissance')
	object.setIntAttribute('cat_armor_standard_protection.kinetic', 640)
	object.setIntAttribute('cat_armor_standard_protection.energy', 2640)
	object.setIntAttribute('cat_armor_special_protection.special_protection_type_heat', 1640)
	object.setIntAttribute('cat_armor_special_protection.special_protection_type_cold', 1640)
	object.setIntAttribute('cat_armor_special_protection.special_protection_type_acid', 1640)
	object.setIntAttribute('cat_armor_special_protection.special_protection_type_electricity', 1640)
	return	