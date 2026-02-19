package com.goodbird.mindofthecolony.mc.core.colony.requestsystem.management.handlers;

import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.management.ITokenHandler;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.manager.IRequestManager;
import com.goodbird.mindofthecolony.mc.api.colony.requestsystem.token.IToken;
import com.goodbird.mindofthecolony.mc.api.util.constant.TypeConstants;
import com.goodbird.mindofthecolony.mc.core.colony.requestsystem.management.IStandardRequestManager;

import java.util.UUID;

/**
 * Class used to handle the inner workings of the request system with regards to tokens.
 */
public class TokenHandler implements ITokenHandler
{

    private final IStandardRequestManager manager;

    public TokenHandler(final IStandardRequestManager manager) {this.manager = manager;}

    @Override
    public IRequestManager getManager()
    {
        return manager;
    }

    /**
     * Generates a new Token for the request system.
     *
     * @return The new token.
     */
    @Override
    public IToken<?> generateNewToken()
    {
        //Force generic type to be correct.
        return manager.getFactoryController().getNewInstance(TypeConstants.ITOKEN, UUID.randomUUID());
    }
}
