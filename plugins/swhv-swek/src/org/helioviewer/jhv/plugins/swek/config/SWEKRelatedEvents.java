/**
 *
 */
package org.helioviewer.jhv.plugins.swek.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a relationship between events.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public class SWEKRelatedEvents {
    /** The source event */
    private SWEKEventType event;

    /** The related event */
    private SWEKEventType relatedWith;

    /** The parameters on which they are related */
    private List<SWEKRelatedOn> relatedOnList;

    /**
     * Creates a related events with no source event, no related event and an empty related on list.
     */
    public SWEKRelatedEvents() {
        super();
        this.event = null;
        this.relatedWith = null;
        this.relatedOnList = new ArrayList<SWEKRelatedOn>();
    }

    /**
     * Creates a related on relation between the source event and the related with event with the given related on list.
     *
     * @param event             The source event in the relation
     * @param relatedWith       The related event
     * @param relatedOnList     The list of parameters on which the related events are related
     */
    public SWEKRelatedEvents(SWEKEventType event, SWEKEventType relatedWith, List<SWEKRelatedOn> relatedOnList) {
        super();
        this.event = event;
        this.relatedWith = relatedWith;
        this.relatedOnList = relatedOnList;
    }

    /**
     * Gets the source event
     *
     * @return the event
     */
    public SWEKEventType getEvent() {
        return event;
    }

    /**
     * Sets the source event
     *
     * @param event the event to set
     */
    public void setEvent(SWEKEventType event) {
        this.event = event;
    }

    /**
     * Gets the related event
     *
     * @return the relatedWith
     */
    public SWEKEventType getRelatedWith() {
        return relatedWith;
    }

    /**
     * Sets the related event
     *
     * @param relatedWith the relatedWith to set
     */
    public void setRelatedWith(SWEKEventType relatedWith) {
        this.relatedWith = relatedWith;
    }

    /**
     * Gets the list of corresponding event parameters.
     *
     * @return the relatedOnList
     */
    public List<SWEKRelatedOn> getRelatedOnList() {
        return relatedOnList;
    }

    /**
     * Sets the list of corresponding event parameters.
     *
     * @param relatedOnList the relatedOnList to set
     */
    public void setRelatedOnList(List<SWEKRelatedOn> relatedOnList) {
        this.relatedOnList = relatedOnList;
    }



    public class SWEKRelatedOn{
        /** The parameter from the source event  */
        private SWEKParameter parameterFrom;

        /** The parameter from the related event */
        private SWEKParameter parameterWith;

        /**
         * Creates a related on with null parameter for the from and null parameter for the with.
         */
        public SWEKRelatedOn() {
            super();
            this.parameterFrom = null;
            this.parameterWith = null;
        }

        /**
         * Creates a related on with the given from and with event parameters.
         *
         * @param parameterFrom     The parameter from the source event
         * @param parameterWith     The parameter from the related event
         */
        public SWEKRelatedOn(SWEKParameter parameterFrom, SWEKParameter parameterWith) {
            super();
            this.parameterFrom = parameterFrom;
            this.parameterWith = parameterWith;
        }

        /**
         * Gets the source event parameter.
         *
         * @return the parameterFrom
         */
        public SWEKParameter getParameterFrom() {
            return parameterFrom;
        }

        /**
         * Sets the source event parameter.
         *
         * @param parameterFrom the parameterFrom to set
         */
        public void setParameterFrom(SWEKParameter parameterFrom) {
            this.parameterFrom = parameterFrom;
        }

        /**
         * Gets the related event parameter.
         *
         * @return the parameterWith
         */
        public SWEKParameter getParameterWith() {
            return parameterWith;
        }

        /**
         * Sets the related event parameter.
         *
         * @param parameterWith the parameterWith to set
         */
        public void setParameterWith(SWEKParameter parameterWith) {
            this.parameterWith = parameterWith;
        }


    }
}
