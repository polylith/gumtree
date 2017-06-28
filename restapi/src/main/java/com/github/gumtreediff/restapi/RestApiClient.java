package com.github.gumtreediff.restapi;

import com.github.gumtreediff.gen.Generators;
import com.github.gumtreediff.gen.python3.Python3TreeGenerator;
import com.github.gumtreediff.io.TreeIoUtils;
import com.github.gumtreediff.tree.TreeContext;
import org.json.JSONObject;

import static spark.Spark.*;

public class RestApiClient {
    public static void main(String[] args) {
        post("/parse/", (request, response) -> {
            String body = request.body();
            JSONObject json = new JSONObject(body);
            String type = (String) json.get("type");
            String content = (String) json.get("content");

            TreeContext tree = new Python3TreeGenerator().generateFromString(content);
            TreeIoUtils.TreeSerializer treeSerializer = TreeIoUtils.toJson(tree);

            return treeSerializer;
        });
    }
}
