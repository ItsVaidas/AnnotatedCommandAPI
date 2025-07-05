package lt.itsvaidas.annotationCommandAPI.dtos;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.IntStream;

public class Sentence implements CharSequence {

    private final @Nullable String sentence;

    public Sentence(@Nullable String sentence) {
        this.sentence = sentence;
    }

    @Override
    public int length() {
        return sentence != null ? sentence.length() : 0;
    }

    @Override
    public char charAt(int index) {
        return sentence != null ? sentence.charAt(index) : '\0';
    }

    @Override
    public boolean isEmpty() {
        return sentence == null || sentence.isEmpty();
    }

    @Override
    public @NotNull CharSequence subSequence(int start, int end) {
        return sentence != null ? sentence.subSequence(start, end) : "";
    }

    @Override
    public @NotNull String toString() {
        return sentence != null ? sentence : "";
    }

    @Override
    public @NotNull IntStream chars() {
        return sentence != null ? sentence.chars() : IntStream.empty();
    }

    @Override
    public @NotNull IntStream codePoints() {
        return sentence != null ? sentence.codePoints() : IntStream.empty();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null && sentence == null) return true;
        if (obj instanceof String string) {
            return sentence != null && sentence.equals(string);
        } else if (obj instanceof Sentence otherSentence) {
            return sentence != null && sentence.equals(otherSentence.sentence);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return sentence != null ? sentence.hashCode() : 0;
    }

    public String[] split(String regex) {
        return sentence != null ? sentence.split(regex) : new String[0];
    }
}
