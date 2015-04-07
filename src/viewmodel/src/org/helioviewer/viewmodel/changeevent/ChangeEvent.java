package org.helioviewer.viewmodel.changeevent;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.helioviewer.viewmodel.view.View;

/**
 * Container for change events.
 *
 * A change event acts as a message in the view chain that something has
 * changed. Therefore it can hold different kinds of {@link ChangedReason} which
 * give more details about it.
 *
 * @author Stephan Pagel
 * @see ChangedReason
 * */
public class ChangeEvent {

    // History (stack) of all change reasons
    protected LinkedList<ChangedReason> history;

    // Types of occurred change reasons
    protected HashSet<Class<? extends ChangedReason>> availableChangeReasons;

    // Used to get the next changed event id
    private static long changeEventIDCounter = 0;

    // Memorizes the change event id of the object
    private long changeEventID;

    /**
     * Default constructor
     * */
    public ChangeEvent() {
        // initialize global objects
        history = new LinkedList<ChangedReason>();
        availableChangeReasons = new HashSet<Class<? extends ChangedReason>>();
        // assign an unique id of the object
        changeEventID = getNextID();
    }

    /**
     * Constructor which creates a new change event container and adds a passed
     * changed reason.
     *
     * If a null value is passed the change event container will be created but
     * adding a change reason will be ignored.
     *
     * @param aReason
     *            A change reason which should be added to the change event
     *            container.
     * */
    public ChangeEvent(ChangedReason aReason) {
        this();
        // add reason to history
        addReason(aReason);
    }

    /**
     * Constructor cleaning up the history, but keeping the id. Therefore, to
     * the following still returns true:
     * <p>
     * (new ChangeEvent(original)).equals(original)
     */
    public ChangeEvent(ChangeEvent original) {
        // initialize global objects
        history = new LinkedList<ChangedReason>();
        availableChangeReasons = new HashSet<Class<? extends ChangedReason>>();
        changeEventID = original.changeEventID;
    }

    /**
     * Creates a new unique change event id.
     *
     * The unique change event id is the next (increased by 1) integral number
     * of the last change event.
     *
     * @return New unique change event id.
     * */
    private static long getNextID() {
        return ++changeEventIDCounter;
    }

    /**
     * Compares this object to the specified object.
     *
     * The result is true if and only if the argument is not null and is an
     * ChangeEvent object that contains the same change event id as this object.
     *
     * @param obj
     *            the object to compare with.
     * @return true if the objects are the same; false otherwise.
     * @see #hashCode()
     */

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (obj instanceof ChangeEvent) {
            return this.changeEventID == ((ChangeEvent) obj).changeEventID;
        }
        return false;
    }

    /**
     * Returns a hash code for this ChangeEvent
     *
     * @return a hash code value for this object, computed from the change event
     *         id for this ChangeEvent object.
     * @see #equals(Object)
     * */

    @Override
    public int hashCode() {
        return (int) (changeEventID ^ (changeEventID >>> 32));
    }

    /**
     * Adds a change reason to the change event container.
     *
     * If a null value is passed this method will do nothing.
     *
     * @param aReason
     *            A change reason which should be added to the change event
     *            container.
     * */
    public void addReason(ChangedReason aReason) {
        if (aReason != null) {
            // add reason type to hash
            availableChangeReasons.add(aReason.getClass());
            // add reason to history
            history.addFirst(aReason);
        }
    }

    /**
     * Returns the latest added change reason of the change event container.
     *
     * @return the latest added change reason or null if no change reason is
     *         available.
     * */
    public ChangedReason getLatestReason() {
        return history.getFirst();
    }

    /**
     * Checks if an instance of an object of the passed change reason class was
     * added to the change event container.
     *
     * @param aReasonClass
     *            Class for which an instance should be searched for.
     *
     * @return true if an instance of an object of the passed change reason
     *         class was found in the change event container; false otherwise.
     *
     *         Checks if the ChangeReason class (type) exists in the change
     *         event and returns a boolean value.
     * */
    public <T extends ChangedReason> boolean reasonOccurred(Class<T> aReasonClass) {
        return availableChangeReasons.contains(aReasonClass);
    }

    /**
     * Returns the latest changed reason object of the given type, which was
     * added to the change event container.
     *
     * @param aReasonClass
     *            Class for which an instance should be searched for.
     * @return the latest changed reason object of the passed type. If no
     *         instance of the passed class was added to the change event
     *         container a null value will be returned.
     * @see #getLastChangedReasonByTypeAndView(Class, View)
     */
    @SuppressWarnings(value = { "unchecked" })
    public <T extends ChangedReason> T getLastChangedReasonByType(Class<T> aReasonClass) {
        // go through history from latest to eldest entry
        for (ChangedReason r : history) {
            if (aReasonClass.isInstance(r))
                return (T) r;
        }
        return null;
    }

    public boolean hasReasons() {
        return history.size() != 0;

    }

    /**
     * Returns the latest changed reason object of the given type and view,
     * which was added to the change event container.
     *
     * @param aReasonClass
     *            Class for which an instance should be searched for.
     * @param view
     *            View for which an instance should be searched for.
     * @return the latest changed reason object of the passed type and view. If
     *         no instance of the passed class and view was added to the change
     *         event container a null value will be returned.
     * @see #getLastChangedReasonByType(Class)
     */
    @SuppressWarnings(value = { "unchecked" })
    public <T extends ChangedReason> T getLastChangedReasonByTypeAndView(Class<T> aReasonClass, View view) {
        // go through history from latest to eldest entry
        for (ChangedReason r : history) {
            if (aReasonClass.isInstance(r) && r.getView() == view)
                return (T) r;
        }
        return null;
    }

    /**
     * Returns all changed reasons objects of the given type, which were added
     * to the change event container.
     *
     * The list s sorted from old to new.
     *
     * @param aReasonClass
     *            Type to search for
     * @return List of all change reasons of the given type. The list might be
     *         empty, but is never null.
     */
    @SuppressWarnings("unchecked")
    public <T extends ChangedReason> List<T> getAllChangedReasonsByType(Class<T> aReasonClass) {
        LinkedList<T> outputList = new LinkedList<T>();

        for (ChangedReason r : history) {
            if (aReasonClass.isInstance(r)) {
                outputList.addFirst((T) r);
            }
        }
        return outputList;
    }

    public void reinitialize() {
        history.clear();
        availableChangeReasons.clear();
        changeEventID = getNextID();
    }

    public boolean isEmpty() {
        return history.isEmpty() && availableChangeReasons.isEmpty();
    }

    @Override
    public ChangeEvent clone() {
        ChangeEvent res = new ChangeEvent();
        res.history.addAll(history);
        res.availableChangeReasons.addAll(availableChangeReasons);
        res.changeEventID = changeEventID;
        return res;
    }

    @Override
    public String toString() {
        return availableChangeReasons.toString();
    }

    public void copyFrom(ChangeEvent event) {
        if (event != null) {
            this.history.addAll(event.history);
            this.availableChangeReasons.addAll(event.availableChangeReasons);
        }
    }

}
