package ru.yandex.practicum;

import java.util.*;

public class WordleDictionary {
    private final List<String> words;
    private final Random random = new Random();

    public WordleDictionary(List<String> words) {
        this.words = new ArrayList<>(words);
    }

    public List<String> getWords() {
        return Collections.unmodifiableList(words);
    }

    public String getRandomWord() {
        if (words.isEmpty()) {
            throw new IllegalStateException("Словарь пуст");
        }
        return words.get(random.nextInt(words.size()));
    }

    public boolean contains(String word) {
        return words.contains(normalizeWord(word));
    }

    public List<String> findPossibleWords(Set<Character> correctLetters,
                                         Set<Character> wrongLetters,
                                         Map<Integer, Character> correctPositions,
                                         Map<Integer, Set<Character>> wrongPositions) {
        List<String> possibleWords = new ArrayList<>();

        for (String word : words) {
            if (isWordPossible(word, correctLetters, wrongLetters, correctPositions, wrongPositions)) {
                possibleWords.add(word);
            }
        }

        return possibleWords;
    }

    private boolean isWordPossible(String word,
                                  Set<Character> correctLetters,
                                  Set<Character> wrongLetters,
                                  Map<Integer, Character> correctPositions,
                                  Map<Integer, Set<Character>> wrongPositions) {
        // Проверяем обязательные буквы
        for (Character letter : correctLetters) {
            if (word.indexOf(letter) == -1) {
                return false;
            }
        }

        // Проверяем отсутствие неправильных букв
        for (Character letter : wrongLetters) {
            if (word.indexOf(letter) != -1) {
                return false;
            }
        }

        // Проверяем правильные позиции
        for (Map.Entry<Integer, Character> entry : correctPositions.entrySet()) {
            int position = entry.getKey();
            char expectedChar = entry.getValue();
            if (word.charAt(position) != expectedChar) {
                return false;
            }
        }

        // Проверяем неправильные позиции
        for (Map.Entry<Integer, Set<Character>> entry : wrongPositions.entrySet()) {
            int position = entry.getKey();
            Set<Character> forbiddenChars = entry.getValue();
            if (forbiddenChars.contains(word.charAt(position))) {
                return false;
            }
        }

        return true;
    }

    private String normalizeWord(String word) {
        return word.toLowerCase().replace('ё', 'е');
    }

    public static String analyzeWord(String guess, String answer) {
        if (guess.length() != 5 || answer.length() != 5) {
            throw new IllegalArgumentException("Слова должны быть длиной 5 букв");
        }

        char[] result = new char[5];
        char[] guessChars = guess.toCharArray();
        char[] answerChars = answer.toCharArray();

        // Сначала отмечаем правильные позиции
        for (int i = 0; i < 5; i++) {
            if (guessChars[i] == answerChars[i]) {
                result[i] = '+';
                answerChars[i] = ' '; // Помечаем как использованную
            }
        }

        // Затем отмечаем буквы в неправильных позициях
        for (int i = 0; i < 5; i++) {
            if (result[i] == '+') continue;

            boolean found = false;
            for (int j = 0; j < 5; j++) {
                if (answerChars[j] == guessChars[i]) {
                    result[i] = '^';
                    answerChars[j] = ' ';
                    found = true;
                    break;
                }
            }

            if (!found) {
                result[i] = '-';
            }
        }

        return new String(result);
    }
}
