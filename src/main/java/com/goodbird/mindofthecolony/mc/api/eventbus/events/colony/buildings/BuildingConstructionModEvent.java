package com.goodbird.mindofthecolony.mc.api.eventbus.events.colony.buildings;

import com.goodbird.mindofthecolony.mc.api.colony.buildings.IBuilding;
import com.goodbird.mindofthecolony.mc.core.colony.workorders.WorkOrderBuilding;

/**
 * Event for when a building was built, upgraded, repaired or removed.
 */
public final class BuildingConstructionModEvent extends AbstractBuildingModEvent
{
    /**
     * The work order which was completed.
     */
    private final WorkOrderBuilding workOrder;

    /**
     * Building construction event.
     *
     * @param building  the building the event was for.
     * @param workOrder the work order which was completed.
     */
    public BuildingConstructionModEvent(final IBuilding building, final WorkOrderBuilding workOrder)
    {
        super(building);
        this.workOrder = workOrder;
    }

    /**
     * Get the work order which was completed.
     *
     * @return the event type.
     */
    public WorkOrderBuilding getWorkOrder()
    {
        return workOrder;
    }
}
