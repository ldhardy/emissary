package emissary.test.util;

import java.util.Map.Entry;
import java.util.Objects;

/**
 * Necessary to have an entry type that is sortable so the sets can be TreeSets and produce the same ordering each time.
 */
public class SimpleUnitTestEntry implements Entry<String, String>, Comparable<Object> {

    private String key;
    private String value;

    public SimpleUnitTestEntry(String k, String v) {
        this.key = k;
        this.value = v;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String setValue(String value) {
        String old = this.value;
        this.value = value;
        return old;
    }

    @Override
    public int compareTo(Object o) {
        if (equals(o)) {
            return 0;
        }
        SimpleUnitTestEntry other = (SimpleUnitTestEntry) o;
        // sort by key first
        if (key.compareTo(other.getKey()) != 0) {
            return key.compareTo(other.getKey());
        }
        // then by value
        return value.compareTo(other.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SimpleUnitTestEntry)) {
            return false;
        }
        SimpleUnitTestEntry other = (SimpleUnitTestEntry) obj;
        return Objects.equals(key, other.key) && Objects.equals(value, other.value);
    }


}
