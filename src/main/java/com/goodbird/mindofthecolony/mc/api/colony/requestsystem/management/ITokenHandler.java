package com.goodbird.mindofthecolony.mc.api.colony.requestsystem.management;

import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.manager.IRequestManager;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.token.IToken;

public interface ITokenHandler
{
    IRequestManager getManager();

    /**
     * Generates a new Token for the request system.
     *
     * @return The new token.
     */
    IToken<?> generateNewToken();
}
