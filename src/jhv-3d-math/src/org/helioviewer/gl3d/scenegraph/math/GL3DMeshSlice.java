package org.helioviewer.gl3d.scenegraph.math;

import java.util.ArrayList;
import java.util.List;

public class GL3DMeshSlice {

	private List<GL3DVec3d> triangles;
	private GL3DVec3d planeNormal;
	
	private List<GL3DVec3d> resultLeft  = new ArrayList<GL3DVec3d>();
	private List<GL3DVec3d> resultRight = new ArrayList<GL3DVec3d>();
	
	
	
	public GL3DMeshSlice() {
		triangles = new ArrayList<GL3DVec3d>();
		planeNormal = new GL3DVec3d(0, 0, 1);
	}
	
	
	
	public GL3DMeshSlice(List<GL3DVec3d> triangles, GL3DVec3d planeNormal) {
		setInput(triangles, planeNormal);
	}
	
	
	
	public void setInput(List<GL3DVec3d> triangles, GL3DVec3d planeNormal) {
		this.triangles   = triangles;
		this.planeNormal = planeNormal;
	}
	
	
	
	public boolean hasBothSides() {
		return resultLeft.size() > 0 && resultRight.size() > 0;
	}
	
	
	public List<GL3DVec3d> getOnlyResult()
	{
		if(resultLeft.size() > 0 && resultRight.size() == 0)
			return resultLeft;
		if(resultLeft.size() == 0 && resultRight.size() > 0)
			return resultRight;
		return null;
	}
	
	
	public List<GL3DVec3d> getResultLeft() {
		return resultLeft;
	}
	
	
	public List<GL3DVec3d> getResultRight() {
		return resultRight;
	}
	
	
	
	public void slice()
	{
		resultLeft.clear();
		resultRight.clear();
		
		GL3DVec3d[] tri = new GL3DVec3d[3];
		int side1;
		int side2;
		int sign;

		// Reset callbacks
		//reset();

		// Loop through faces, slice it if necessary
		// In all cases: Use callback objects to process results
		for(int i=0; i<triangles.size()-2; i+=3)
		{
			tri[0] = triangles.get(i);
			tri[1] = triangles.get(i+1);
			tri[2] = triangles.get(i+2);

			side1 = 0x00;
			side2 = 0x00;

			// Calculate sign mask for vertices
			// .... 0000 0000 0cba -> bit flags for 3 vertices (a, b, c).
			// bit is 1 if vertex lies on that side, 0 if not
			// if bit is 0 for both sides -> vertex lies directly on the plane
			sign   = pointPlaneSign(tri[2]);
			side1 |= (sign== 1 ? 1 : 0); side1 <<= 1;
			side2 |= (sign==-1 ? 1 : 0); side2 <<= 1;

			sign   = pointPlaneSign(tri[1]);
			side1 |= (sign== 1 ? 1 : 0); side1 <<= 1;
			side2 |= (sign==-1 ? 1 : 0); side2 <<= 1;

			sign   = pointPlaneSign(tri[0]);
			side1 |= (sign== 1 ? 1 : 0);
			side2 |= (sign==-1 ? 1 : 0);

			// No vertices directly on plane
			if((side1 | side2) == 7)
			{
				// Case 1a) All on side1
				if(side2==0)
					case1(tri, true);
				// Case 1b) All on side2
				else if(side1==0)
					case1(tri, false);

				// Case 2a) 2 on side1, 1 on side2
				else if(side2 == (side2 & -side2))
					case2(tri, side2, true);
				// Case 2b) 1 on side1, 2 on side2
				else
					case2(tri, side1, false);
			}

			// Minimum of 1 vertex lies on the plane -> 1 or 2 bits set in total
			else if((side1 | side2) != 0)
			{
				// Min 1 vertex on side1
				if(side1>0)
				{
					// Case 3) 1 on both sides, 1 on plane
					if(side2>0)
						case3(tri, side1, side2);

					// Case 4a) 2 vertices on side1, 1 on plane
					else if(side1 != (side1 & -side1))
						case1(tri, true); // Handle like case 1

					// Case 5a) 1 vertex on side1, 2 on plane
					else
						case1(tri, true); // Use case 1 without edges
				}

				// v-- No vertices on side1 -> 1 or 2 vertices on side2 --v

				// Case 4b) 2 vertices on side2, 1 on plane
				else if(side2 != (side2 & -side2))
					case1(tri, false); // Handle like case 1

				// Case 5b) 1 vertex on side2, 2 on plane
				else
					case1(tri, false); // Use case 1 without edges
			}

			// Case 6) All 3 vertices on the plane
			else
				case1(tri, true); // Handle like case 1
		}

		// Finalize callbacks
		//finalize();
	}
	
	
	
