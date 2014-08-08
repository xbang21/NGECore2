import sys

def setup(core, object):
	object.setStringAttribute('required_faction', 'Imperial')
	object.setStringAttribute('armor_category', '@obj_attr_n:armor_reconnaissance')
	object.setIntAttribute('cat_armor_standard_protection.kinetic', 4608)
	object.setIntAttribute('cat_armor_standard_protection.energy', 6608)
	object.setIntAttribute('cat_armor_special_protection.special_protection_type_heat', 5608)
	object.setIntAttribute('cat_armor_special_protection.special_protection_type_cold', 5608)
	object.setIntAttribute('cat_armor_special_protection.special_protection_type_acid', 5608)
	object.setIntAttribute('cat_armor_special_protection.special_protection_type_electricity', 5608)
	return	