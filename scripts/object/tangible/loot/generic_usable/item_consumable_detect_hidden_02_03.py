import sys

def setup(core, object):
	object.setAttachment('radial_filename', 'object/usable')
	object.setStfFilename('static_item_n')
	object.setStfName('item_consumable_detect_hidden_02_03')
	object.setDetailFilename('static_item_d')
	object.setDetailName('item_consumable_detect_hidden_02_03')
	object.setIntAttribute('reuse_time', 5)
	object.setUses(8)
	object.setStringAttribute('proc_name', '@ui_buff:detecthiddenconsumable65')
	object.setAttachment('alternateBuffName', 'detectHiddenConsumable65')
	return
	