	private int pointPlaneSign(GL3DVec3d p)
	{
		// Plane lies on (0, 0, 0)

		double dot = planeNormal.dot(p);

		if(dot < -0.0000001)
			return -1;
		else if(dot > 0.0000001)
			return 1;
		else
			return 0;
	}
	
	
	
	private GL3DVec3d linePlaneIntersection(GL3DVec3d l1, GL3DVec3d l2)
	{
		// Plane lies on (0, 0, 0)
		
		GL3DVec3d diff = new GL3DVec3d(l2);
		diff.subtract(l1);

		GL3DVec3d n = new GL3DVec3d(planeNormal);
		n.multiply(l1);
		
		double t = planeNormal.dot( GL3DVec3d.multiply(l1, -1) )  /  planeNormal.dot(diff);
		
		return new GL3DVec3d(
			l1.x + t * diff.x,
			l1.y + t * diff.y,
			l1.z + t * diff.z );
	}
	
	
	
	private void case1(GL3DVec3d[] tri, boolean left) 
	{
		triangle(tri[0], tri[1], tri[2], left);
	}



	private void case2(GL3DVec3d[] tri, int side, boolean twoLeft)
	{
		// Find index of single vertex
		int idx = 0;
		for(; (side & 1) == 0; ++idx, side >>= 1);

		// Calculate intersection points
		final int suc = idx==2 ? 0 : idx+1;
		final int pre = idx==0 ? 2 : idx-1;

		GL3DVec3d a = linePlaneIntersection(tri[idx], tri[suc]); // Successor of the single vertex, counterclockwise
		GL3DVec3d b = linePlaneIntersection(tri[idx], tri[pre]); // Predecessor of the single vertex, counterclockwise

		// Triangluate and add to volume
		if(twoLeft)
		{
			triangle(tri[suc], tri[pre], a, true);
			triangle(tri[pre], b, a, true);
			//edge(b, a);

			triangle(tri[idx], a, b, false);
		}
		else
		{
			triangle(tri[idx], a, b, true);
			//edge(a, b);

			triangle(tri[suc], tri[pre], a, false);
			triangle(tri[pre], b, a, false);
		}
	}



	private void case3(GL3DVec3d[] tri, int side1, int side2)
	{
		int left    = 0;
		int right   = 0;
		int onPlane = 0;

		// Assign vertices
		for(int i=0; i<=2; ++i, side1>>=1, side2>>=1)
		{
			if((side1 & 1) == 1)
				left = i;
			else if((side2 & 1) == 1)
				right = i;
			else
				onPlane = i;
		}

		// Calculate intersection point
		GL3DVec3d a = linePlaneIntersection(tri[left], tri[right]);

		// Check winding
		if(right == (left==2 ? 0 : left+1))
		{
			triangle(tri[left], a, tri[onPlane], true);
			triangle(tri[right], tri[onPlane], a, false);

			//edge(a, tri[onPlane]);
		}
		else
		{
			triangle(tri[left], tri[onPlane], a, true);
			triangle(tri[right], a, tri[onPlane], false);

			//edge(tri[onPlane], a);
		}
	}



	/*private void case5(GL3DVec3d[] tri, int side, boolean left)
	{
		triangle(tri[0], tri[1], tri[2], left);

		// Which is the single vertex?
		int idx = 0;
		for(; (side & 1) == 0; ++idx, side>>=1);

		final int suc = idx==2 ? 0 : idx+1;
		final int pre = idx==0 ? 2 : idx-1;

		// Add polygon edge
		if(left)
			edge(*tri[suc], *tri[pre]);
		else
			edge(*tri[pre], *tri[suc]);
	}*/
	
	
	
	private void triangle(GL3DVec3d a, GL3DVec3d b, GL3DVec3d c, boolean left)
	{
		List<GL3DVec3d> result = left ? resultLeft : resultRight;
		result.add(new GL3DVec3d(a));
		result.add(new GL3DVec3d(b));
		result.add(new GL3DVec3d(c));
	}
}
