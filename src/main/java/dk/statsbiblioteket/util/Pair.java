package dk.statsbiblioteket.util;

/**
 * Created by IntelliJ IDEA.
 * User: abr
 * Date: 11/1/11
 * Time: 12:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class Pair<L,R> {

    L left;
    R right;

    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public L getLeft() {
        return left;
    }

    public R getRight() {
        return right;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Pair)) {
            return false;
        }

        Pair pair = (Pair) o;

        if (left != null ? !left.equals(pair.left) : pair.left != null) {
            return false;
        }
        if (right != null ? !right.equals(pair.right) : pair.right != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = left != null ? left.hashCode() : 0;
        result = 31 * result + (right != null ? right.hashCode() : 0);
        return result;
    }
}
