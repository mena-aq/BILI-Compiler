package BottomUpParser.src;

import java.util.*;

public class Tree {

    // tree node
    public static class TreeNode {
        private final String label;
        private final List<TreeNode> children;
        private final boolean isTerminal;
        private final int id;
        private int reductionStep;

        public TreeNode(String label, boolean isTerminal, int id) {
            this(label, isTerminal, id, -1);
        }

        public TreeNode(String label, boolean isTerminal, int id, int reductionStep) {
            this.label = label;
            this.isTerminal = isTerminal;
            this.children = new ArrayList<>();
            this.id = id;
            this.reductionStep = reductionStep;
        }

        public void addChild(TreeNode child) {
            children.add(child);
        }

        public String getLabel() { return label; }
        public List<TreeNode> getChildren() { return children; }
        public boolean isTerminal() { return isTerminal; }
        public int getId() { return id; }
        public int getReductionStep() { return reductionStep; }
        public void setReductionStep(int step) { this.reductionStep = step; }
    }

    // tree
    private TreeNode root;
    private int nodeCounter = 0;
    private int reductionCounter = 0;

    // Stack that holds TreeNode objects - mirrors the parser's symbol stack
    private final Stack<TreeNode> treeStack;

    // Track reduction steps for display
    private final List<ReductionStep> reductionSteps;

    /**
     * Represents a single reduction step in the parse tree construction
     */
    public static class ReductionStep {
        public final int stepNumber;
        public final String production;
        public final List<String> rhsSymbols;
        public final String lhs;
        public final TreeNode createdNode;

        public ReductionStep(int stepNumber, String production, List<String> rhsSymbols,
                             String lhs, TreeNode createdNode) {
            this.stepNumber = stepNumber;
            this.production = production;
            this.rhsSymbols = rhsSymbols;
            this.lhs = lhs;
            this.createdNode = createdNode;
        }

        @Override
        public String toString() {
            return String.format("Step %d: %s → %s", stepNumber, lhs,
                    String.join(" ", rhsSymbols));
        }
    }

    public Tree() {
        this.treeStack = new Stack<>();
        this.reductionSteps = new ArrayList<>();
    }

    /**
     * Initialize the tree with the start symbol
     * Called at the beginning of parsing
     */
    public void init(String startSymbol) {
        root = new TreeNode(startSymbol, false, nodeCounter++);
        treeStack.clear();
        // Don't push anything yet - we'll build bottom-up from leaves
        reductionCounter = 0;
        reductionSteps.clear();
    }

    /**
     * Called during a shift action.
     * Creates a leaf node for the terminal and pushes it onto the tree stack.
     *
     * @param terminal the terminal symbol being shifted
     */
    public void shift(String terminal) {
        TreeNode leafNode = new TreeNode(terminal, true, nodeCounter++);
        treeStack.push(leafNode);
    }

    /**
     * Called during a reduce action.
     * This is where the tree is built bottom-up:
     * 1. Pop |rhs| nodes from the tree stack (these become children)
     * 2. Create a new node for the LHS
     * 3. Attach the popped nodes as children to the new node
     * 4. Push the new node back onto the stack
     *
     * @param lhs the left-hand side non-terminal
     * @param rhs the right-hand side symbols (production body)
     * @param production the full production string (for display)
     * @return the newly created TreeNode
     */
    public TreeNode reduce(String lhs, List<String> rhs, String production) {
        reductionCounter++;

        List<TreeNode> children = new ArrayList<>();

        // Handle epsilon production
        if (rhs.isEmpty() || (rhs.size() == 1 && rhs.get(0).equals("@"))) {
            // Epsilon production - create node with epsilon child
            TreeNode lhsNode = new TreeNode(lhs, false, nodeCounter++, reductionCounter);
            TreeNode epsilonNode = new TreeNode("@", true, nodeCounter++, reductionCounter);
            lhsNode.addChild(epsilonNode);
            treeStack.push(lhsNode);

            reductionSteps.add(new ReductionStep(reductionCounter, production,
                    Collections.singletonList("@"), lhs, lhsNode));
            return lhsNode;
        }

        // Pop |rhs| nodes from the tree stack
        // IMPORTANT: The stack has symbols in the order they were shifted
        // We need to pop and reverse to maintain left-to-right order
        for (int i = 0; i < rhs.size(); i++) {
            if (!treeStack.isEmpty()) {
                TreeNode child = treeStack.pop();
                children.add(0, child); // Insert at beginning to maintain order
            }
        }

        // Create new node for LHS
        TreeNode lhsNode = new TreeNode(lhs, false, nodeCounter++, reductionCounter);

        // Add children (now in correct left-to-right order)
        for (TreeNode child : children) {
            lhsNode.addChild(child);
        }

        // Push the new node onto the stack
        treeStack.push(lhsNode);

        // Record this reduction step
        reductionSteps.add(new ReductionStep(reductionCounter, production, rhs, lhs, lhsNode));

        return lhsNode;
    }

