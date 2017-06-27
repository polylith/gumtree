package com.github.gumtreediff.gen.python3;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.tree.TreeContext;

import java.io.*;
import java.util.Arrays;
import java.util.regex.Pattern;

@Register(id = "python3-ast", accept = "\\.py$")
public class Python3TreeGenerator extends TreeGenerator {
    private static final TreeContext.MetadataSerializers defaultSerializers = new TreeContext.MetadataSerializers();
    private static final TreeContext.MetadataUnserializers defaultUnserializers = new TreeContext.MetadataUnserializers();
    private static final String PYTHON_PARSER_PATH = System.class.getResource("/python_parser.py").getPath();
    public static final java.lang.String PYTHON_BIN_PATH = System.getProperty("gumtree.python.path", "python3");

    static {
        defaultSerializers.add("lines", x -> Arrays.toString((int[]) x));
        Pattern comma = Pattern.compile(", ");
        defaultUnserializers.add("lines", x -> {
            String[] v = comma.split(x.substring(1, x.length() - 2), 4);
            int[] ints = new int[v.length];
            for (int i = 0; i < ints.length; i++) {
                ints[i] = Integer.parseInt(v[i]);
            }
            return ints;
        });
    }

    @Override
    protected TreeContext generate(Reader r) throws IOException {
        //FIXME this is not efficient but I am not sure how to speed up things here.
        File f = File.createTempFile("gumtree", ".py");
        FileWriter w = new FileWriter(f);
        BufferedReader br = new BufferedReader(r);
        String line = br.readLine();
        while (line != null) {
            w.append(line);
            w.append(System.lineSeparator());
            line = br.readLine();
        }
        w.close();
        br.close();

        //FIXME this is not efficient but I am not sure how to speed up things here.
        File python_parser_file = File.createTempFile("python_parser", ".py");
        FileWriter python_parser_w = new FileWriter(python_parser_file);
        BufferedReader python_parser_br = new BufferedReader(new InputStreamReader(System.class.getResourceAsStream("/python_parser.py")));
        String python_parser_line = python_parser_br.readLine();
        while (python_parser_line != null) {
            python_parser_w.append(python_parser_line);
            python_parser_w.append(System.lineSeparator());
            python_parser_line = python_parser_br.readLine();
        }
        python_parser_w.close();
        python_parser_br.close();

        ProcessBuilder b = new ProcessBuilder(PYTHON_BIN_PATH, python_parser_file.getAbsolutePath(), f.getAbsolutePath());
        b.directory(f.getParentFile());
        try {
            Process p = b.start();
            StringBuffer buf = new StringBuffer();
            br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            // TODO Why do we need to read and bufferize eveything, when we could/should only use generateFromStream
            line = null;
            while ((line = br.readLine()) != null)
                buf.append(line + "\n");
            p.waitFor();
            if (p.exitValue() != 0)
                throw new RuntimeException(
                        String.format("cgum Error [%d] %s\n", p.exitValue(), buf.toString())
                );
            r.close();
            String xml = buf.toString();
            return TreeIoUtils.fromXml(Python3TreeGenerator.defaultUnserializers).generateFromString(xml);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            f.delete();
        }
    }
}
