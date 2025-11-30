package ru.yandex.practicum;

import java.util.*;

public class WordleGame {
    private final String answer;
    private int remainingSteps;
    private final WordleDictionary dictionary;
    private final List<String> attempts = new ArrayList<>();
    private final Set<Character> correctLetters = new HashSet<>();
    private final Set<Character> wrongLetters = new HashSet<>();
    private final Map<Integer, Character> correctPositions = new HashMap<>();
    private final Map<Integer, Set<Character>> wrongPositions = new HashMap<>();

    public WordleGame(WordleDictionary dictionary) {
        this.dictionary = dictionary;
        this.answer = dictionary.getRandomWord();
        this.remainingSteps = 6;

        for (int i = 0; i < 5; i++) {
            wrongPositions.put(i, new HashSet<>());
        }
    }

    public GameResult makeAttempt(String word) throws WordleGameException {
        if (remainingSteps <= 0) {
            throw new WordleGameException("Игра окончена, попытки закончились");
        }

        String normalizedWord = normalizeWord(word);

        if (normalizedWord.length() != 5) {
            throw new WordleGameException("Слово должно содержать 5 букв");
        }

        if (!dictionary.contains(normalizedWord)) {
            throw new WordNotFoundInDictionaryException("Слово не найдено в словаре: " + word);
        }

        attempts.add(normalizedWord);
        remainingSteps--;

        if (normalizedWord.equals(answer)) {
            return new GameResult(true, WordleDictionary.analyzeWord(normalizedWord, answer), normalizedWord);
        }

        // Обновляем информацию о буквах
        updateLetterInfo(normalizedWord);

        return new GameResult(false, WordleDictionary.analyzeWord(normalizedWord, answer), normalizedWord);
    }

    public String getHint() {
        List<String> possibleWords = dictionary.findPossibleWords(
            correctLetters, wrongLetters, correctPositions, wrongPositions);

        if (possibleWords.isEmpty()) {
            return null;
        }

        Random random = new Random();
        return possibleWords.get(random.nextInt(possibleWords.size()));
    }

    public boolean isGameOver() {
        return remainingSteps <= 0 || attempts.contains(answer);
    }

    public boolean isWon() {
        return attempts.contains(answer);
    }

    public String getAnswer() {
        return answer;
    }

    public int getRemainingSteps() {
        return remainingSteps;
    }

    public List<String> getAttempts() {
        return Collections.unmodifiableList(attempts);
    }

    private void updateLetterInfo(String word) {
        String analysis = WordleDictionary.analyzeWord(word, answer);

        for (int i = 0; i < 5; i++) {
            char guessChar = word.charAt(i);
            char resultChar = analysis.charAt(i);

            if (resultChar == '+') {
                correctLetters.add(guessChar);
                correctPositions.put(i, guessChar);
            } else if (resultChar == '^') {
                correctLetters.add(guessChar);
                wrongPositions.get(i).add(guessChar);
            } else if (resultChar == '-') {
                wrongLetters.add(guessChar);
            }
        }
    }

    private String normalizeWord(String word) {
        return word.toLowerCase().replace('ё', 'е');
    }

    public static class GameResult {
        private final boolean win;
        private final String analysis;
        private final String word;

        public GameResult(boolean win, String analysis, String word) {
            this.win = win;
            this.analysis = analysis;
            this.word = word;
        }

        public boolean isWin() {
            return win;
        }

        public String getAnalysis() {
            return analysis;
        }

        public String getWord() {
            return word;
        }
    }
}
