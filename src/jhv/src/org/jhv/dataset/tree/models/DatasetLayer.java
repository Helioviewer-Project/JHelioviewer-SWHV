package org.jhv.dataset.tree.models;

import java.util.Enumeration;

import javax.swing.tree.TreeNode;

import org.helioviewer.jhv.layers.LayerDescriptor;

public class DatasetLayer implements TreeNode{
	private LayerDescriptor descriptor;
	private DatasetType parent;
	
	public DatasetLayer( LayerDescriptor descriptor, DatasetType parent){
		this.parent = parent;
		this.descriptor = descriptor;
	}
	public LayerDescriptor getDescriptor() {
		return this.descriptor;
	}
	public String toLongString(){
		String str = "";
		str += "\t\t" + this.descriptor.title + "\n";
		return str;
	}

	public String toString(){
		String str = "";
		str += this.descriptor.title;
		return str;
	}
	
	@Override
	public Enumeration children() {
		return null;
	}
	@Override
	public boolean getAllowsChildren() {
		return false;
	}
	@Override
	public TreeNode getChildAt(int childIndex) {
		return null;
	}
	@Override
	public int getChildCount() {
		return 0;
	}
	@Override
	public int getIndex(TreeNode node) {
		return 0;
	}
	@Override
	public TreeNode getParent() {
		return this.parent;
	}
	@Override
	public boolean isLeaf() {
		return true;
	}
}
