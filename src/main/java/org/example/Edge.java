package org.example;
import java.util.Objects;

public class Edge {
    int destination;
    int weight;

    public Edge(int destination, int weight) {
        this.destination = destination;
        this.weight = weight;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Edge edge = (Edge) obj;
        return destination == edge.destination && weight == edge.weight;
    }

    @Override
    public int hashCode() {
        return Objects.hash(destination, weight);
    }

}
