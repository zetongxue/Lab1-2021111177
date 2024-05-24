package org.example;

import java.util.*;
import javafx.scene.control.TextField;


public class RandomWalkThread implements Runnable{
    private final Map<String, Integer> word2id;
    private final String[] id2word;
    private final List<LinkedList<Edge>> graph;
    private final TextField txtRandomWalkResult;
    private final Random random = new Random();
    private Set<Edge> edgeSet = new HashSet<>();

    public RandomWalkThread(Map<String, Integer> word2id, String[] id2word, List<LinkedList<Edge>> graph, TextField txtRandomWalkResult) {
        this.word2id = word2id;
        this.id2word = id2word;
        this.graph = graph;
        this.txtRandomWalkResult = txtRandomWalkResult;
    }
    @Override
    public void run() {
        StringBuilder randomWalkSentence = new StringBuilder();
        int pointer;
        pointer = random.nextInt(word2id.size());
        randomWalkSentence.append(id2word[pointer]);
        while(!Thread.interrupted()){
            LinkedList<Edge> head = graph.get(pointer);
            if (head.isEmpty())
                break;
            else {
                int r = random.nextInt(head.size()); 
                Edge edge = new Edge(pointer, head.get(r).destination);
                pointer = head.get(r).destination;
                if (!edgeSet.contains(edge)){
                    randomWalkSentence.append(" ").append(id2word[pointer]);
                    edgeSet.add(edge);
                }
                else {
                    break;
                }
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                txtRandomWalkResult.setText(randomWalkSentence.toString());
                Thread.currentThread().interrupt();
                break;
            }
        }
        randomWalkSentence.append(".");
        txtRandomWalkResult.setText(randomWalkSentence.toString());
    }
}

