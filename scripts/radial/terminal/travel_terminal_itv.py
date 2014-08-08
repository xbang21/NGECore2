from resources.common import RadialOptions
from protocol.swg import EnterTicketPurchaseModeMessageItv
import sys

def createRadial(core, owner, target, radials):
    return
    
def handleSelection(core, owner, target, option):


    if option == 21 and target:
    
        if owner is not None:
            if owner.getCombatFlag() == 1:
				owner.sendSystemMessage('You can\'t use that while in combat.', 0)
				return
            tpm = EnterTicketPurchaseModeMessage(owner.getPlanet().getName(), core.mapService.getClosestCityName(owner), owner, True)
            owner.getClient().getSession().write(tpm.serialize())
            return
    return
    
def handleSUI(owner, window, eventType, returnList):

    return
    
def use(core, actor, object):
	if actor.getObjectID() != object.getOwnerId():
		actor.sendSystemMessage('@travel:not_your_ship', 0)
		return
	return
	