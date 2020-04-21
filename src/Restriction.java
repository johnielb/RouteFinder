public class Restriction {
    Road prevRd;
    Road nextRd;
    Node prev;
    Node curr;
    Node next;

    public Restriction(Road prevRd, Road nextRd, Node prev, Node curr, Node next) {
        this.prevRd = prevRd;
        this.nextRd = nextRd;
        this.prev = prev;
        this.curr = curr;
        this.next = next;
    }

    public boolean notAllowed(Node prev, Node curr, Node next, Road nextRd) {
        boolean isPrevRd = false;
        if (prev != null) {
            for (Segment s : prev.segments) {
                if (s.start.equals(curr) || s.end.equals(curr)) isPrevRd = prevRd.equals(s.road);
            }
            return prev.equals(this.prev) && next.equals(this.next) && nextRd.equals(this.nextRd) && isPrevRd;
        }
        return false;
    }
}
