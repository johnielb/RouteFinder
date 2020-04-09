public class FringeElement implements Comparable {
    private Node node, prev;
    private double estCost, costSoFar;

    public FringeElement(Node node, Node prev, double estCost, double costSoFar) {
        this.node = node;
        this.prev = prev;
        this.estCost = estCost;
        this.costSoFar = costSoFar;
    }

    public double getEstCost() {
        return estCost;
    }

    public double getCostSoFar() { return costSoFar; }

    public Node getNode() {
        return node;
    }

    public Node getPrev() {
        return prev;
    }

    @Override
    public int compareTo(Object o) {
        if (o.getClass() == getClass()) {
            FringeElement that = (FringeElement) o;
            if (that.estCost > this.estCost) return -1;
            else if (that.estCost < this.estCost) return 1;
        }
        return 0;
    }
}
