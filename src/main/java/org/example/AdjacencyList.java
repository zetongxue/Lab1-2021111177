package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class AdjacencyList {
    private static final Logger logger = LogManager.getLogger(AdjacencyList.class);
    List<LinkedList<Edge>> graph = new ArrayList<>();
    int size;   // 节点数量，节点标号范围为0 ~ size-1

    // 构造函数
    public AdjacencyList(int vertices) {
        this.size = vertices;
        for (int i = 0; i < vertices; ++i) {
            this.graph.add(new LinkedList<>());
        }
    }

    private boolean isInvalidNode(int node){
        return node < 0 || node >= this.size;
    }

    // 边查找函数，返回边的权重，如果不存在则返回0
    public int findEdge(int source, int destination){
        if (isInvalidNode(source) || isInvalidNode(destination)) {
            return 0;
        }
        LinkedList<Edge> p = this.graph.get(source);
        for (Edge edge : p) {
            if (edge.destination == destination) {
                return edge.weight;
            }
        }
        return 0;
    }

    // 添加边
    public void addEdge(int source, int destination) {
        if (isInvalidNode(source) || isInvalidNode(destination)) {
            return;
        }
        if (findEdge(source, destination) == 0){
            // 未找到，则添加边，权重为1
            graph.get(source).add(new Edge(destination, 1));
        } else {
            // 找到，则权重加1
            for (Edge edge : graph.get(source)) {
                if (edge.destination == destination) {
                    edge.weight++;
                    break;
                }
            }
        }
    }

    // 打印带权邻接表
    public void printGraph() {
        StringBuilder sb = new StringBuilder();
        sb.append("Adjacency List:");
        for (int i = 0; i < this.size; ++i){
            sb.append(String.format("%n[%d] => ", i));
            for (Edge edge : this.graph.get(i)) {
                sb.append(String.format("(%d, %d) ", edge.destination, edge.weight));
            }
        }
        logger.info(sb);
    }

    public static void main(String[] args) {
        int vertices = 5;
        AdjacencyList graph = new AdjacencyList(vertices);
        graph.addEdge(1, 2);
        graph.addEdge(1, 2);
        graph.addEdge(1, 4);
        graph.addEdge(2, 3);
        graph.addEdge(2, 5);
        graph.addEdge(3, 4);
        graph.printGraph();
    }
}
