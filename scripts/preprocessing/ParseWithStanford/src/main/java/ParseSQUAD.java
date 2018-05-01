import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.Filters;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

public class ParseSQUAD {
    public static void main(String[] args) throws IOException {

        String input = args[0];
        String output = args[1];
        String PCFG = args[2];

        LexicalizedParser lp = LexicalizedParser.loadModel(
                args[2],
                "-maxLength", "1000", "-retainTmpSubcategories");
        TreebankLanguagePack tlp = new PennTreebankLanguagePack();
        // Uncomment the following line to obtain original Stanford Dependencies
        // tlp.setGenerateOriginalDependencies(true);
        GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory(Filters.<String> acceptFilter());

        String context_file = input;
        LineIterator it = FileUtils.lineIterator(new File(context_file));

        FileWriter fw = new FileWriter(output);

        int counter = 0;

        HashMap<JSONArray, JSONObject> context_parses = new HashMap<JSONArray, JSONObject>();

        while(it.hasNext()) {
            String line = it.nextLine();
            try {
                JSONParser parser = new JSONParser();
                JSONObject jsonObject = (JSONObject) parser.parse(line);
                JSONArray question = (JSONArray) jsonObject.get("question");
                JSONArray document = (JSONArray) jsonObject.get("document");

                String[] sent = new String[question.size()];
                int i = 0;
                for (Object o : question) {
                    sent[i] = (String) o;
                    i++;
                }

                Tree parse = lp.apply(Sentence.toWordList(sent));
                GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
                Collection<TypedDependency> tdl = gs.typedDependencies();

                JSONArray parent_id = new JSONArray();
                JSONArray relation = new JSONArray();

                for (TypedDependency typedDependency : tdl) {
                    String reln = typedDependency.reln().getShortName();
                    int parent_id_value = typedDependency.gov().hashCode();
                    if(parent_id_value < 0 || parent_id_value > sent.length) {
                        parent_id_value = 0;
                    }
                    String parent = String.valueOf(parent_id_value);
                    parent_id.add(parent);
                    relation.add(reln);
                }

                jsonObject.put("qrelation", relation);
                jsonObject.put("qparent", parent_id);

                if(context_parses.containsKey(document)) {
                    JSONObject temp = context_parses.get(document);
                    jsonObject.put("relation", temp.get("relation"));
                    jsonObject.put("parent", temp.get("parent"));
                } else {
                    sent = new String[document.size()];
                    i = 0;
                    for (Object o : document) {
                        sent[i] = (String) o;
                        i++;
                    }

                    parse = lp.apply(Sentence.toWordList(sent));
                    gs = gsf.newGrammaticalStructure(parse);
                    tdl = gs.typedDependencies();

                    parent_id = new JSONArray();
                    relation = new JSONArray();

                    for (TypedDependency typedDependency : tdl) {
                        String reln = typedDependency.reln().getShortName();
                        int parent_id_value = typedDependency.gov().hashCode();
                        if(parent_id_value < 0 || parent_id_value > sent.length) {
                            parent_id_value = 0;
                        }
                        String parent = String.valueOf(parent_id_value);
                        parent_id.add(parent);
                        relation.add(reln);
                    }

                    jsonObject.put("relation", relation);
                    jsonObject.put("parent", parent_id);

                    JSONObject temp = new JSONObject();
                    temp.put("relation", relation);
                    temp.put("parent", parent_id);
                    context_parses.put(document, temp);
                }

                fw.append(jsonObject.toJSONString() + "\n");
                fw.flush();

            } catch(Exception e) {
            }

        }
        fw.close();
    }
}
