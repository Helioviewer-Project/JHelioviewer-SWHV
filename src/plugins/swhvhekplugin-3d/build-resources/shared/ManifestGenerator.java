import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;

/**
 * This class is an Ant task which reads in a file generated with 'bzr -S -r 0 >
 * ${file}' and generates a comma separated list of all versioned files within
 * this branch
 */
public class ManifestGenerator extends Task {

    private String input;
    private String output;

    public void setInput(String input) {
        this.input = input;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    /**
     * {@inheritDoc}
     */
    public void execute() throws BuildException {
        BufferedReader inputReader = null;
        BufferedWriter outputWriter = null;
        boolean first = true;
        String line;

        try {
            try {
                inputReader = new BufferedReader(new FileReader(new File(input)));
                outputWriter = new BufferedWriter(new FileWriter(new File(output)));
    
                while ((line = inputReader.readLine()) != null) {
                    if (line.startsWith("+N ") && !line.endsWith(System.getProperty("file.separator"))) {
                        line = line.substring(3).trim();
                        if (first) {
                            first = false;
                        } else {
                            outputWriter.write(',');
                        }
                        outputWriter.write(line);
                    }
                }
            } finally {
                if (inputReader != null) {
                    inputReader.close();
                }
                if (outputWriter != null) {
                    outputWriter.close();
                }
            }
        } catch (Throwable t) {
            throw new BuildException(t);
        }
    }
}