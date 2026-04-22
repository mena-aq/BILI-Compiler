package BottomUpParser.src;

import java.util.ArrayList;
import java.util.List;

/**
 * Stack Implementation for SLR Parser
 * Generic stack using ArrayList for storing states and symbols
 */
public class Stack {
    private List<String> stack;

    /**
     * Constructor - initializes empty stack
     */
    public Stack() {
        this.stack = new ArrayList<>();
    }

    /**
     * Push a symbol/state onto the stack
     *
     * @param item Symbol or state to push
     */
    public void push(String item) {
        stack.add(item);
    }

    /**
     * Pop the top symbol/state from the stack
     *
     * @return The popped symbol/state
     * @throws IllegalStateException if stack is empty
     */
    public String pop() {
        if (isEmpty()) {
            throw new IllegalStateException("Cannot pop from empty stack");
        }
        return stack.remove(stack.size() - 1);
    }

    /**
     * View the top symbol/state without removing it
     *
     * @return The top symbol/state
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
     * Get stack contents as a string (bottom to top, space-separated)
     *
     * @return String representation of stack
     */
    @Override
    public String toString() {
        return String.join(" ", stack);
    }

    /**
     * Create a deep copy of the stack
     *
     * @return New stack with same contents
     */
    public Stack copy() {
        Stack newStack = new Stack();
        newStack.stack = new ArrayList<>(this.stack);
        return newStack;
    }
}
