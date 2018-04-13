import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ReadQUASAR {
    static HashMap<String, ArrayList<String>> contextsHash = new HashMap<String, ArrayList<String>>();
    static HashMap<String, HashMap<String, String>> questions = new HashMap<String, HashMap<String, String>>();

    public static void readJSON(String context_file, String answer_file) throws ParseException, IOException {
        LineIterator it = FileUtils.lineIterator(new File(context_file));
        int numberOfSentence = 0;
        int numberOfParagraphs = 0;
        while(it.hasNext()) {
            String line = it.nextLine();
            try {
                JSONParser parser = new JSONParser();
                JSONObject jsonObject = (JSONObject) parser.parse(line);
                String id = (String) jsonObject.get("uid");
                JSONArray contexts = (JSONArray) jsonObject.get("contexts");
                ArrayList<String> contextsArray = new ArrayList<String>();
                for (Object context : contexts) {
                    int i = 0;
                    for (Object text : (JSONArray) context) {
                        if (i == 1) {
                            contextsArray.add((String) text);
                            String[] sentences = ((String) text).split("[.]");
                            numberOfSentence += sentences.length;
                            numberOfParagraphs++;
                        }
                        i++;
                    }
                }
                contextsHash.put(id, contextsArray);
            } catch(Exception e) {
                System.out.println(line);
            }
//            System.out.print("");
        }

//        System.out.println("Total number of contexts (topics) = " + contextsHash.size());
//        System.out.println("Pragraphs per topic = " + numberOfParagraphs / contextsHash.size());
//        System.out.println("Total number of paragraphs = " + numberOfParagraphs);
//        System.out.println("Total number of sentences = " + numberOfSentence);
//        System.out.println("Average length of paragraph = " + numberOfSentence/numberOfParagraphs + " sentences");

        it = FileUtils.lineIterator(new File(answer_file));

        int numberOfQuestions = 0;

        while(it.hasNext()) {
            String line = it.nextLine();
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(line);
            String question = (String) jsonObject.get("question");
            String answer = (String) jsonObject.get("answer");
            String id = (String) jsonObject.get("uid");
            if(questions.containsKey(id)) {
                questions.get(id).put(question, answer);
            } else {
                HashMap<String, String> questionAnswer = new HashMap<String, String>();
                questionAnswer.put(question, answer);
                questions.put(id, questionAnswer);
            }
            numberOfQuestions++;
        }

//        System.out.println("");
//        System.out.println("Number of questions = " + numberOfQuestions);
    }

    public static void createJSON(int size, String context_file, String output) throws IOException, ParseException {

        JSONObject obj = new JSONObject();
        JSONArray data = new JSONArray();

        int sizeOfContext = size;
        int lastCount = 0;

        int numberOfEntries = 0;

        for (Map.Entry<String, ArrayList<String>> entry : contextsHash.entrySet()) {
            String id = entry.getKey();
            String contextString = "";
            int contextSize = 0;
            for (String s : entry.getValue()) {
                contextString += s;
                contextSize++;
                if(contextSize == sizeOfContext)
                    break;
                else
                    contextString += "\n";
            }

            HashMap<String, String> qaPair = questions.get(id);
            String questionString = "";
            String answerString = "";
            for (Map.Entry<String, String> qaPairEntry : qaPair.entrySet()) {
                questionString = qaPairEntry.getKey();
                answerString = qaPairEntry.getValue();
            }

            ArrayList<Integer> answerIndices = new ArrayList<Integer>();
            int startIndex = 0;
            String loswerCaseContext = contextString.toLowerCase();
            while(loswerCaseContext.contains(answerString) && startIndex < loswerCaseContext.length()) {
                int index = loswerCaseContext.indexOf(answerString, startIndex);
                if(index < 0)
                    break;
                startIndex = index + 1;
                answerIndices.add(index);

            }

            ArrayList<Integer> correctAnswerIndices = new ArrayList<Integer>();
            for (Integer answerIndex : answerIndices) {
                char next_char = 'x';
                char prev_char = ' ';
                if(answerIndex + answerString.length() < contextString.length()) {
                    next_char = contextString.charAt(answerIndex + answerString.length());
                }
                if(answerIndex != 0) {
                    prev_char = contextString.charAt(answerIndex - 1);
                }
                if(!Character.isLetter(next_char) && !Character.isLetter(prev_char) ) {
                    correctAnswerIndices.add(answerIndex);
                }
            }

            if(correctAnswerIndices.size() > 0) {
                lastCount++;


                JSONArray answers = new JSONArray();
                for (Integer correctAnswerIndex : correctAnswerIndices) {
                    JSONObject answer = new JSONObject();
                    answer.put("answer_starts", correctAnswerIndex);
                    answer.put("text", answerString);
                    answers.add(answer);
                }

                JSONObject answerObject = new JSONObject();
                answerObject.put("answers", answers);

                answerObject.put("question", questionString);
                answerObject.put("id", id);

                JSONArray qas = new JSONArray();
                qas.add(answerObject);

                JSONObject paragraph = new JSONObject();
                paragraph.put("context", contextString);
                paragraph.put("qas", qas);

                JSONArray paragraphArray = new JSONArray();
                paragraphArray.add(paragraph);

                JSONObject context = new JSONObject();
                context.put("title", id);
                context.put("paragraphs", paragraphArray);

                data.add(context);
//                numberOfEntries++;
//
//                if(numberOfEntries == 100)
//                    break;
            }
        }

        obj.put("data", data);

        String length = "";
        String file = "";
        if(context_file.contains("long")) {
            length = "long";
        } else {
            length = "short";
        }

        if(context_file.contains("dev")) {
            file = "dev";
        } else if(context_file.contains("train")) {
            file = "train";
        } else {
            file = "test";
        }

        FileWriter fw = new FileWriter(output + "/" + size + "_" + "QUASAR-T" + "_" + length + "_" + file + ".json");
        fw.append(obj.toJSONString());
        fw.close();
    }

    public static void main(String[] args) throws IOException, ParseException {
        readJSON(args[0], args[1]);
        createJSON(Integer.parseInt(args[3]), args[0], args[2]);
    }
}
