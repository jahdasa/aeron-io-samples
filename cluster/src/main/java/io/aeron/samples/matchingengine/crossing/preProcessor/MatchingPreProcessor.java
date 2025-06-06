package io.aeron.samples.matchingengine.crossing.preProcessor;

import io.aeron.samples.matchingengine.crossing.MatchingContext;

public interface MatchingPreProcessor {
    enum MATCHING_ACTION {
        NO_ACTION,
        AGGRESS_ORDER,
        ADD_AND_AGGRESS,
        PARK_ORDER,
        ADD_ORDER;
    }

    void preProcess(MatchingContext context);
}