    /**
     * Called when parsing completes successfully.
     * The root should be the only node left on the stack.
     *
     * @return true if the tree is valid
     */
    public boolean finalizeTree() {
        if (treeStack.size() == 1) {
            root = treeStack.pop();
            return true;
        }
        return false;
    }

    /**
     * Clear the tree (reset for a new parse)
     */
    public void clear() {
        treeStack.clear();
        reductionSteps.clear();
        nodeCounter = 0;
        reductionCounter = 0;
        root = null;
    }

    /**
     * Get the current size of the tree stack (for debugging)
     */
    public int getStackSize() {
        return treeStack.size();
    }


    /**
     * Generates a Graphviz DOT representation of the parse tree.
     */
    public String toDot() {
        if (root == null) return "digraph ParseTree { }";

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

        if (node.isTerminal()) {
            if (node.getLabel().equals("@")) {
                sb.append(String.format("  %s [label=\"ε\" shape=box style=dashed];\n", nodeId));
            } else {
                sb.append(String.format("  %s [label=\"%s\" shape=box style=filled fillcolor=lightblue];\n", nodeId, label));
            }
        } else {
            // Non-terminal - no step footnote
            sb.append(String.format("  %s [label=\"%s\" shape=ellipse style=filled fillcolor=lightyellow];\n",
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

    /**
     * Prints the tree in a readable text format (preorder traversal)
     */
    public void printTree() {
        if (root == null) {
            System.out.println("No parse tree available.");
            return;
        }
        System.out.println("\n=== Parse Tree ===");
        printTreeRecursive(root, 0);
        System.out.println();
    }

    private void printTreeRecursive(TreeNode node, int indent) {
        String indentStr = "  ".repeat(indent);
        String nodeInfo = node.getLabel();

        if (!node.isTerminal() && node.getReductionStep() > 0) {
            nodeInfo += " [step " + node.getReductionStep() + "]";
        }

        // Add epsilon symbol for clarity
        if (node.getLabel().equals("@")) {
            nodeInfo = "ε";
        }

        System.out.println(indentStr + "├─ " + nodeInfo);

        for (int i = 0; i < node.getChildren().size(); i++) {
            TreeNode child = node.getChildren().get(i);
            printTreeRecursiveWithPrefix(child, indent + 1, i == node.getChildren().size() - 1);
        }
    }

    private void printTreeRecursiveWithPrefix(TreeNode node, int indent, boolean isLast) {
        String indentStr = "  ".repeat(indent - 1) + (isLast ? "   " : "│  ");
        String prefix = indentStr + (isLast ? "└─ " : "├─ ");

        String nodeInfo = node.getLabel();
        if (!node.isTerminal() && node.getReductionStep() > 0) {
            nodeInfo += " [step " + node.getReductionStep() + "]";
        }
        if (node.getLabel().equals("@")) {
            nodeInfo = "ε";
        }

        System.out.println(prefix + nodeInfo);

        for (int i = 0; i < node.getChildren().size(); i++) {
            TreeNode child = node.getChildren().get(i);
            printTreeRecursiveWithPrefix(child, indent + 1, i == node.getChildren().size() - 1);
        }
    }

    /**
     * Prints all reduction steps that built the tree
     */
    public void printReductionSteps() {
        if (reductionSteps.isEmpty()) {
            System.out.println("No reduction steps recorded.");
            return;
        }

        System.out.println("\n=== Reduction Steps (Building Tree Bottom-Up) ===");
        for (ReductionStep step : reductionSteps) {
            System.out.printf("Step %2d: %s → %s%n",
                    step.stepNumber,
                    step.lhs,
                    String.join(" ", step.rhsSymbols));
        }
        System.out.println();
    }

    /**
     * Print everything (tree + reduction steps)
     */
    public void printFull() {
        printReductionSteps();
        printTree();
    }

    public TreeNode getRoot() { return root; }
    public List<ReductionStep> getReductionSteps() { return reductionSteps; }
    public int getNodeCount() { return nodeCounter; }

    // Simple Stack implementation for TreeNode
    private static class Stack<T> {
        private final List<T> elements = new ArrayList<>();

        public void push(T item) { elements.add(item); }
        public T pop() {
            if (elements.isEmpty()) return null;
            return elements.remove(elements.size() - 1);
        }
        public T top() {
            if (elements.isEmpty()) return null;
            return elements.get(elements.size() - 1);
        }
        public boolean isEmpty() { return elements.isEmpty(); }
        public int size() { return elements.size(); }
        public void clear() { elements.clear(); }
        public List<T> getContents() { return new ArrayList<>(elements); }
    }
}