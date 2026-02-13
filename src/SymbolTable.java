import java.util.*;

public class SymbolTable {

    public static class Symbol {
        
        public final String name; // identifier name
        public final int firstLine;
        public final int firstColumn;
        public int frequency;
        
        public Symbol(String name, int line, int column) {
            this.name = name;
            this.firstLine = line;
            this.firstColumn = column;
            this.frequency = 1;
        }
    }

    // list of symbols
    // hash map for quick lookup by identifier name to increment frequency
    // use linkedhashmap to preserve insertion order (appear in order of occurence in source code)
    private final Map<String, Symbol> table = new LinkedHashMap<>();

    public void add(String identifier, int line, int column) {
        Symbol e = table.get(identifier);
        if (e == null) {
            e = new Symbol(identifier, line, column);
            table.put(identifier, e);
        } else {
            // if alr in table, just increment frequency
            e.frequency++;
        }
    }

    public Collection<Symbol> entries() {
        return table.values();
    }
}

