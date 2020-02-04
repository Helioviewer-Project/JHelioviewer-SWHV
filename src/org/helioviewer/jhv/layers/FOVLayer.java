package org.helioviewer.jhv.layers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.AbstractTableModel;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Colors;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.TableValue;
import org.helioviewer.jhv.gui.components.base.JHVTableCellRenderer;
import org.helioviewer.jhv.gui.components.base.TerminatedFormatterFactory;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.math.Transform;
import org.helioviewer.jhv.opengl.BufVertex;
import org.helioviewer.jhv.opengl.FOVShape;
import org.helioviewer.jhv.opengl.GLSLLine;
import org.helioviewer.jhv.opengl.GLSLShape;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.opengl.text.JhvTextRenderer;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

@SuppressWarnings("serial")
public class FOVLayer extends AbstractLayer {

    private enum FOVType {RECTANGULAR, CIRCULAR}

    private static class FOV {

        private final String name;
        private final FOVType type;
        private final double inner;
        private final double wide;
        private final double high;
        private final byte[] color;
        private boolean selected;

        FOV(String _name, FOVType _type, double innerDeg, double wideDeg, double highDeg, byte[] _color) {
            name = _name;
            type = _type;
            inner = 0.5 * Math.tan(innerDeg * (Math.PI / 180.));
            wide = 0.5 * Math.tan(wideDeg * (Math.PI / 180.));
            high = 0.5 * Math.tan(highDeg * (Math.PI / 180.));
            color = _color;
        }

        void putFOV(FOVShape f, double distance, BufVertex buf, JhvTextRenderer renderer) {
            if (!selected)
                return;
            if (inner > 0)
                f.putCircLine(inner * distance, buf, color);
            if (type == FOVType.RECTANGULAR) {
                f.putRectLine(wide * distance, high * distance, buf, color);
                drawLabel(name, wide * distance, -high * distance, high * distance, renderer);
            } else {
                f.putCircLine(wide * distance, buf, color);
                double halfSide = wide * distance / Math.sqrt(2);
                drawLabel(name, halfSide, -halfSide, halfSide, renderer);
            }
        }

        boolean isSelected() {
            return selected;
        }

        void select() {
            selected = !selected;
        }

        @Override
        public String toString() {
            return name;
        }

    }

    private static final List<FOV> FOVs = List.of(
            new FOV("SOLO/EUI/HRI", FOVType.RECTANGULAR, 0, 16.6 / 60., 16.6 / 60., Colors.Blue),
            new FOV("SOLO/EUI/FSI", FOVType.RECTANGULAR, 0, 228 / 60., 228 / 60., Colors.Blue),
            new FOV("SOLO/METIS", FOVType.CIRCULAR, 3, 5.8, 5.8, Colors.Blue),
            new FOV("SOLO/PHI/HRT", FOVType.RECTANGULAR, 0, 0.28, 0.28, Colors.Blue),
            new FOV("SOLO/PHI/FDT", FOVType.RECTANGULAR, 0, 2, 2, Colors.Blue),
            new FOV("SOLO/SPICE", FOVType.RECTANGULAR, 0, 16 / 60., 11 / 60., Colors.Blue),
            new FOV("SOLO/STIX", FOVType.RECTANGULAR, 0, 2, 2, Colors.Blue)
    );

    private static final double LINEWIDTH_FOV = GLSLLine.LINEWIDTH_BASIC;
    private static final double textEpsilon = 0.09;
    private static final double textScale = 0.075;

    private final FOVShape fov = new FOVShape();
    private final byte[] fovColor = Colors.Blue;
    private final GLSLLine fovLine = new GLSLLine(true);
    private final BufVertex fovBuf = new BufVertex((4 * (FOVShape.RECT_SUBDIVS + 1) + 2) * GLSLLine.stride);
    private final GLSLShape center = new GLSLShape(true);
    private final BufVertex centerBuf = new BufVertex(GLSLShape.stride);

    private final JPanel optionsPanel;

    private boolean drawCustom;
    private double fovAngle = Camera.INITFOV / Math.PI * 180;

    @Override
    public void serialize(JSONObject jo) {
    }

    public FOVLayer(JSONObject jo) {
        optionsPanel = optionsPanel();
    }

    private static void drawLabel(String name, double x, double y, double size, JhvTextRenderer renderer) {
        float textScaleFactor = (float) (textScale / renderer.getFont().getSize2D() * size);
        renderer.begin3DRendering();
        renderer.draw3D(name, (float) x, (float) y, (float) (FOVShape.computeZ(x, y) + textEpsilon), textScaleFactor);
        renderer.end3DRendering();
    }

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;

        double pixFactor = CameraHelper.getPixelFactor(camera, vp);
        Position viewpoint = camera.getViewpoint();

        Transform.pushView();
        Transform.rotateViewInverse(viewpoint.toQuat());
        boolean far = Camera.useWideProjection(viewpoint.distance);
        if (far) {
            Transform.pushProjection();
            camera.projectionOrthoWide(vp.aspect);
        }

        fov.putCenter(centerBuf, fovColor);
        center.setData(gl, centerBuf);
        center.renderPoints(gl, pixFactor);

        JhvTextRenderer renderer = GLText.getRenderer(48);
        FOVs.forEach(f -> f.putFOV(fov, viewpoint.distance, fovBuf, renderer));
        if (drawCustom) {
            double halfSide = 0.5 * viewpoint.distance * Math.tan(fovAngle * (Math.PI / 180.));
            fov.putRectLine(halfSide, halfSide, fovBuf, fovColor);
            drawLabel("Custom", halfSide, -halfSide, halfSide, renderer);
        }
        fovLine.setData(gl, fovBuf);
        fovLine.render(gl, vp.aspect, LINEWIDTH_FOV);

