package org.helioviewer.viewmodel.view;

/**
 * View representing an inner knot of the view chain.
 *
 * <p>
 * This view represents an inner knot of the view chain, whose connections to
 * other views can be modified. Since this feature is required for changing the
 * view chain, every view except from the first and last view within the view
 * chain (usually implementations of {@link ImageInfoView} and
 * {@link ComponentView}) and junctions should implement this interface. Otherwise,
 * some things might not work as expected.
 *
 * @author Ludwig Schmidt
 *
 */
public interface ModifiableInnerViewView extends View {

}
