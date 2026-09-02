package eva.evangelion.view.shape;

import javafx.beans.binding.DoubleBinding;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.transform.Rotate;

public class Arrow extends Line {

    private Polygon triangle;
    private Pane Parent;

    // -------- Constructors --------
    public Arrow(Pane Parent) {
        super(0, 2, 3, 5);
        this.Parent = Parent;
        Parent.getChildren().addAll(this);
        triangle = new Polygon(getEndX(), getEndY(), getEndX() - 16, getEndY() + 8, getEndX() - 16, getEndY() - 8);
        Parent.getChildren().add(triangle);
        canvas(3, 5);
        this.setDisable(true);
        triangle.setDisable(true);
    }

    public Arrow(Pane parent, Color color, int startX, int startY, int endX, int endY) {
        this(parent);
        SetColor(color);
        DrawArrow(startX, startY, endX, endY);
        this.setDisable(true);
        triangle.setDisable(true);
    }

    // New double constructor (for animation) – triangle is defined with tip at (0,0)
    public Arrow(Pane parent, Color color, double startX, double startY, double endX, double endY) {
        super(startX, startY, endX, endY);
        this.Parent = parent;
        triangle = new Polygon(0, 0, -16, -8, -16, 8); // tip at (0,0)
        triangle.setFill(color);
        triangle.setStroke(color);
        parent.getChildren().addAll(this, triangle);
        this.setDisable(true);
        triangle.setDisable(true);
        // Position triangle exactly at the end point (no offset)
        canvas(endX, endY);
    }

    // -------- Dynamic updates (reuse canvas) --------
    public void setStart(double x, double y) {
        setStartX(x);
        setStartY(y);
        // Update triangle position and rotation using current end point
        canvas(getEndX(), getEndY());
    }

    public void setEnd(double x, double y) {
        setEndX(x);
        setEndY(y);
        canvas(getEndX(), getEndY());
    }

    // -------- Original methods (adapted) --------
    public void delete() {
        Parent.getChildren().remove(this);
        Parent.getChildren().remove(triangle);
    }

    public void SetColor(Color color) {
        this.setStroke(color);
        triangle.setFill(color);
    }

    private DoubleBinding dx;
    private DoubleBinding dy;

    public double StartX;
    public double StartY;
    public double EndX;
    public double EndY;

    public void DrawArrow(double startX, double startY, double endX, double endY) {
        setStartX(startX);
        setStartY(startY);
        setEndX(endX);
        setEndY(endY);
        canvas(endX, endY);  // no offset
    }

    public void setStartEnd(double startX, double startY, double endX, double endY) {
        StartX = startX;
        StartY = startY;
        EndX = endX;
        EndY = endY;
    }

    public void UpdateArrow(double x, double y) {
        DrawArrow(StartX - x, StartY - y, EndX - x, EndY - y);
    }

    public void canvas(double x, double y) {
        // Place triangle at the end of the line
        triangle.setLayoutX(x);
        triangle.setLayoutY(y);
        updateTriangleRotation();
    }

    private void updateTriangleRotation() {
        double angle = Math.toDegrees(Math.atan2(
                getEndY() - getStartY(),
                getEndX() - getStartX()
        ));
        // Rotate around the tip (0,0) so that the tip stays at the end point
        triangle.getTransforms().clear();
        triangle.getTransforms().add(new Rotate(angle, 0, 0));
    }

    private double getAngle(double dy, double dx) {
        return Math.toDegrees(Math.atan2(dy, dx));
    }
}