package com.goodbird.mindofthecolony.mc.core.entity.pathfinding.pathjobs;

import com.goodbird.mindofthecolony.mc.core.entity.pathfinding.MNode;

/**
 * Interface for area based search path jobs
 */
public interface ISearchPathJob
{
    public double getEndNodeScore(MNode n);
}
