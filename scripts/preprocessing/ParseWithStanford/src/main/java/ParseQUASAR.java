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

public class ParseQUASAR {
    public static void main(String[] args) throws IOException {

        String input = args[0];
        String output = args[1];
        int size = Integer.parseInt(args[2]);

        LexicalizedParser lp = LexicalizedParser.loadModel(
                "englishPCFG.ser.gz",
                "-maxLength", "1000", "-retainTmpSubcategories");
        TreebankLanguagePack tlp = new PennTreebankLanguagePack();
        // Uncomment the following line to obtain original Stanford Dependencies
        // tlp.setGenerateOriginalDependencies(true);
        GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory(Filters.<String> acceptFilter());

        String context_file = input;
        LineIterator it = FileUtils.lineIterator(new File(context_file));

        FileWriter fw = new FileWriter(output);

        HashMap<String, JSONObject> context_parses = new HashMap<String, JSONObject>();

        while(it.hasNext()) {
            String line = it.nextLine();
            try {
                JSONParser parser = new JSONParser();
                JSONObject jsonObject = (JSONObject) parser.parse(line);
                String id = (String) jsonObject.get("id");
                JSONArray question = (JSONArray) jsonObject.get("question");
                JSONArray document = (JSONArray) jsonObject.get("document");
                JSONArray offset = (JSONArray) jsonObject.get("offsets");
                JSONArray qlemma = (JSONArray) jsonObject.get("qlemma");
                JSONArray lemma = (JSONArray) jsonObject.get("lemma");
                JSONArray pos = (JSONArray) jsonObject.get("pos");
                JSONArray ner = (JSONArray) jsonObject.get("ner");
                JSONArray answers = (JSONArray) jsonObject.get("answers");

                JSONObject newObject = new JSONObject();

                newObject.put("id", id);
                newObject.put("question", question);
                newObject.put("qlemma", qlemma);


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

                newObject.put("qrelation", relation);
                newObject.put("qparent", parent_id);

                int sent_counter = 0;
                String sentence = "";
                JSONArray newDocument = new JSONArray();
                int length = 0;
                for (Object o : document) {
                    if(o.equals(".") || o.equals("?") || o.equals("!")) {
                        sentence += " " + o;
                        newDocument.add(o);
                        length++;
                        sent_counter++;
                    } else {
                        if(sentence.isEmpty()) {
                            sentence += o;
                            newDocument.add(o);
                            length++;
                        } else {
                            sentence += " " + o;
                            newDocument.add(o);
                            length++;
                        }
                    }
                    if(sent_counter == size) {
                        break;
                    }
                }

                newObject.put("document", newDocument);


                if(context_parses.containsKey(sentence)) {
                    JSONObject temp = context_parses.get(sentence);
                    newObject.put("relation", temp.get("relation"));
                    newObject.put("parent", temp.get("parent"));
                } else {
                    sent = sentence.split(" ");

                    parse = lp.apply(Sentence.toWordList(sent));
                    gs = gsf.newGrammaticalStructure(parse);
                    tdl = gs.typedDependencies();

                    parent_id = new JSONArray();
                    relation = new JSONArray();

                    for (TypedDependency typedDependency : tdl) {
                        String reln = typedDependency.reln().getShortName();
                        String parent = String.valueOf(typedDependency.gov().hashCode());
                        parent_id.add(parent);
                        relation.add(reln);
                    }

                    newObject.put("relation", relation);
                    newObject.put("parent", parent_id);

                    JSONObject temp = new JSONObject();
                    temp.put("relation", relation);
                    temp.put("parent", parent_id);
                    context_parses.put(sentence, temp);
                }

                JSONArray newOffset = new JSONArray();
                int tempCounter = 0;
                for (Object o : offset) {
                    newOffset.add(o);
                    tempCounter++;
                    if(tempCounter == length) {
                        break;
                    }
                }
                newObject.put("offsets", newOffset);

                JSONArray newLemma = new JSONArray();
                tempCounter = 0;
                for (Object o : lemma) {
                    newLemma.add(o);
                    tempCounter++;
                    if(tempCounter == length) {
                        break;
                    }
                }
                newObject.put("lemma", newLemma);

                JSONArray newNER = new JSONArray();
                tempCounter = 0;
                for (Object o : ner) {
                    newNER.add(o);
                    tempCounter++;
                    if(tempCounter == length) {
                        break;
                    }
                }
                newObject.put("ner", newNER);

                JSONArray newPOS = new JSONArray();
                tempCounter = 0;
                for (Object o : pos) {
                    newPOS.add(o);
                    tempCounter++;
                    if(tempCounter == length) {
                        break;
                    }
                }
                newObject.put("pos", newPOS);

                int lastOffset = (int) ((JSONArray)(newOffset.get(newOffset.size() - 1))).get(1);

                JSONArray newAnswers = new JSONArray();

                for (Object answer : answers) {
                    if((int)((JSONArray)answer).get(0) > lastOffset || (int)((JSONArray)answer).get(1) > lastOffset) {
                        break;
                    } else {
                        newAnswers.add(answer);
                    }
                }

                newObject.put("answers", newAnswers);


                fw.append(newObject.toJSONString() + "\n");
                fw.flush();

//                counter++;
//                if(counter % 100 == 0) {
//                    System.out.println(counter);
//                }
            } catch(Exception e) {
//                System.out.println(line);
            }

        }
//        System.out.println(counter);
        fw.close();
    }
}
