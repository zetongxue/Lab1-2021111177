package org.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class QueryBridgeWords {
  private HashMap<String, Integer> word2id = null;
  private String[] id2word = null;
  private AdjacencyList graph = null;
  
  public void init(){
    String[] wordList = readFile("C:\\Users\\35009\\Desktop\\SE\\exp\\lab1\\text.txt");
    this.word2id = encodeWords(wordList);
    this.id2word = getReversedWordDict(this.word2id);
    this.graph = buildGraph(wordList, this.word2id);
  }

  private static HashMap<String, Integer> encodeWords(String[] wordList) {
    // 将单词列表进行编码，构建词典
    AtomicInteger count = new AtomicInteger();
    HashMap<String, Integer> wordDict = new HashMap<>();
    for (String word : wordList) {
      wordDict.computeIfAbsent(word, k -> count.getAndIncrement());
    }
    return wordDict;
  }

  private static String[] getReversedWordDict(HashMap<String, Integer> wordDict) {
    // 创建一个数组，将哈希表的映射反过来
    String[] reversedWordDict = new String[wordDict.size()];
    String key;
    int value;
    for (Map.Entry<String, Integer> entry : wordDict.entrySet()) {
      key = entry.getKey();
      value = entry.getValue();
      reversedWordDict[value] = key;
    }
    return reversedWordDict;
  }

  private static AdjacencyList buildGraph(String[] wordList, HashMap<String, Integer> wordDict) {
    // 根据单词列表和词典构建图
    int n = wordDict.size();
    int m = wordList.length;
    AdjacencyList graph = new AdjacencyList(n);
    for (int i = 0; i < m - 1; i++) {
      graph.addEdge(wordDict.get(wordList[i]), wordDict.get(wordList[i + 1]));
    }
    return graph;
  }

  private String[] readFile(String fileName) {
    // 读取文件，返回单词列表
    StringBuilder sb = new StringBuilder();
    try (FileReader fileReader = new FileReader(fileName);
         BufferedReader bufferedReader = new BufferedReader(fileReader)) {
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        // 使用正则表达式替换非字母字符为空格，并替换为小写
        String cleanedLine = line.replaceAll("[^a-zA-Z]+", " ").toLowerCase();
        sb.append(cleanedLine).append(" ");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return sb.toString().split("\\s+");
  }

  public String queryBridgeWords(String word1, String word2) {
    int id1 = 0;
    int id2 = 0;
    int errorNo = 0;
    try {
      id1 = word2id.get(word1);
    } catch (Exception e) {
      errorNo = 1;
    }
    try {
      id2 = word2id.get(word2);
    } catch (Exception e) {
      if (errorNo == 1) {
        errorNo = 3;
      } else {
        errorNo = 2;
      }
    }
    String errorInfo = null;
    switch (errorNo) {
      case 1:
        errorInfo = "No \"" + word1 + "\" in the graph!";
        break;
      case 2:
        errorInfo = "No \"" + word2 + "\" in the graph!";
        break;
      case 3:
        errorInfo = "No \"" + word1 + "\" and \"" + word2 + "\" in the graph!";
        break;
      case 0:
      default:
        break;
    }
    if (errorNo != 0) {
      return errorInfo;
    }
    // 获取桥接词id
    List<Integer> resultId = new ArrayList<>();
    for (Edge edge : graph.graph.get(id1)) {
      int r = graph.findEdge(edge.destination, id2);
      if (r != 0) {
        resultId.add(edge.destination);
      }
    }
    StringBuilder resultText = new StringBuilder(
            "The bridge words from \"" + word1 + "\" to \"" + word2 + "\" ");
    // 根据桥接词内容生成返回文本
    if (resultId.isEmpty()) {
      // 如果没有，则报，返回null
      return "No bridge words from \"" + word1 + "\" to \"" + word2 + "\" !";
    } else if (resultId.size() == 1) {
      // 有1个单词
      resultText.append("is: ").append(id2word[resultId.get(0)]).append(".");
    } else {
      // 有多个单词
      resultText.append("are: ");
      for (int re : resultId.subList(0, resultId.size() - 1)) {
        resultText.append(id2word[re]).append(", ");
      }
      resultText.append("and ").append(id2word[resultId.get(resultId.size() - 1)]).append(".");
    }
    return resultText.toString();
  }
}