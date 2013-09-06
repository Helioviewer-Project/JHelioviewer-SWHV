package org.jhv.dataset.tree.models;

import org.helioviewer.jhv.layers.LayerDescriptor;

public class DatasetLayer{
	private LayerDescriptor descriptor;
	public DatasetLayer( LayerDescriptor descriptor ){
		this.descriptor = descriptor;
	}
	public LayerDescriptor getDescriptor() {
		return this.descriptor;
	}
	public String toString(){
		String str = "";
		str += "\t\t" + this.descriptor.title + "\n";
		return str;
	}
}
