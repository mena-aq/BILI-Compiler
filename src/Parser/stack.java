package Parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Task 2.2: Stack Implementation for LL(1) Parser
 * Custom stack implementation using ArrayList (can also use LinkedList)
 * Initialized with $ (bottom marker) and start symbol on top
 */
public class stack {
    private List<String> stack;

    /**
     * Constructor - initializes empty stack
     */
    public stack() {
        this.stack = new ArrayList<>();
    }

    /**
     * Initialize stack with $ (bottom marker) and push start symbol
     *
     * @param startSymbol The grammar's start symbol
     */
    public void initialize(String startSymbol) {
        stack.clear();
        // Push $ first (bottom marker)
        stack.add("$");
        // Push start symbol on top
        stack.add(startSymbol);
        System.out.println("Stack initialized: " + this);
    }

    /**
     * Push a symbol onto the stack
     *
     * @param symbol Symbol to push
     */
    public void push(String symbol) {
        stack.add(symbol);
    }

    /**
     * Push multiple symbols in reverse order (for production expansion)
     *
     * @param symbols List of symbols to push
     */
    public void pushAll(List<String> symbols) {
        // Push in reverse order so the first symbol ends up on top
        for (int i = symbols.size() - 1; i >= 0; i--) {
            push(symbols.get(i));
        }
    }

    /**
     * Pop the top symbol from the stack
     *
     * @return The popped symbol
     * @throws IllegalStateException if stack is empty
     */
    public String pop() {
        if (isEmpty()) {
            throw new IllegalStateException("Cannot pop from empty stack");
        }
        return stack.remove(stack.size() - 1);
    }

    /**
     * View the top symbol without removing it
     *
     * @return The top symbol
     * @throws IllegalStateException if stack is empty
     */
    public String top() {
        if (isEmpty()) {
            throw new IllegalStateException("Stack is empty");
        }
        return stack.get(stack.size() - 1);
    }

    /**
     * Check if stack is empty
     *
     * @return true if stack is empty, false otherwise
     */
    public boolean isEmpty() {
        return stack.isEmpty();
    }

    /**
     * Get the size of the stack
     *
     * @return number of elements in stack
     */
    public int size() {
        return stack.size();
    }

    /**
     * Clear the stack
     */
    public void clear() {
        stack.clear();
    }

    /**
     * Get a copy of the stack contents (from bottom to top)
     *
     * @return List of stack elements
     */
    public List<String> getContents() {
        return new ArrayList<>(stack);
    }

    /**
     * Check if stack contains only $ (bottom marker)
     *
     * @return true if stack only has $, false otherwise
     */
    public boolean onlyDollar() {
        return stack.size() == 1 && stack.get(0).equals("$");
    }

    /**
     * Get stack contents as a string (bottom to top)
     *
     * @return String representation of stack
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < stack.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(stack.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Get stack contents as a string formatted for parsing steps (bottom to top, space-separated)
     *
     * @return Formatted string for display
     */
    public String toDisplayString() {
        if (stack.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        // Show from bottom to top (left to right)
        for (int i = 0; i < stack.size(); i++) {
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(stack.get(i));
        }
        return sb.toString();
    }

    /**
     * Create a deep copy of the stack
     *
     * @return New stack with same contents
     */
    public stack copy() {
        stack newStack = new stack();
        newStack.stack = new ArrayList<>(this.stack);
        return newStack;
    }
}