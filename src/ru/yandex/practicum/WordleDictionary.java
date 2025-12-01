package ru.yandex.practicum;

import java.util.*;

public class WordleDictionary {
    private final List<String> words;
    private final Random random = new Random();

    public WordleDictionary(List<String> words) {
        // Фильтруем только 5-буквенные слова при создании словаря
        this.words = new ArrayList<>();
        for (String word : words) {
            String normalized = normalizeWord(word);
            if (normalized.length() == 5) {
                this.words.add(normalized);
            }
        }
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
        String normalized = normalizeWord(word);
        return words.contains(normalized);
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
            if (position < word.length() && word.charAt(position) != expectedChar) {
                return false;
            }
        }

        // Проверяем неправильные позиции
        for (Map.Entry<Integer, Set<Character>> entry : wrongPositions.entrySet()) {
            int position = entry.getKey();
            Set<Character> forbiddenChars = entry.getValue();
            if (position < word.length() && forbiddenChars.contains(word.charAt(position))) {
                return false;
            }
        }

        return true;
    }

    private static String normalizeWord(String word) {
        if (word == null) return "";
        return word.toLowerCase().replace('ё', 'е');
    }

    public static String analyzeWord(String guess, String answer) {
        String normalizedGuess = normalizeWord(guess);
        String normalizedAnswer = normalizeWord(answer);

        if (normalizedGuess.length() != 5 || normalizedAnswer.length() != 5) {
            throw new IllegalArgumentException("Слова должны быть длиной 5 букв");
        }

        char[] result = new char[5];
        char[] guessChars = normalizedGuess.toCharArray();
        char[] answerChars = normalizedAnswer.toCharArray();
        boolean[] used = new boolean[5]; // Отслеживаем использованные буквы в ответе

    // Сначала отмечаем правильные позиции (зеленые)
        for (int i = 0; i < 5; i++) {
            if (guessChars[i] == answerChars[i]) {
                result[i] = '+';
                used[i] = true; // Помечаем букву как использованную
            } else {
                result[i] = '-'; // Временно ставим минус
            }
        }

    // Затем отмечаем буквы в неправильных позициях (желтые)
        for (int i = 0; i < 5; i++) {
            if (result[i] == '+') continue; // Уже обработали

        // Ищем букву в ответе, которая еще не использована
            for (int j = 0; j < 5; j++) {
                if (!used[j] && guessChars[i] == answerChars[j]) {
                    result[i] = '^';
                    used[j] = true; // Помечаем букву как использованную
                    break;
                }
            }
        // Если не нашли подходящую букву, оставляем '-'
        }

        return new String(result);
    }
}
