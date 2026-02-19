package com.goodbird.mindofthecolony.mc.core.colony.requestsystem.resolvers;

import com.google.common.reflect.TypeToken;
import com.goodbird.mindofthecolony.mc.api.colony.buildings.workerbuildings.IWareHouse;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.location.ILocation;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.manager.IRequestManager;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.request.IRequest;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.requestable.deliveryman.Delivery;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.token.IToken;
import com.goodbird.mindofthecolony.mc.api.util.constant.TypeConstants;
import org.jetbrains.annotations.NotNull;

/**
 * Resolves deliveries
 */
public class DeliveryRequestResolver extends DeliverymenRequestResolver<Delivery>
{
    public DeliveryRequestResolver(
      @NotNull final ILocation location,
      @NotNull final IToken<?> token)
    {
        super(location, token);
    }

    @Override
    public boolean canResolveRequest(@NotNull final IRequestManager manager, final IRequest<? extends Delivery> requestToCheck)
    {
        final IWareHouse wareHouse = manager.getColony().getServerBuildingManager().getBuilding(getLocation().getInDimensionLocation(), IWareHouse.class);
        if (wareHouse == null)
        {
            return false;
        }

        return super.canResolveRequest(manager, requestToCheck);
    }

    @Override
    public TypeToken<? extends Delivery> getRequestType()
    {
        return TypeConstants.DELIVERY;
    }

}
