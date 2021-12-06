package org.helioviewer.jhv.io;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.helioviewer.jhv.log.Log;

import com.google.common.collect.LinkedListMultimap;

import uk.ac.bristol.star.cdf.AttributeEntry;
import uk.ac.bristol.star.cdf.CdfContent;
import uk.ac.bristol.star.cdf.CdfReader;
import uk.ac.bristol.star.cdf.GlobalAttribute;
import uk.ac.bristol.star.cdf.Variable;
import uk.ac.bristol.star.cdf.VariableAttribute;

class CDFUtils {

    private record CDFVariable(Variable variable, Map<String, String> attributes) {
    }

    static void load(URI uri) throws IOException {
        CdfContent cdf = new CdfContent(new CdfReader(new File(uri)));

        LinkedListMultimap<String, String> globalAttrs = LinkedListMultimap.create();
        for (GlobalAttribute attr : cdf.getGlobalAttributes()) {
            String name = attr.getName();
            for (AttributeEntry entry : attr.getEntries()) {
                globalAttrs.put(name, entry.toString());
            }
        }
        dumpGlobalAttrs(globalAttrs);
        String instrumentName = String.join(" ", globalAttrs.get("Instrument_name"));

        Variable[] cdfVars = cdf.getVariables();
        VariableAttribute[] cdfAttrs = cdf.getVariableAttributes();

        CDFVariable[] variables = new CDFVariable[cdfVars.length];
        for (int i = 0; i < cdfVars.length; i++) {
            Variable v = cdfVars[i];
            Map<String, String> attrs = new HashMap<>();
            for (VariableAttribute a : cdfAttrs) {
                AttributeEntry e = a.getEntry(v);
                if (e != null)
                    attrs.put(a.getName(), e.toString());
            }
            variables[i] = new CDFVariable(v, attrs);
        }

        CDFVariable epoch = null;
        for (CDFVariable v : variables) {
            if ("EPOCH".equals(v.variable().getName())) {
                epoch = v;
                break;
            }
        }
        if (epoch == null) {
            Log.error("Epoch not found: " + uri);
            return;
        }

        CDFVariable data = null;
        for (CDFVariable v : variables) {
            Map<String, String> attrs = v.attributes();
            if ("data".equals(attrs.get("VAR_TYPE"))) {
                data = v;
                break;
            }
        }
        if (data == null) {
            Log.error("Data not found: " + uri);
            return;
        }

        CDFVariable label = null;
        String labelRef = data.attributes().get("LABL_PTR_1");
        for (CDFVariable v : variables) {
            if (v.variable().getName().equals(labelRef)) {
                label = v;
                break;
            }
        }
        if (label == null) {
            Log.error("Label not found: " + uri);
            return;
        }

        dumpVariableAttrs(epoch);
        dumpVariableAttrs(data);
        dumpVariableAttrs(label);
    }

    private static void dumpGlobalAttrs(LinkedListMultimap<String, String> map) {
        for (String key : map.keySet()) {
            System.out.println(">>> " + key);
            System.out.println("        " + String.join("\n        ", map.get(key)));
        }
    }

    private static void dumpVariableAttrs(CDFVariable v) {
        System.out.println(">>> " + v.variable().getName());
        for (Map.Entry<String, String> entry : v.attributes().entrySet()) {
            System.out.println("        " + entry.getKey() + ' ' + entry.getValue());
        }
    }

}
