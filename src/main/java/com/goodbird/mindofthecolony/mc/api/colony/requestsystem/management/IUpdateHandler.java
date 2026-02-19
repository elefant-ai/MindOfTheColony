package com.goodbird.mindofthecolony.mc.api.colony.requestsystem.management;

import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.management.update.UpdateType;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.manager.IRequestManager;

public interface IUpdateHandler
{
    IRequestManager getManager();

    void handleUpdate(final UpdateType type);

    int getCurrentVersion();
}
