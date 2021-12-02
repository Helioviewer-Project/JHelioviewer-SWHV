package org.helioviewer.jhv.io;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import com.google.common.collect.LinkedListMultimap;

import uk.ac.bristol.star.cdf.AttributeEntry;
import uk.ac.bristol.star.cdf.CdfContent;
import uk.ac.bristol.star.cdf.CdfReader;
import uk.ac.bristol.star.cdf.GlobalAttribute;
import uk.ac.bristol.star.cdf.Variable;
import uk.ac.bristol.star.cdf.VariableAttribute;

class CDFUtils {

    static void load(URI uri) throws IOException {
        CdfContent cdf = new CdfContent(new CdfReader(new File(uri)));

        LinkedListMultimap<String, String> globalAttrs = LinkedListMultimap.create();
        for (GlobalAttribute attr : cdf.getGlobalAttributes()) {
            String name = attr.getName();
            for (AttributeEntry entry : attr.getEntries()) {
                globalAttrs.put(name, entry.toString());
            }
        }
        dump(globalAttrs);

        VariableAttribute[] varAttrs = cdf.getVariableAttributes();

        for (Variable v : cdf.getVariables()) {
            System.out.println(">>> Var " + v.getNum() + " | " + v.getName() + " | " + v.getSummary());
            for (VariableAttribute attr : varAttrs) {
                AttributeEntry entry = attr.getEntry(v);
                if (entry != null)
                    System.out.println("        " + attr.getName() + ' ' + entry);
            }
        }

    }

    private static void dump(LinkedListMultimap<String, String> map) {
        for (String key : map.keySet()) {
            System.out.println(">>> " + key);
            System.out.println("        " + String.join("\n        ", map.get(key)));
        }
    }

}
