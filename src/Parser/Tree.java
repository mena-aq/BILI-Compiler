package Parser;

import java.util.*;

public class Tree {

   // tree node
    public static class TreeNode {
        private final String label;
        private final List<TreeNode> children;
        private final boolean isTerminal;
        private final int id; // unique id for DOT node naming

        public TreeNode(String label, boolean isTerminal, int id) {
            this.label = label;
            this.isTerminal = isTerminal;
            this.children = new ArrayList<>();
            this.id = id;
        }

        public void addChild(TreeNode child) {
            children.add(child);
        }

        public String getLabel() { return label; }
        public List<TreeNode> getChildren() { return children; }
        public boolean isTerminal() { return isTerminal; }
        public int getId() { return id; }
    }

    // tree

    private TreeNode root;
    private int nodeCounter = 0;

    // Mirrors the parser stack — each entry is the TreeNode
    // corresponding to the symbol at that stack position
    private final java.util.Stack<TreeNode> nodeStack = new java.util.Stack<>();

    public TreeNode init(String startSymbol) {
        root = new TreeNode(startSymbol, false, nodeCounter++);
        nodeStack.push(root);
        return root;
    }

    /**
     * Called when NT X is expanded with production X → Y1 Y2 ... Yn.
     * Pops X's node from nodeStack, creates child nodes for each Yi,
     * pushes them in reverse order (so Y1 is processed first).
     */
    public void expand(List<String> production, Set<String> nonTerminals) {
        TreeNode parent = nodeStack.pop();

        if (production.isEmpty() ||
                (production.size() == 1 && production.get(0).equals("@"))) {
            // Epsilon production — add epsilon leaf
            TreeNode epsNode = new TreeNode("@", true, nodeCounter++);
            parent.addChild(epsNode);
            return;
        }

        // Create child nodes and attach to parent
        List<TreeNode> childNodes = new ArrayList<>();
        for (String symbol : production) {
            boolean isTerminal = !nonTerminals.contains(symbol);
            TreeNode child = new TreeNode(symbol, isTerminal, nodeCounter++);
            parent.addChild(child);
            childNodes.add(child);
        }

        // Push in reverse so the leftmost symbol is on top
        for (int i = childNodes.size() - 1; i >= 0; i--) {
            nodeStack.push(childNodes.get(i));
        }
    }

    /**
     * Called when terminal a is matched — pops the terminal node from nodeStack.
     */
    public void match() {
        if (!nodeStack.isEmpty()) nodeStack.pop();
    }

    // error recovery
    /**
     * Called during error recovery when a node is popped from the parser stack.
     * Marks the node as an error node so it shows differently in the DOT output.
     */
    public void popError() {
        if (!nodeStack.isEmpty()) {
            TreeNode node = nodeStack.pop();
            // Add a special error marker child
            TreeNode errorNode = new TreeNode("✗", true, nodeCounter++);
            node.addChild(errorNode);
        }
    }

    /**
     * Called during error recovery when input is scanned (skipped).
     * Adds a skipped-token node under the current stack top's node.
     */
    public void skipToken(String token) {
        System.out.println("DEBUG skipToken(" + token + ") nodeStack size=" + nodeStack.size());
        for (int i = nodeStack.size() - 1; i >= 0; i--) {
            System.out.println("  nodeStack[" + i + "] = " + nodeStack.get(i).getLabel());
        }
        // Create a standalone skipped-token node
        // attach it to the parent of the current stack top, not the top itself
        if (nodeStack.size() >= 2) {
            // peek at the node below the top — that's the parent context
            TreeNode context = nodeStack.get(nodeStack.size() - 2);
            TreeNode skipped = new TreeNode("?" + token, true, nodeCounter++);
            context.addChild(skipped);
        } else if (!nodeStack.isEmpty()) {
            // only root context available
            TreeNode skipped = new TreeNode("?" + token, true, nodeCounter++);
            nodeStack.peek().addChild(skipped);
        }
    }


    // output 1 : graphviz dot format

    /**
     * Generates a Graphviz DOT representation of the parse tree.
     * Terminals are box-shaped, non-terminals are ellipses.
     * Error nodes are red, skipped tokens are orange.
     */
    public String toDot() {
        if (root == null) return "digraph ParseTree {}";

        StringBuilder sb = new StringBuilder();
        sb.append("digraph ParseTree {\n");
        sb.append("  node [fontname=\"Helvetica\"];\n");
        sb.append("  edge [arrowhead=none];\n\n");

        // Traverse tree and emit nodes + edges
        buildDot(root, sb);

        sb.append("}\n");
        return sb.toString();
    }

    private void buildDot(TreeNode node, StringBuilder sb) {
        String nodeId = "n" + node.getId();
        String label = node.getLabel().replace("\"", "\\\"");

        if (node.getLabel().equals("✗")) {
            // Error marker — red
            sb.append(String.format(
                    "  %s [label=\"%s\" shape=circle style=filled fillcolor=red fontcolor=white];\n",
                    nodeId, label));
        } else if (node.getLabel().startsWith("?")) {
            // Skipped token — orange
            String skippedLabel = node.getLabel().substring(1);
            sb.append(String.format(
                    "  %s [label=\"%s\\n(skipped)\" shape=box style=filled fillcolor=orange fontcolor=black];\n",
                    nodeId, skippedLabel));
        } else if (node.isTerminal()) {
            // Terminal — box
            sb.append(String.format(
                    "  %s [label=\"%s\" shape=box style=filled fillcolor=lightblue];\n",
                    nodeId, label));
        } else {
            // Non-terminal — ellipse
            sb.append(String.format(
                    "  %s [label=\"%s\" shape=ellipse style=filled fillcolor=lightyellow];\n",
                    nodeId, label));
        }

        for (TreeNode child : node.getChildren()) {
            sb.append(String.format("  n%d -> n%d;\n", node.getId(), child.getId()));
            buildDot(child, sb);
        }
    }

    public void printDot() {
        System.out.println(toDot());
    }

    // output 2 : preorder traversal
    public void preOrder(TreeNode node) {
        if (node == null) return;

        System.out.println(node.getLabel()); // visit root first

        for (TreeNode child : node.getChildren()) {
            preOrder(child); // then each child left to right
        }
    }


    public TreeNode getRoot() { return root; }
}