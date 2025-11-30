package ru.yandex.practicum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WordleTest {

    private WordleDictionary dictionary;
    private WordleDictionaryLoader loader;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Используем только 5-буквенные слова для тестов
        List<String> testWords = Arrays.asList(
            "столик", "стулья", "окошко", "дверца", "книжка",
            "ручеек", "бумага", "мышка", "клавиа"
        );
        dictionary = new WordleDictionary(testWords);
        loader = new WordleDictionaryLoader();
    }

    @Test
    void testDictionaryNormalization() throws IOException {
        // Создаем временный файл словаря с 5-буквенными словами
        File dictFile = tempDir.resolve("test_dict.txt").toFile();
        try (PrintWriter writer = new PrintWriter(dictFile, StandardCharsets.UTF_8)) {
            writer.println("СтОли");
            writer.println("СТУЛЬ");
            writer.println("окошк");
            writer.println("дверь");
            writer.println("ёлочк"); // тест замены ё на е
            writer.println("мёдик");
        }

        WordleDictionary loadedDict = loader.loadDictionary(dictFile.getAbsolutePath());
        List<String> words = loadedDict.getWords();

        assertTrue(words.contains("столи"));
        assertTrue(words.contains("стуль"));
        assertTrue(words.contains("окошк"));
        assertTrue(words.contains("дверь"));
        assertTrue(words.contains("елочк")); // после нормализации
        assertTrue(words.contains("медик"));  // после нормализации
    }

    @Test
    void testDictionaryContains() {
        assertTrue(dictionary.contains("столик"));
        assertTrue(dictionary.contains("СТОЛИК")); // регистр не важен
        assertTrue(dictionary.contains("СтОлИк")); // смешанный регистр
        assertFalse(dictionary.contains("несуществующееслово"));
    }

    @Test
    void testWordAnalysis() {
        // Правильные позиции
        assertEquals("+++++", WordleDictionary.analyzeWord("столик", "столик"));
        assertEquals("+----", WordleDictionary.analyzeWord("стулья", "столик").substring(0, 5));
        assertEquals("+^---", WordleDictionary.analyzeWord("слоник", "столик").substring(0, 5));

        // Буквы в неправильных позициях
        assertEquals("-^---", WordleDictionary.analyzeWord("лукошко", "стулья").substring(0, 5));
        assertEquals("^^---", WordleDictionary.analyzeWord("толика", "столик").substring(0, 5));
    }

    @Test
    void testGameInitialization() {
        WordleGame game = new WordleGame(dictionary);

        assertEquals(6, game.getRemainingSteps());
        assertFalse(game.isGameOver());
        assertFalse(game.isWon());
        assertNotNull(game.getAnswer());
        assertTrue(game.getAttempts().isEmpty());
    }

    @Test
    void testSuccessfulAttempt() throws WordleGame.WordleGameException {
        WordleGame game = new WordleGame(dictionary);
        String answer = game.getAnswer();

        WordleGame.GameResult result = game.makeAttempt(answer);

        assertTrue(result.isWin());
        assertEquals("+++++", result.getAnalysis());
        assertTrue(game.isWon());
        assertTrue(game.isGameOver());
    }

    @Test
    void testFailedAttempt() throws WordleGame.WordleGameException {
        WordleGame game = new WordleGame(dictionary);
        String answer = game.getAnswer();
        
        // Находим слово, которое точно не является ответом
        String wrongWord = dictionary.getWords().stream()
                .filter(word -> !word.equals(answer))
                .findFirst()
                .orElse("стулья"); // fallback

        WordleGame.GameResult result = game.makeAttempt(wrongWord);

        assertFalse(result.isWin());
        assertNotNull(result.getAnalysis());
        assertEquals(5, game.getRemainingSteps());
        assertFalse(game.isWon());
        assertFalse(game.isGameOver());
    }

    @Test
    void testInvalidWordLength() {
        WordleGame game = new WordleGame(dictionary);

        assertThrows(WordleGame.WordleGameException.class, () -> game.makeAttempt("короткоеслово"));
        assertThrows(WordleGame.WordleGameException.class, () -> game.makeAttempt("дл"));
    }

    @Test
    void testWordNotInDictionary() {
        WordleGame game = new WordleGame(dictionary);

        assertThrows(WordleGame.WordNotFoundInDictionaryException.class,
            () -> game.makeAttempt("абвгд")); // несуществующее слово
    }

    @Test
    void testGameOverAfterSixAttempts() throws WordleGame.WordleGameException {
        WordleGame game = new WordleGame(dictionary);
        
        // Находим слово, которое точно не является ответом
        String wrongWord = dictionary.getWords().stream()
                .filter(word -> !word.equals(game.getAnswer()))
                .findFirst()
                .orElse("стулья"); // fallback

        // Делаем 6 неудачных попыток
        for (int i = 0; i < 6; i++) {
            if (!game.isGameOver()) {
                game.makeAttempt(wrongWord);
            }
        }

        assertTrue(game.isGameOver());
        assertFalse(game.isWon());
        assertEquals(0, game.getRemainingSteps());

        // Нельзя сделать ход после окончания игры
        assertThrows(WordleGame.WordleGameException.class, () -> game.makeAttempt("столик"));
    }

    @Test
    void testHintSystem() throws WordleGame.WordleGameException {
        WordleGame game = new WordleGame(dictionary);

        String hint = game.getHint();
        assertNotNull(hint);
        assertTrue(dictionary.contains(hint));
        assertEquals(5, hint.length()); // Подсказка должна быть из 5 букв

        // После ввода слова подсказка должна учитывать новые данные
        String testWord = dictionary.getWords().get(0);
        if (!testWord.equals(game.getAnswer())) {
            game.makeAttempt(testWord);
            String newHint = game.getHint();
            assertNotNull(newHint);
            assertEquals(5, newHint.length());
        }
    }

    @Test
    void testLetterTracking() throws WordleGame.WordleGameException {
        WordleGame game = new WordleGame(dictionary);

        // Делаем попытку и проверяем, что информация о буквах обновляется
        String testWord = dictionary.getWords().get(0);
        game.makeAttempt(testWord);

        // Проверяем, что попытка записана
        assertEquals(1, game.getAttempts().size());
        assertEquals(testWord, game.getAttempts().get(0));
    }

    @Test
    void testDictionaryLoaderFileNotFound() {
        assertThrows(IOException.class,
            () -> loader.loadDictionary("nonexistent_file.txt"));
    }

    @Test
    void testDictionaryLoaderEmptyFile() throws IOException {
        File emptyFile = tempDir.resolve("empty.txt").toFile();
        emptyFile.createNewFile();

        assertThrows(IOException.class,
            () -> loader.loadDictionary(emptyFile.getAbsolutePath()));
    }

    @Test
    void testNormalizationWithYo() throws IOException {
        File dictFile = tempDir.resolve("yo_dict.txt").toFile();
        try (PrintWriter writer = new PrintWriter(dictFile, StandardCharsets.UTF_8)) {
            writer.println("ёлочка");
            writer.println("мёдик");
            writer.println("берёзк");
        }

        WordleDictionary loadedDict = loader.loadDictionary(dictFile.getAbsolutePath());

        assertTrue(loadedDict.contains("елочка"));
        assertTrue(loadedDict.contains("медик"));
        assertTrue(loadedDict.contains("березк"));
    }

    @Test
    void testRandomWordSelection() {
        // Проверяем, что случайные слова действительно из словаря
        for (int i = 0; i < 10; i++) {
            String randomWord = dictionary.getRandomWord();
            assertTrue(dictionary.contains(randomWord));
            assertEquals(5, randomWord.length()); // Должно быть 5 букв
        }
    }

    @Test
    void testGameResult() {
        WordleGame.GameResult result = new WordleGame.GameResult(true, "+++++", "тест");

        assertTrue(result.isWin());
        assertEquals("+++++", result.getAnalysis());
        assertEquals("тест", result.getWord());
    }
}