        if (far) {
            Transform.popProjection();
        }
        Transform.popView();
    }

    @Override
    public void renderFloat(Camera camera, Viewport vp, GL2 gl) {
    }

    @Override
    public void init(GL2 gl) {
        fovLine.init(gl);
        center.init(gl);
    }

    @Override
    public void dispose(GL2 gl) {
        fovLine.dispose(gl);
        center.dispose(gl);
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
    }

    @Override
    public Component getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    public String getName() {
        return "FOV";
    }

    @Nullable
    @Override
    public String getTimeString() {
        return null;
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    private static final int ICON_WIDTH = 12;
    private static final int NUMBEROFVISIBLEROWS = 5;

    private static final int SELECTED_COL = 0;
    private static final int OBJECT_COL = 1;

    private JPanel optionsPanel() {
        double fovMin = 0, fovMax = 180;
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(Double.valueOf(fovAngle), Double.valueOf(fovMin), Double.valueOf(fovMax), Double.valueOf(0.01)));
        spinner.setMaximumSize(new Dimension(6, 22));
        spinner.addChangeListener(e -> {
            fovAngle = (Double) spinner.getValue();
            MovieDisplay.display();
        });
        JFormattedTextField f = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
        f.setFormatterFactory(new TerminatedFormatterFactory("%.2f", "\u00B0", fovMin, fovMax));
        WheelSupport.installMouseWheelSupport(spinner);

        JCheckBox customCheckBox = new JCheckBox("Custom angle", false);
        customCheckBox.addChangeListener(e -> {
            drawCustom = !drawCustom;
            MovieDisplay.display();
        });

        JPanel customPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c0 = new GridBagConstraints();
        c0.anchor = GridBagConstraints.LINE_END;
        c0.weightx = 1.;
        c0.weighty = 1.;
        c0.gridy = 0;
        c0.gridx = 0;
        customPanel.add(customCheckBox, c0);
        c0.anchor = GridBagConstraints.LINE_START;
        c0.gridx = 1;
        customPanel.add(spinner, c0);

        FOVModel model = new FOVModel();
        JTable grid = new JTable(model);
        grid.setTableHeader(null);
        grid.setShowGrid(false);
        grid.setRowSelectionAllowed(true);
        grid.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        grid.setColumnSelectionAllowed(false);
        grid.setIntercellSpacing(new Dimension(0, 0));

        grid.getColumnModel().getColumn(SELECTED_COL).setCellRenderer(new SelectedRenderer());
        grid.getColumnModel().getColumn(SELECTED_COL).setPreferredWidth(ICON_WIDTH + 8);
        grid.getColumnModel().getColumn(SELECTED_COL).setMaxWidth(ICON_WIDTH + 8);
        grid.getColumnModel().getColumn(OBJECT_COL).setCellRenderer(new FOVRenderer());

        grid.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!grid.isEnabled())
                    return;
                TableValue v = TableValue.tableValueAtPoint(grid, e.getPoint());
                if (v == null || !(v.value instanceof FOV))
                    return;

                if (v.col == SELECTED_COL) {
                    ((FOV) v.value).select();
                    model.fireTableRowsUpdated(v.row, v.row);
                }
                MovieDisplay.display();
            }
        });

        JScrollPane scroll = new JScrollPane();
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));

        scroll.setViewportView(grid);
        scroll.getViewport().setBackground(grid.getBackground());
        scroll.setPreferredSize(new Dimension(-1, getGridRowHeight(grid) * NUMBEROFVISIBLEROWS + 1));
        grid.setRowHeight(getGridRowHeight(grid));

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c1 = new GridBagConstraints();
        c1.fill = GridBagConstraints.BOTH;
        c1.weightx = 1;
        c1.gridx = 0;

        c1.weighty = 0;
        c1.gridy = 0;
        panel.add(customPanel, c1);

        c1.weighty = 1;
        c1.gridy = 1;
        panel.add(scroll, c1);

        ComponentUtils.smallVariant(panel);
        return panel;
    }

    private int rowHeight = -1;

    private int getGridRowHeight(JTable grid) {
        if (rowHeight == -1) {
            rowHeight = grid.getRowHeight() + 4;
        }
        return rowHeight;
    }

    private static class SelectedRenderer extends JHVTableCellRenderer {

        private final JCheckBox checkBox = new JCheckBox();

        SelectedRenderer() {
            setHorizontalAlignment(CENTER);
            checkBox.putClientProperty("JComponent.sizeVariant", "small");
            checkBox.setBorderPainted(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof FOV) {
                FOV fov = (FOV) value;
                checkBox.setSelected(fov.isSelected());
                checkBox.setBorder(JHVTableCellRenderer.cellBorder);
            }
            checkBox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return checkBox;
        }

    }

    private static class FOVRenderer extends JHVTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof FOV) {
                FOV fov = (FOV) value;
                label.setText(fov.toString());
                label.setBorder(JHVTableCellRenderer.cellBorder);
            }
            return label;
        }
    }

    private static class FOVModel extends AbstractTableModel {

        private final List<FOV> fovs = FOVs;

        @Override
        public int getRowCount() {
            return fovs.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt(int row, int column) {
            return fovs.get(row);
        }

    }

}
